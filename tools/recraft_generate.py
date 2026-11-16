"""Generates game images with the Recraft API (PLAN.md K7).

Reads docs/grafika/prompty.json and writes PNGs to shared/src/commonMain/composeResources/drawable/<name>.png.
The API token comes from the RECRAFT_API_TOKEN environment variable and is never written anywhere.
Each image costs Recraft credits (see https://www.recraft.ai/docs/api-reference/pricing.md); commercial use of the
results needs a paid Recraft plan.

Usage (in the repo root):
  python3 tools/recraft_generate.py --dry-run              # show what would be generated, no requests
  python3 tools/recraft_generate.py                        # generate assets whose PNG does not exist yet
  python3 tools/recraft_generate.py art_player_drone       # (re)generate only the named assets
  python3 tools/recraft_generate.py --force                # regenerate everything
  python3 tools/recraft_generate.py --max-size 512         # also shrink results (needs Pillow)

Only the Python standard library is required (Pillow is optional, for --max-size).
"""
import argparse
import base64
import io
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROMPTS = os.path.join(ROOT, "docs", "grafika", "prompty.json")
OUT = os.path.join(ROOT, "shared", "src", "commonMain", "composeResources", "drawable")
API = "https://external.api.recraft.ai/v1"


def request(method, path, token, body=None, content_type="application/json", retries=3):
    for attempt in range(retries):
        req = urllib.request.Request(API + path, data=body, method=method)
        req.add_header("Authorization", f"Bearer {token}")
        if body is not None:
            req.add_header("Content-Type", content_type)
        try:
            with urllib.request.urlopen(req, timeout=180) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", "replace")
            if e.code in (429, 500, 502, 503) and attempt < retries - 1:
                time.sleep(5 * (attempt + 1))
                continue
            raise SystemExit(f"Recraft {path}: HTTP {e.code}: {detail}")
    raise SystemExit(f"Recraft {path}: no response")


def generate(token, config, asset):
    prompt = f"{asset['prompt']}, {config['style_suffix']}" if config.get("style_suffix") else asset["prompt"]
    body = {
        "prompt": prompt,
        "model": asset.get("model", config.get("model")),
        "size": asset.get("size", config.get("size")),
        "n": 1,
        "response_format": "b64_json",
        "image_format": "png",
    }
    for key in ("style", "style_id"):
        value = asset.get(key, config.get(key))
        if value:
            body[key] = value
    result = request("POST", "/images/generations", token, json.dumps({k: v for k, v in body.items() if v is not None}).encode())
    return base64.b64decode(result["data"][0]["b64_json"])


def remove_background(token, png):
    boundary = uuid.uuid4().hex
    parts = [
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"response_format\"\r\n\r\nb64_json\r\n".encode(),
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"image_format\"\r\n\r\npng\r\n".encode(),
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.png\"\r\n"
        f"Content-Type: image/png\r\n\r\n".encode() + png + b"\r\n",
        f"--{boundary}--\r\n".encode(),
    ]
    result = request("POST", "/images/removeBackground", token, b"".join(parts), f"multipart/form-data; boundary={boundary}")
    image = result.get("image") or result.get("data", [{}])[0]
    if "b64_json" in image:
        return base64.b64decode(image["b64_json"])
    with urllib.request.urlopen(image["url"], timeout=180) as resp:
        return resp.read()


def shrink_check():
    try:
        import PIL  # noqa: F401
    except ImportError:
        raise SystemExit("--max-size needs Pillow: pip install pillow")


def shrink(png, max_size):
    try:
        from PIL import Image
    except ImportError:
        raise SystemExit("--max-size needs Pillow: pip install pillow")
    img = Image.open(io.BytesIO(png))
    img.thumbnail((max_size, max_size), Image.LANCZOS)
    out = io.BytesIO()
    img.save(out, format="PNG", optimize=True)
    return out.getvalue()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("names", nargs="*", help="asset names to generate (default: all missing)")
    parser.add_argument("--force", action="store_true", help="regenerate even if the PNG exists")
    parser.add_argument("--dry-run", action="store_true", help="list the work without calling the API")
    parser.add_argument("--max-size", type=int, default=0, help="shrink the longer side to this many pixels")
    args = parser.parse_args()

    with open(PROMPTS, encoding="utf-8") as f:
        config = json.load(f)
    assets = config["assets"]
    known = {a["name"] for a in assets}
    unknown = [n for n in args.names if n not in known]
    if unknown:
        raise SystemExit(f"Unknown asset names: {', '.join(unknown)}")

    todo = []
    for asset in assets:
        path = os.path.join(OUT, asset["name"] + ".png")
        if args.names and asset["name"] not in args.names:
            continue
        if os.path.exists(path) and not args.force and not args.names:
            continue
        todo.append((asset, path))

    print(f"{len(todo)} image(s) to generate" + (" (dry run)" if args.dry_run else ""))
    for asset, path in todo:
        print(f"  {asset['name']}{' + remove background' if asset.get('remove_background') else ''}")
    if args.dry_run or not todo:
        return

    token = os.environ.get("RECRAFT_API_TOKEN")
    if not token:
        raise SystemExit("Set RECRAFT_API_TOKEN (Recraft profile → API) in your shell first.")
    if args.max_size:
        shrink_check()  # fail before spending credits, not after

    os.makedirs(OUT, exist_ok=True)
    for asset, path in todo:
        png = generate(token, config, asset)
        if asset.get("remove_background"):
            png = remove_background(token, png)
        if args.max_size:
            png = shrink(png, args.max_size)
        with open(path, "wb") as f:
            f.write(png)
        print(f"saved {os.path.relpath(path, ROOT)} ({len(png) // 1024} KB)")


if __name__ == "__main__":
    sys.exit(main())
