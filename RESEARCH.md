# Research: Survivor.io (Habby, 2022) i gatunek survivor-like

Stan na 2026-11-16. Źródła to głównie poradniki fanów z lat 2022–2023 i analizy branżowe (Naavik, mobilegamer.biz, PocketGamer.biz, Deconstructor of Fun). Oficjalna wiki fandom i część poradników blokowały pobieranie (HTTP 402/403), więc liczby oznaczone „niepotw.” pochodzą z jednego źródła albo są sprzeczne. Liczby gry mogły się zmienić w nowszych wersjach.

Cel dokumentu: zrozumieć, **co** gra robi i **dlaczego** to działa. Specyfikacja oryginalnej gry jest w [SPEC.md](SPEC.md).

---

## 1. Run, czyli rdzeń rozgrywki

### Sterowanie i kamera
- Jeden palec: wirtualny joystick tylko do ruchu. Postać atakuje sama. To zasada „one-button control”, wspólna dla gier Habby (Archero, Survivor.io). [TA][MGB]
- Orientacja pionowa (niepotw.). Joystick pływający albo stały: niepotw.
- Kamera jest mocno oddalona. Przez to obrażenia obszarowe są cenniejsze, a ruch ważniejszy niż celowanie. [NAAVIK]
- Mapa przewija się bez końca w jednej albo dwóch osiach. [NAAVIK]
- Hitbox gracza jest mniejszy niż sprite. Wrogowie idą do aktualnej pozycji gracza, nie wyprzedzają ruchu. [PG]

**Dlaczego:** zero nauki sterowania i gra jedną ręką w kolejce czy tramwaju. Cała uwaga idzie w pozycjonowanie i wybór umiejętności.

### Struktura runu
- Rozdział trwa **15 minut**. Boss co 5 minut (5:00, 10:00, finał 15:00). [NAAVIK]
- Oś czasu rozdziału 1 (w przybliżeniu): [SGG1][LW]

  | Czas | Wydarzenie |
  |---|---|
  | 1:00 | pierwsze jednostki dystansowe |
  | 2:00 | alarm: horda |
  | 3:00 | pierwszy elitarny przeciwnik (opancerzony) |
  | 4:00 | alarm: horda ze wszystkich stron |
  | 5:00 | boss 1 (3 odbijające się pociski), zwykli wrogowie dostają pancerz |
  | 8:00 | alarm: rój szybkich psów + elitarny ślimak |
  | 10:00 | boss 2, potem nowe typy wrogów (owady) |
  | 12:00 | elitarny przeciwnik |
  | 15:00 | boss finałowy (szarża zapowiadana czerwonymi liniami) |

- Licznik czasu jest u góry ekranu. Komunikat ostrzega przed hordą i bossem. [PGBIZ]
- **Arena bossa:** zwykli wrogowie znikają, wokół gracza zamyka się nieruchomy pierścień, z którego nie da się wyjść. To główna różnica wobec Vampire Survivors, gdzie przed bossem można uciec. [PGBIZ][TA]
- Wygrana: przetrwać do końca i pokonać bossa finałowego. Przegrana: HP spada do 0 od kontaktu z wrogami. [BSB][TA]
- Po przegranej gracz i tak dostaje XP konta i złoto. [BSB]
- Jedyny „wynik” runu to czas przetrwania. Naavik ocenia to jako słaby cel do bicia. [NAAVIK]
- Krytyka: 15 minut to długość partii battle royale. Zawęża to grupę odbiorców i męczy przy powtarzaniu rozdziału (w Archero partia trwa kilka minut). [NAAVIK]

### Pętla poziomów
- Wrogowie upuszczają kryształy XP w trzech kolorach: zielone, niebieskie i złote/żółte (rzadkie, większa wartość). Wartości i krzywa XP: niepotw. [NAAVIK][LW]
- Pasek poziomu napełnia się coraz wolniej. [TA]
- Każdy poziom daje **3 losowe karty**. Wybór karty posiadanej umiejętności podnosi jej poziom. [TA][OCG]
- **Sloty: 6 aktywnych (w tym broń startowa) + 6 pasywnych.** Maks. poziom 5. [NAAVIK][OCG][LW]
- Gdy wszystkie sloty są pełne i wymaksowane, poziom i skrzynie dają złoto. [OCG]
- Losowanie kart od nowa (reroll) jest odblokowywane w mecie albo dostępne raz na grę (niepotw., sprzeczne źródła).

**Dlaczego:** decyzja co 20–40 sekund, zawsze prosta (1 z 3). Gracz buduje „build” bez czytania tabel. Limit 6+6 wymusza wybory i daje ewolucjom sens.

### Umiejętności aktywne (przykłady wzorców)
Nazwy są cudze. Liczą się **wzorce** zachowania:

| Wzorzec | Przykład w Survivor.io | Skalowanie na poziomach |
|---|---|---|
| pocisk w najbliższego | Kunai (broń domyślna) | liczba pocisków, obrażenia |
| pocisk wracający | Boomerang | rozmiar, obrażenia |
| rzut w górę i opad (łuk) | Brick | liczba 1→5 |
| pocisk rykoszetujący od krawędzi ekranu | Drill Shot | liczba 1→5 |
| orbitujące obiekty blokujące pociski | Guardian | liczba 2→6 |
| aura obrażeń wokół gracza | Forcefield | obszar |
| piorun w losowych wrogów | Lightning Emitter | liczba uderzeń 1→5 |
| promień/laser | Laser Launcher | obrażenia |
| rakiety z dronów | Drone A/B | obrażenia |
| podpalenie obszaru | Molotov | liczba plam |
| miny/pułapki | Modular Mine | obrażenia |
| odbijająca się piłka | Soccer Ball | liczba trafień |
| eksplozja obszarowa | RPG | obrażenia, obszar |

### Umiejętności pasywne (pełna lista z [OCG], poziomy 1→5)
| Efekt | Wartość |
|---|---|
| prędkość pocisków | +10% → +50% |
| czas odnowienia | −8% → −40% |
| regeneracja HP | 1% → 5% HP co 5 s |
| czas trwania efektów | +10% → +50% |
| maks. HP | +20% → +100% |
| zasięg / obszar | +10% → +50% |
| atak | +10% → +50% |
| zasięg zbierania łupu | +100% → +500% |
| zdobywane XP | +8% → +40% |
| zdobywane złoto | +8% → +40% |
| redukcja obrażeń | −10% → −50% (niepotw.) |
| prędkość ruchu | do +50% |

### Ewolucje
- Warunek: umiejętność aktywna na **poziomie 5** + **sparowana pasywna** na dowolnym poziomie. [OCG][BS]
- Ewolucja przychodzi ze **złotej skrzyni po elicie albo bossie** (automatycznie, gdy oba elementy są) albo jako karta przy awansie. [BS][LW]
- Wyewoluowana umiejętność ma osobny znacznik (czerwona gwiazdka) i mocno skacze w sile (np. mnożnik ×6 → ×20). [OCG]
- Około 17 ewolucji. Są też pary broń+broń (dwa drony → niszczyciel). [BS]
- Przykładowe pary (logika tematyczna): pocisk rykoszetowy + szybkość pocisków; aura + regeneracja; orbitujące obiekty + czas trwania; rzut ciężarkiem + maks. HP; eksplozja + obszar; bumerang + magnes; podpalenie + złoto.

**Dlaczego:** to drugi, dłuższy cel w runie („zbieram parę do ewolucji”). Gracz ma powód, żeby wybrać słabszą pasywkę. Elita i boss dają wyraźną nagrodę, więc chce się walczyć zamiast uciekać.

### Przedmioty na mapie
- Niszczalne skrzynki dają złoto, mięso (leczenie), **magnes** (ściąga wszystkie kryształy z ekranu) i **bombę** (zabija wszystkich wrogów na ekranie, rani bossa). [SGG1][LW][PG]
- Łup nie wygasa, ale znika, gdy zbyt długo jest poza ekranem. [LW]
- Skrzynia po elicie: animacja „koła/maszyny losującej”, 1, 3 albo 5 ulepszeń, tylko dla posiadanych umiejętności. [SGG1][OCG][PG]

### Wrogowie
- Typy w rozdziale 1: wolni piechurzy (zombie), szybkie roje (psy), dystansowi, latający, opancerzone elity, owady. [SGG1]
- Gęstość i siła rosną z czasem. Pod koniec wrogowie są „masą”, nie pojedynczymi zagrożeniami. [NAAVIK]
- Bossowie: czerwone strefy-zapowiedzi ataku, szarże, odbijające się pociski. Bije dużo mocniej niż zwykły wróg. [TA][PGPOW]
- Ograniczenia projektowe według Naavik: tłum zasłania przeszkody, ataki dystansowe wrogów są prawie nieczytelne w tłumie, nowe mechaniki nie mogą być zbyt mocne. [NAAVIK]

### Pauza, wskrzeszenie, autoplay
- Pauza jest. [NAAVIK]
- Wskrzeszenie: monety wskrzeszenia albo reklama za jedno wskrzeszenie (niepotw.).
- Autoplay w runie: brak. Przyspieszenie gry: brak potwierdzenia.

### Dlaczego to się dobrze gra (synteza)
1. **Fantazja mocy:** zaczynasz słaby, po 10 minutach czyścisz ekran. „Od zera do bohatera” w jednym runie. [NAAVIK]
2. **Niskie obciążenie:** brak wzorców ruchu wrogów do nauki, mniej wymagające niż Archero. [NAAVIK]
3. **Rytm decyzji:** krótka decyzja co kilkadziesiąt sekund, większy cel (ewolucja) co kilka minut, punkty kulminacyjne (boss) co 5 minut.
4. **Natychmiastowe wejście:** po starcie gracz od razu gra. Systemy RPG są dokładane stopniowo. [MGB]
5. **Czytelny postęp:** licznik czasu, pasek XP, pasek HP bossa.

### Skargi na run
- Długość 15 minut przy powtarzaniu tego samego rozdziału. [NAAVIK]
- Fantazja mocy z czasem się nie rozwija („rozdział 10 wygląda jak 1”). [NAAVIK]
- Losowość kart i ulepszeń. [Kimola]

---

## 2. Meta, czyli wszystko poza runem

### Ekwipunek
- **6 slotów:** broń, naszyjnik, rękawice, zbroja, pas, buty. **Broń wyznacza umiejętność startową runu.** [TA][OCG]
- Rzadkości (kolor): szara → zielona → niebieska → fioletowa → żółta → czerwona. Fioletowa i żółta mają podstopnie +1 i +2. Inne źródło podaje 11 poziomów. [PG]
- **Merge:** 3 identyczne przedmioty tej samej rzadkości dają 1 przedmiot wyższej. Na wyższych poziomach zasady są inne (2 takie same + „pasza”), sprzeczne źródła. [PG][TA]
- Przycisk „Połącz wszystko” dla niskich rzadkości. [my.games support]
- Ulepszanie: złoto + **plany** (designs) właściwe dla slotu. Reset zwraca 100% kosztu. [OCG][PGBIZ]
- Wyższa rzadkość odblokowuje dodatkowe pasywne bonusy przedmiotu. [BSB]

**Dlaczego:** duplikaty nigdy nie są bezwartościowe (zawsze idą do merge'a). Daje to stały, widoczny postęp i powód do otwierania skrzyń.

### Kolejne warstwy progresji (kolejność dodawania)
| Kiedy | Warstwa |
|---|---|
| start (sierpień 2022) | jeden bohater, ekwipunek + ulepszenia, zakładka „Ewolucja” (stałe bonusy statystyk, limit wg poziomu konta), Próby (Trials), skrzynie, Patrol (idle) |
| do 2 miesięcy | Survivor Pass (battle pass) |
| v1.5 | części technologiczne (dodatkowe sloty wzmacniające konkretne umiejętności) |
| v1.12 | grywalne postaci (odłamki), tryb bossa z rankingiem |
| v1.13 | zwierzaki |
| v2.5+ | kolekcje |
| v5.x | kolejne systemy (Xenoform, wierzchowce) |

**Wniosek:** w dniu premiery meta była mała. Naavik krytykował „bardzo mało ścieżek progresji”, a gra i tak zarobiła 75 mln USD w 2 miesiące. Kolejne warstwy to głównie narzędzia retencji i monetyzacji dokładane później.

### Waluty
Złoto, klejnoty (premium), energia, klucze (skrzynie ekwipunku), plany, złote DNA (z Prób), XP konta, ciasteczka zwierzaków, odłamki postaci.

### Energia (niepotw., poradniki 2022–23)
- 5 energii za rozdział. Limit 30 (50 z miesięczną kartą). Odnawianie około 1 na 20 min.
- Doładowanie: reklama +5 (3×/dzień), 100 klejnotów za 15 (3×/dzień), awans konta +5.
- Próby i wyzwania dzienne nie kosztują energii.

### Tryby
- **Rozdziały:** liniowo, następny po ukończeniu poprzedniego. Na premierę ~100, w v5.4 już ponad 350.
- **Próby (Trials):** od rozdziału 2. Każdy ukończony rozdział daje 3 trudniejsze powtórki z mocniejszymi wrogami. Nagroda: złote DNA, klejnoty.
- **Patrol (idle):** nagrody naliczane offline do 24 h. Szybki patrol za energię albo reklamę.
- **Wyzwania / tryb bossa z rankingiem:** sezony po ~4 tygodnie, 2 próby dziennie.
- Później: wiele trybów eventowych.

### Retencja
- Misje dzienne (punkty → skrzynie co 20 pkt do 100) i tygodniowe.
- Sklep dzienny, pakiety dzienne za reklamę, poczta, eventy, kody prezentowe.
- Retencja (data.ai, ~2 mies. po premierze): **D1 44%, D7 15%, D30 4%**. Dla porównania Archero: D1 49%, D7 23%, D30 11%. [mobilegamer.biz]

### Monetyzacja
- Model hybrid-casual: około **50% przychodu z reklam z nagrodą, 50% z IAP** (szacunek). [mobilegamer.biz]
- Reklamy są tylko z nagrodą (nie wymuszone): darmowe skrzynie w sklepie, energia, szybki patrol, skrzynie zwierzaków, losowania eventowe.
- IAP: pakiety rozdziałowe 1,99–2,99 USD, pakiety dzienne 0,99–1,99 USD, skarbonka (napełnia się w grze, płacisz za rozbicie), fundusz wzrostu, karty miesięczne, battle pass, skrzynie z gwarancją.
- Najlepiej konwertują tanie oferty na początku i po ukończeniu rozdziału: szybka nauka i szybki wzrost siły, potem ściana. [NAAVIK]
- Przychody (AppMagic, tylko IAP): 15 mln USD w pierwszy miesiąc, **75 mln USD i 37 mln pobrań po 2 miesiącach**, ~5 mln USD/mies. po 3 latach. Około 75% przychodu z Chin, Korei i Tajwanu.
- Pozyskanie użytkowników: ~50% budżetu na TikToku.

### Skargi i pochwały
- Skargi: ściana energii, ściana pay-to-win w późnej grze, losowość, „grind bez nagrody”, utrata postępu.
- Pochwały: wciągająca pętla, brak wymuszonych reklam, ocena 4,7/5 w App Store US (245 tys. ocen).

---

## 3. Porównanie z innymi grami gatunku

| Gra | Model | Run | Sterowanie | Wyróżnik |
|---|---|---|---|---|
| Survivor.io | F2P, reklamy + IAP | 15 min, boss co 5 min, arena | 1 palec, pion | merge ekwipunku, ewolucje ze skrzyń |
| Vampire Survivors | płatna (~3 USD) | 30 min | auto-atak, poziom | ogromna liczba postaci, map i sekretów |
| Archero (Habby) | F2P | pokoje, do 50 z rzędu | 1 palec, strzał gdy stoisz | krótkie partie, wybór umiejętności po pokoju |
| 20 Minutes Till Dawn | płatna | 20 min | dwa joysticki, ręczne celowanie, poziom | więcej skilla, mniej losowości |
| Brotato | płatna | fale ~20×1 min | auto-atak | sklep między falami, do 6 broni |
| Magic Survival | F2P | – | 1 palec | prekursor formatu na mobile |

---

## 4. Technologia

### Silniki (stan 2025–2026)
| Silnik | Język | Dowody na 500–1000+ obiektów na telefonie | Reklamy / IAP | Survivor-like w nim |
|---|---|---|---|---|
| **Unity 6** | C# | mocne (Deep Rock Galactic: Survivor, opisany przez Unity) | dojrzałe SDK (LevelPlay, AppLovin MAX, AdMob, IAP) | Vampire Survivors (po przepisaniu), 20 Minutes Till Dawn, DRG: Survivor |
| **Godot 4.x** | GDScript (C# na mobile nadal „experimental”) | prawdopodobne, brak benchmarku 2D mobile | Play Billing i StoreKit 2 oficjalnie; reklamy przez wtyczki społeczności | Brotato, Halls of Torment |
| **Phaser / PixiJS + Capacitor** | TypeScript | ~300+ obiektów (anegdotycznie), zgłaszane problemy wydajności na Androidzie | wtyczki Capacitor (RevenueCat, AdMob społecznościowy) | wczesne Vampire Survivors, Granny's Rampage |
| Defold | Lua | brak danych | niepotw. | – |
| Flutter + Flame | Dart | brak danych, wymaga optymalizacji | RevenueCat oficjalnie | – |
| Cocos Creator | TS | anegdotycznie | niepotw. | – |
| Bevy | Rust | brak danych | brak | – |

- Unity Personal: darmowe do 200 tys. USD przychodu, splash screen opcjonalny od Unity 6. Opłata runtime anulowana we wrześniu 2024.
- Vampire Survivors zaczynało na Phaserze. Przepisanie na Unity wyraźnie podniosło FPS.

### Techniki dla setek przeciwników
- **Pula obiektów (object pooling)** dla wrogów, pocisków, kryształów i liczb obrażeń. Bez tworzenia i niszczenia w trakcie gry.
- **Siatka przestrzenna (spatial hash)** do kolizji i odpychania wrogów: O(N) zamiast O(N²).
- **Bez silnika fizyki** dla tłumu. Proste odpychanie zamiast ciał fizycznych, bo „pchanie ciał fizyki nie skaluje się do setek wrogów” (case study Vampire Survivors).
- **Flow field** zamiast osobnego wyszukiwania ścieżki dla każdego wroga, gdy mapa ma przeszkody.
- **Jedna pętla aktualizacji** dla wszystkich wrogów zamiast Update na każdym obiekcie, uproszczenia dla wrogów poza ekranem.
- **Instancing GPU / batching** sprite'ów, jeden atlas.
- **Liczby obrażeń** jako jeden system cząsteczek z atlasem cyfr.
- **Automatyczne scenariusze obciążeniowe** (1000 wrogów naraz) mierzące CPU, GPU i pamięć. DRG: cel 30 fps na słabym telefonie, 60 na flagowcu.

### Sklepy
- Google Play: 25 USD jednorazowo. **Nowe konto osobiste: test zamknięty z min. 12 testerami przez 14 dni** przed dostępem do produkcji. Od 31.08.2026 wymagany target API 36 (Android 16).
- Apple: 99 USD/rok. Prowizja 15% w Small Business Program. Nowe kategorie wiekowe 4+/9+/13+/16+/18+.
- Reklamy w UE/UK: wymagana zgoda przez certyfikowany CMP (np. Google UMP), najpierw komunikat GDPR, potem ATT na iOS.

### Granica prawna „inspiracji”
- **Nie są chronione:** mechaniki, zasady, gatunek (auto-atak, kryształy XP, 1 z 3, ewolucje, merge).
- **Są chronione:** nazwy, postaci, grafika, muzyka, teksty, konkretny wygląd UI i elementów, kod, znaki towarowe.
- Tetris v. Xio (2012): klon przegrał, bo skopiował konkretny wygląd i ruch klocków, nie samą ideę. Triple Town v. Yeti Town: ugoda, klon wycofany.
- Apple 4.1 (Copycats) i 4.3 (Spam) odrzucają kopie i aplikacje „za podobne”. Google Play ma polityki podszywania się i własności intelektualnej.

---

## Klucz źródeł
- [NAAVIK] https://naavik.co/deep-dives/survivorio-archeros-footsteps/
- [PGBIZ] https://www.pocketgamer.biz/feature/79592/how-innovation-and-iteration-has-transformed-survivorio/
- [MGB] https://mobilegamer.biz/the-design-secrets-powering-habbys-hit-machine/
- mobilegamer.biz: https://mobilegamer.biz/two-months-in-survivor-io-passes-75m-from-37m-downloads/ , https://mobilegamer.biz/tiktok-drives-habbys-survivor-io-past-15m-iap-revenue-12m-downloads/
- [DOF] https://www.deconstructoroffun.com/blog/2025/7/31/habbys-hybridcasual-empire-the-template-that-built-a-powerhouse
- [OCG] https://onechilledgamer.com/survivor-io-skill-guide , https://onechilledgamer.com/survivor-io-guide-and-tips-for-beginners
- [LW] https://www.levelwinner.com/?p=72984
- [SGG1] https://simplegameguide.co.uk/how-to-beat-chapter-1-survivor-io
- [TA] https://www.talkandroid.com/24372-survivor-io-ultimate-game-guide-with-tips-2023/
- [PG] https://www.pocketgamer.com/survivor-io/guide/
- [PGPOW] https://www.pocketgamer.com/survivor-io/best-powers/
- [BS] https://www.bluestacks.com/blog/game-guides/survivor-io/sio-skills-evolution-guide-en.html
- [BSB] https://www.bluestacks.com/blog/game-guides/survivor-io/sio-beginner-guide-en.html
- [Kimola] https://kimola.com/reports/comprehensive-survivor-io-feedback-report-unlock-insights-app-store-sg-142100
- Unity DRG: https://unity.com/blog/optimizing-deep-rock-galactic-survivor-for-mobile
- Vampire Survivors co-op case study: https://coherence.io/blog/tradecraft/vampire-survivors-online-coop-case-study
- Godot mobile 2026: https://godotengine.org/article/godot-mobile-update-apr-2026/
- Phaser bullet heaven: https://choostgames.com/blog/building-a-bullet-heaven-in-phaser/
- Google Play testy: https://support.google.com/googleplay/android-developer/answer/14151465
- Apple 4.1: https://developer.apple.com/help/app-review/guideline-reference/4-1-prevent-copycat-rejection

---

## 5. Runda 2 (2026-11-16): dokładne dane

Źródła: wiki fandom czytana w przeglądarce (agentom zwracała HTTP 402) oraz poradniki z lat 2022–2024. [WIKI] = https://survivorio.fandom.com/wiki/<strona>.

### 5.1 Mnożniki umiejętności aktywnych (×ATK, liczba obiektów) [OCG]
| Umiejętność (wzorzec) | Lv1 | Lv2 | Lv3 | Lv4 | Lv5 | EVO |
|---|---|---|---|---|---|---|
| Boomerang (wracający) | ×2.4, 1 | ×2.4, 2 | ×4.8, 2 | ×6, rozmiar+, 2 | ×6, rozmiar+, 2 | ×6, 2 duże po okręgu |
| Brick (rzut łukiem) | ×2, 1 | ×3, 2 | ×4, 3 | ×5, 4 | ×6, 5 | ×6, 8, przebija |
| Drill Shot (rykoszet od krawędzi) | ×1, 1 | ×1, 2 | ×1, szybkość+, 3 | ×1.3, 4 | ×1.3, 5 | ×13, 1 krążący, przebija |
| Drone A (rakiety) | ×1 | ×2 | ×2, rakiety+ | ×3 | ×5 | – |
| Drone B (rakiety) | ×2 | ×2, rakiety+ | ×2, rakiety+ | ×3 | ×5 | – |
| Durian (toczący się kolec) | ×2 | ×4 | ×4, rozmiar+ | ×6 | ×12 | ×12, rozmiar+, strzela kolcami |
| Forcefield (aura) | ×1 | ×1.5, obszar+ | ×2, obszar+ | ×2.2, obszar+ | ×2.5, obszar+ | ×2.5, spowalnia |
| Guardian (orbita) | ×0.5, 2 | ×0.6, 3 | ×0.7, 4 | ×0.8, 5 | ×0.9, 6 | blokuje pociski i wrogów |
| Laser Launcher (promień) | ×0.6 | ×1.2 | ×1.2, obszar+ | ×2 | ×3 | ×3.4, 8 promieni wokół gracza |
| Lightning Emitter (piorun) | ×1.8, 1 | ×2.2, 2 | ×2.7, 3 | ×3.1, 4 | ×3.6, 5 | + fale uderzeniowe |
| Modular Mine (miny) | ×10, 1 | ×10, 2 | ×20, 2 | ×20, obszar+, 2 | ×20, 3 | ×30, 3 |
| Molotov (podpalenie) | ×0.6, 2 | ×0.7, 3 | ×0.9, 4 | ×1, 5 | ×1.2, 6 | ×1.2, 12 beczek |
| RPG (eksplozja) | ×2, 1 | ×4, 1 | ×4, 2 | ×6, 2 | ×6, 3 | ×20, 1 większa |
| Soccer Ball (odbijająca) | ×6, 1 | ×6, 2 | ×8, szybkość+, 2 | ×10, 2 | ×10, 3 | ×10, 9 piłek |
| Kunai (broń, najbliższy) [WIKI Kunai] | ×1.5 | ×2, +1 | ×3, +1 | ×4, +1 | ×5.5, +1 | szybszy ostrzał, przebija |

Broń z ekwipunku zajmuje pierwszy slot aktywny i dokłada swoją umiejętność do puli kart. [WIKI Skills][OCG]

### 5.2 Pełna tabela ewolucji [WIKI Weapon_Skill_Evolution_Guide]
| Aktywna | Para | Typ |
|---|---|---|
| Boomerang | magnes (zasięg łupu) | aktywna + pasywna |
| Brick | maks. HP | aktywna + pasywna |
| Soccer Ball | prędkość ruchu | aktywna + pasywna |
| Forcefield | regeneracja | aktywna + pasywna |
| Drill Shot | prędkość pocisków | aktywna + pasywna |
| Durian | zasięg/obszar | aktywna + pasywna |
| Guardian | czas trwania | aktywna + pasywna |
| Laser Launcher | czas odnowienia | aktywna + pasywna |
| Lightning Emitter | czas odnowienia | aktywna + pasywna |
| Molotov | złoto | aktywna + pasywna |
| RPG | zasięg/obszar | aktywna + pasywna |
| broń: Kunai | XP | broń + pasywna |
| broń: Shotgun, Revolver | atak | broń + pasywna |
| broń: Katana, Lightchaser | redukcja obrażeń | broń + pasywna |
| broń: Baseball Bat | maks. HP | broń + pasywna |
| broń: Void Power (S) | czas trwania | broń + pasywna |
| Drone A + Drone B | – | dwie aktywne → jedna, zwalnia slot |
| Modular Mine + Molotov | – | dwie aktywne → jedna |
| Modular Mine + Lightning Emitter | – | dwie aktywne → jedna |
| (Drone A+B) + Medi-drone | – | ewolucja ewolucji (później) |

Wniosek: pasywki są dzielone (np. czas odnowienia i obszar służą dwóm ewolucjom). Każda z 12 pasywek ma przynajmniej jedną ewolucję, więc każdy wybór pasywki ma sens.

### 5.3 Ekwipunek
- Każdy slot ma kilka typów przedmiotów (w premierze 5 „zestawów” na slot, w każdym zestawie inne bonusy rzadkości). Broń, naszyjnik i rękawice dają ATK. Zbroja, pas i buty dają HP. [WIKI Equipment][OCG]
- **Drabina rzadkości (15 stopni):** Normal → Good → Better → Excellent → Excellent+1 → Excellent+2 → Epic → Epic+1 → Epic+2 → Epic+3 → Legend → Legend+1 → +2 → +3 → +4. [WIKI Merging]
- **Merge (spójny model z [WIKI Merging] i liczb „ekwiwalentu fioletu” [Clashify]):**

  | Wynik | Składniki | Ekwiwalent Excellent |
  |---|---|---|
  | Good / Better / Excellent | 3 identyczne z niższego stopnia | 1 |
  | Excellent+1 | Excellent + 1 dowolny Excellent tego samego slotu | 2 |
  | Excellent+2 | Excellent+1 + 2 dowolne Excellent tego slotu | 4 |
  | Epic | Excellent+2 + identyczny Excellent+2 | 8 |
  | Epic+1 / +2 / +3 | poprzedni + 1 dowolny Epic tego slotu | 16 / 24 / 32 |
  | Legend | Epic+3 + 2 identyczne Epic | 48 |
  | Legend+1…+4 | poprzedni + 1 dowolny Legend tego slotu | 96 / 144 / 192 / 240 |

- **Bonusy rzadkości broni (Kunai) [WIKI Kunai]:**

  | Rzadkość | Bazowy ATK | Odblokowany bonus |
  |---|---|---|
  | Normal | 10 | – |
  | Good | 20 | ATK +10% |
  | Better | 30 | obrażenia tej umiejętności +30% |
  | Excellent | 50 | start runu z tą umiejętnością na Lv2 |
  | Excellent+1 / +2 | 60 / 70 | – |
  | Epic | 132 | ATK +15% |
  | Epic+1 / +2 / +3 | 154 / 176 / 198 | – |
  | Legend | 231 | zmiana zachowania (pocisk dzieli się po trafieniu) |

  Inne bronie mają ten sam szkielet (Good +10% ATK, Epic +15% ATK, Legend zmienia zachowanie, Better/Excellent wzmacniają własną umiejętność). Przedmioty nie-broniowe mają bonusy sytuacyjne (np. pas Epic: +5% prędkości po zjedzeniu mięsa, maks. 30%). [appgamer via snippet]
- Ulepszanie: złoto + plany danego slotu, rosnący koszt, reset zwraca 100%. Limit poziomu rośnie z rzadkością (liczby niepotw.). [OCG]
- S-grade: lepsze statystyki i więcej bonusów. Później Astral Forge, SS i Relic (systemy późnej gry). [OCG][WIKI Merging]

### 5.4 Rozdziały
- Trzy formaty map: **otwarta** (nieskończona, 15 min, 3 bossy, rozdz. 1 ~35 tys. wrogów), **zamknięta** (prostokąt, 8 min, 5 bossów, rozdz. 3 ~10 tys. wrogów), **korytarz pionowy** (przewijanie tylko góra–dół, rozdz. 4). [WIKI Chapter_1, Chapter_3, Enclosed][appgamer snippet]
- XP konta za ukończenie: rozdz. 1 = 1000, rozdz. 3 = 1300. [WIKI]
- Rozdziały pogrupowane tematycznie po 5 (miasto, przedmieścia, zbrojownia, pustynia, wulkan, las). Na 2026-11 jest 355 rozdziałów. [WIKI Chapters]
- Na ekranie rozdziału są skrzynie za kamienie milowe (przetrwaj 5 min, 10 min, ukończ), m.in. z klejnotami (~20–100). [TA][gamesfuze] (kwoty niepotw.)

### 5.5 Run: reszta
- Reroll kart: domyślnie brak. Talent „Rogue” za złote DNA z Prób daje 1 odświeżenie na run. Płatny przywilej daje drugie. [App Store story][mturbogamer]
- Wskrzeszenie: za klejnoty (~100+), za żeton wskrzeszenia albo z bonusu ekwipunku. [mturbogamer][progameguides]
- Statystyki bazowe, krzywa XP, wartości kryształów, szanse 1/3/5 w skrzyni: nigdzie niepublikowane (niepotw.).

### 5.6 Meta: reszta
- **Energia:** 5 za run, limit 30, Próby za darmo. Odnawianie: niepotw.
- **Patrol:** naliczanie co 10 min, limit 24 h (wcześniej 16 h). Na godzinę: złoto ≈ 3000 + 300 × ukończone rozdziały, XP ≈ 1200 + 120 × rozdziały. Szybki patrol: 15 energii = 5 h nagród, 3× dziennie + 1× za reklamę. Odblokowanie po rozdziale 1. [mturbogamer][BSB]
- **Próby:** po rozdziale 2, 3 na rozdział, kolejno, bez energii, bez limitu prób. Modyfikatory: Próba 1 = 1 (wściekłość: szybsi/silniejsi wrogowie), Próba 2 = 3, Próba 3 = 5 (m.in. „połowa XP i dropów”, „podwójny boss”, „ciemność”). Nagrody za pierwsze przejście: 200 klejnotów / 1 złote DNA / 200 klejnotów + 1 DNA. [simplegameguide][theclashify]
- **Zakładka Ewolucja (talenty):** węzły statystyk za złoto, limitowane poziomem konta. Specjalne węzły za złote DNA (np. reroll). [BSB]
- **Misje dzienne:** zadania po 10–20 pkt (zaloguj się, zagraj rozdział, kup w sklepie, ulepsz przedmiot), skrzynia co 20 pkt do 100. Tygodniowe: 4 zadania po 20 pkt (np. pokonaj 50 bossów). [OCG][appgamer]
- **Sklep:** sklep dzienny (ekwipunek za klejnoty, plany za złoto, odświeżany w ciągu dnia), darmowa skrzynia kilka razy dziennie, skrzynie: podstawowa (Normal/Good, klucz albo 80 klejnotów), lepsza (Good–Excellent, gwarancja Excellent co 10 otwarć, klucz), S (300 klejnotów / 2680 za 10). [OCG][PG]
- **Kolejność odblokowań:** rozdział 1 → Patrol i rozdział 2. Rozdział 2 → Próby i wyzwania. Talenty limitowane poziomem konta. [appgamer snippet][BSB]

Dodatkowe źródła rundy 2: [Clashify] http://theclashify.com/survivorio-io-merge-guide/ · [mturbogamer] https://mturbogamer.com/2022/09/survivor-io-patrol-quick-patrol/ , https://mturbogamer.com/2022/10/survivor-io-best-way-to-spend-gems/ · [simplegameguide] https://simplegameguideorg.pages.dev/get-key-evolution-in-survivor-io · [theclashify] https://theclashify.com/survivor-io-wiki/ · [gamesfuze] https://gamesfuze.com/guides/survivor-io-where-to-farm-gold-gems-and-equipment-farm-guide/ · [whatifgaming] https://whatifgaming.com/?p=120942
