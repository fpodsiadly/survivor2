# Zasady pracy w repo survivor2

## Gałęzie i commity
- Zmiany idą na `main`. Commit i push tylko na polecenie właściciela.
- Opisy commitów: po angielsku, w trybie rozkazującym, bez kropki na końcu. Bez stopek o AI i bez współautorów.
- Po pushu na `main` sprawdź CI (Actions). Jeśli padnie, napraw w kolejnym commicie.

## Wersjonowanie
- Wersja jest w `app/build.gradle.kts` (`versionName`, `versionCode`).
- Każdy commit zmieniający aplikację podbija wersję. Nowa funkcja: środkowa cyfra (`0.1.0` → `0.2.0`). Poprawka: ostatnia (`0.1.0` → `0.1.1`). Za każdym razem `versionCode` + 1.
- Commity zmieniające tylko dokumentację (`*.md`, `docs/`) nie budują aplikacji i nie zmieniają wersji.

## Kod
- Kod wspólny dla Androida i iOS jest w module `shared` (Kotlin Multiplatform + Compose Multiplatform):
  - logika w `core/`,
  - rysowanie w `ui/`,
  - różnice platform w `androidMain`/`iosMain` za interfejsem `GamePlatform`.
- We wspólnym kodzie nie używaj API Javy ani Androida (`java.*`, `android.*`, `Math.*`, `String.format`). Używaj `kotlin.math`, a różnice platform dodawaj jako `expect`/`actual` albo do `GamePlatform`.
- Reguły gry trzymamy w czystych klasach i funkcjach w `core/`, z testami w `shared/src/androidUnitTest`. Nowa mechanika najpierw trafia do logiki razem z testem, dopiero potem do rysowania.
- Liczby balansu są tylko w `core/Balance.kt`. Treść (umiejętności, przedmioty, wrogowie, bossowie, rozdziały) trafia do `core/Content.kt` (SPEC W41). Wartość tymczasową oznacz komentarzem `placeholder`.
- Zmiana formatu zapisu: podbij `SaveData.CURRENT_VERSION` i dodaj migrację z testem (`save/Save.kt`).
- Gra działa offline (SPEC W45): bez uprawnienia INTERNET, bez SDK reklam i analityki w F1–F2.
- Klucze i sekrety (keystore, `RECRAFT_API_TOKEN`) nigdy nie trafiają do repo.

## Grafiki
- Grafiki są w `shared/src/commonMain/composeResources/drawable/art_*.png`, dostępne jako `Res.drawable.art_*`.
- Grafiki powstają przez `tools/recraft_generate.py` z promptów w `docs/grafika/prompty.json`. Nową grafikę dodajesz wpisem do pliku i uruchomieniem skryptu dla jej nazwy.
- Grafiki, nazwy i teksty są własne. Nic nie może pochodzić z Survivor.io ani go przypominać (SPEC W40). Przed buildem testowym przejdź listę kontrolną z W40.

## Dokumenty
- Specyfikacja `SPEC.md` mówi, co ma działać. Plan `PLAN.md` mówi, jak to zbudować, i zawiera wyniki w sekcji `## Realizacja`.
- Zmiana wymagań najpierw trafia do `SPEC.md`. Każda zmiana kodu aktualizuje `README.md` (status) i w razie potrzeby `PLAN.md`.
- Dokumenty piszemy po polsku, kod i komentarze w kodzie po angielsku.
