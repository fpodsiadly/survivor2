"""Prints a Markdown table of JUnit XML results for the GitHub Actions job summary.

Usage: python3 .github/scripts/test_summary.py <directory with TEST-*.xml>
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET


def main(directory: str) -> None:
    rows = []
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    for path in sorted(glob.glob(os.path.join(directory, "TEST-*.xml"))):
        suite = ET.parse(path).getroot()
        counts = {key: int(suite.get(key, 0)) for key in totals}
        for key in totals:
            totals[key] += counts[key]
        name = suite.get("name", os.path.basename(path)).rsplit(".", 1)[-1]
        failed = counts["failures"] + counts["errors"]
        rows.append(f"| {'❌' if failed else '✅'} {name} | {counts['tests']} | {failed} | {counts['skipped']} |")

    if not rows:
        print("No test results found.")
        return
    failed_total = totals["failures"] + totals["errors"]
    print(f"### Tests: {totals['tests']} run, {failed_total} failed, {totals['skipped']} skipped\n")
    print("| Class | Tests | Failed | Skipped |")
    print("|---|---|---|---|")
    print("\n".join(rows))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
