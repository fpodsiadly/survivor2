# survivor2 (nazwa robocza: Surv20)

Gra mobilna na Androida i iPhone'a z gatunku survivor-like. Grasz jednym palcem, a postać atakuje sama. Przez 15 minut przetrwasz coraz gęstsze fale wrogów i co kilkadziesiąt sekund wybierasz 1 z 3 umiejętności. Zbierasz pary do ewolucji i walczysz z bossami w zamkniętej arenie. Między runami rozwijasz ekwipunek: łączysz przedmioty w lepsze, ulepszasz je i kupujesz talenty.

Mechaniki odwzorowują grę Survivor.io (Habby, 2022) z okresu jej premiery. Nazwa, świat, postaci, grafika, UI, muzyka i poziomy są własne: gracz steruje dronem bojowym na obcej planecie, a umiejętności to moduły. Szczegóły tej granicy są w [SPEC.md](SPEC.md), w sekcji „Granica podobieństwa”.

## Pobierz

Po każdym pushu na `main` GitHub Actions publikuje wydanie ze stałymi linkami. Linki działają po zalogowaniu na GitHubie, z dostępem do repo:
- 📲 Android: [Surv20.apk](https://github.com/fpodsiadly/survivor2/releases/latest/download/Surv20.apk)
- 🍏 iPhone: [Surv20.ipa](https://github.com/fpodsiadly/survivor2/releases/latest/download/Surv20.ipa). Plik powstaje tylko po ręcznym uruchomieniu workflow z `ios=true`. Instalujesz go na przykład przez Sideloadly z darmowym Apple ID.
- Google Play: `Surv20.aab` w tym samym wydaniu.

## Status

Wersja 0.2.1: prototyp runu (etap E1) i cała logika gry z testami.

Działa:
- aplikacja Android i iOS z jednego modułu `shared` (Kotlin Multiplatform + Compose Multiplatform),
- **prototyp runu:**
  - pływający joystick (dolne 2/3 ekranu, jeden palec),
  - dron gracza z bronią, która co 0,8 s rzuca pocisk w najbliższego wroga,
  - dwa typy wrogów z narastającym spawnem, odpychaniem w tłumie i przenoszeniem odległych (gdy dron stoi, po losowej stronie); ponad limit 400 wrogowie czekają w kolejce,
  - kryształy XP z przyciąganiem i poziomami; przy 300 kryształach dwa najstarsze łączą się w jeden,
  - najwyżej 60 klatek na sekundę i symulacja w stałych krokach 1/60 s, więc gra działa tak samo na ekranach 60, 90 i 120 Hz,
  - HP z nietykalnością po trafieniu, pauza i ekran porażki,
  - na razie proste kształty zamiast grafik, rysowane jako sprite'y z jednego atlasu (ta sama droga, którą pójdą grafiki z Recraft),
- **test wydajności** (menu, tylko w buildzie debug): 300 wrogów, 200 pocisków i 300 kryształów, licznik średnich FPS i „1% low” z okna 60 s (SPEC W43); co minutę wynik trafia też do logu (`adb logcat -s Surv20`),
- reguły gry jako czysta logika ze 118 testami JUnit:
  - **ekwipunek:** 15 stopni rzadkości, pełna tabela merge'a, „Połącz wszystko”, ulepszanie z limitami, reset, statystyki i bonusy stopni,
  - **run:** oś czasu z bossami, arena i alarmy, sloty 6+6, karty 1 z 3, ewolucje (także aktywna+aktywna), skrzynia 1/3/5, XP i kryształy,
  - **mapy:** geometria trzech formatów (otwarta, zamknięta, korytarz), kamera, spawn hord, przenoszenie odległych wrogów, limit 400 wrogów,
  - **walka:** bomba, znikanie przedmiotów, szarżujący wróg z zapowiedzią, walidacja bossów, statystyki, HP z nietykalnością,
  - **meta:** nagrody, poziom konta, talenty, rozdziały i najlepsze czasy, wybór języka,
  - **zapis:** JSON z kopią zapasową i migracjami wersji.

Następne kroki (PLAN.md, etapy E1–E5):
1. pomiar testu wydajności na telefonie (bramka W43: średnio ≥ 58 FPS, 1% low ≥ 45),
2. umiejętności, wrogowie i bossowie w grze, HUD, pauza,
3. ekwipunek, talenty, rozdziały i ekran główny,
4. grafiki z Recraft, dźwięk, balans, akceptacja F1.

## Dokumenty

| Plik | Co zawiera |
|---|---|
| [SPEC.md](SPEC.md) | Zatwierdzona specyfikacja: 45 wymagań (W1–W45) w fazach F1–F3 |
| [RESEARCH.md](RESEARCH.md) | Analiza Survivor.io: run, meta, ekonomia, liczby z wiki, technologia, granica prawna |
| [PLAN.md](PLAN.md) | Plan F1, decyzje techniczne (K1–K11), mapowanie wymagań, wyniki realizacji |
| [AGENTS.md](AGENTS.md) | Zasady pracy w repo: wersje, testy, balans, grafiki, dokumenty |

## Stack

Ten sam co w grze JumperIdle:
- Kotlin 2.1.21, Compose Multiplatform 1.8.2, Android Gradle Plugin 8.9.1,
- Gradle 8.11.1, JDK 17,
- Android 8.0+ (API 26, target 36), iOS 15+,
- GitHub Actions: testy, APK, AAB i wydanie po pushu na `main`, IPA na żądanie,
- grafiki z [Recraft](https://www.recraft.ai/) generowane skryptem.

## Kod

- `shared/src/commonMain/kotlin/com/surv20/game/`:
  - `core/` to czysta logika bez Androida i iOS:
    - `Balance.kt` zawiera wszystkie liczby do strojenia,
    - `Content.kt` zawiera treść gry (na razie typy wrogów),
    - `FramePacer.kt` ogranicza klatki do 60 i wylicza stałe kroki symulacji,
    - `equipment/` to przedmioty, merge, ulepszanie i statystyki,
    - `run/` to oś czasu, umiejętności, karty, skrzynia, XP, mapy, walka, joystick i symulacja runu `RunSim` (tablice, siatka przestrzenna, bez fizyki),
    - `meta/` to nagrody, konto, talenty, rozdziały i język,
    - `save/` to zapis,
  - `ui/` to motyw i rysowanie (`RunView.kt`: pętla `withFrameNanos`, `Canvas`, punkty rysowane paczkami),
  - `App.kt` to powłoka gry,
  - `Platform.kt` zawiera różnice między platformami.
- `shared/src/androidMain`, `shared/src/iosMain` to zapis (SharedPreferences, NSUserDefaults) i wejście iOS.
- `shared/src/androidUnitTest` to testy JUnit.
- `app/` to `MainActivity` Androida, `iosApp/` to aplikacja SwiftUI (projekt z `iosApp/project.yml`, XcodeGen).
- `tools/recraft_generate.py` generuje grafiki, a prompty są w `docs/grafika/prompty.json`.

## Budowanie lokalnie

Potrzebujesz JDK 17, Android SDK (platforma 36) i Gradle 8.11.1. W `local.properties` ustaw `sdk.dir=/ścieżka/do/Android/Sdk` (plik nie trafia do repo).

Testy:

```bash
gradle :shared:testDebugUnitTest
```

APK do instalacji, wynik w `app/build/outputs/apk/debug/app-debug.apk`:

```bash
gradle :app:assembleDebug
```

Instalacja na telefonie podłączonym kablem:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Grafiki (Recraft)

1. Załóż konto w Recraft i weź token API (profil → API). Do użytku komercyjnego potrzebny jest płatny plan, bo obrazy z darmowego planu są publiczne i bez licencji komercyjnej.
2. Ustaw token tylko w swoim terminalu, nigdy w repo:

   ```bash
   export RECRAFT_API_TOKEN=...
   ```

3. Sprawdź listę bez wydawania kredytów:

   ```bash
   python3 tools/recraft_generate.py --dry-run
   ```

4. Wygeneruj brakujące grafiki do `shared/src/commonMain/composeResources/drawable/`:

   ```bash
   python3 tools/recraft_generate.py --max-size 512
   ```

   Opcja `--max-size` zmniejsza obrazy i wymaga Pillow.

Wszystkie prompty kończą się wspólnym sufiksem stylu, więc gra zachowuje jeden wygląd. Prompty nie mogą odwoływać się do nazw, postaci ani wyglądu Survivor.io (W40).

## Podpis APK w CI

Bez sekretów CI podpisuje APK kluczem debug, więc kolejne wydania mogą nie instalować się jako aktualizacja. Stały podpis daje komplet czterech sekretów repo:
- `KEYSTORE_B64`: plik keystore zakodowany w base64,
- `KEYSTORE_PASSWORD`,
- `KEY_ALIAS`,
- `KEY_PASSWORD`.
