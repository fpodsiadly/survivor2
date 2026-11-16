status: approved | 2026-11-16 | stream: n/d (projekt prywatny)
spec: 2026-11-16 @ 26e0fc11
repos: brak (katalog ~/Developer/private/surv20, nie jest repozytorium git; projekt Unity powstanie w `game/`)

# surv20: plan implementacji fazy F1

Repo: brak (projekt prywatny). Specyfikacja: [SPEC.md](SPEC.md) (zatwierdzona, wersja 2). Plan obejmuje tylko wymagania z fazą F1 i części F1 wymagań „F1 / F2”. F2 dostanie osobny plan po zakończeniu F1.

## Stan wyjściowy

- Na komputerze nie ma Unity Hub, edytora Unity, Android SDK, .NET SDK ani git-lfs. Jest Java i git. (sprawdzone 2026-11-16)
- System: Ubuntu 26.04 LTS, 16 rdzeni, 59 GB RAM, 209 GB wolnego miejsca. Unity oficjalnie wspiera edytor na Ubuntu 22.04 i 24.04 z X11. 26.04 nie ma na liście, a jedno zgłoszenie mówi o problemach Hub 3.20 z pobieraniem modułów na 24.04/26.04. Wayland jest „experimental”.
- Najnowsza linia LTS w dokumentacji to Unity 6.3 LTS (6000.3). Istnieją też nowsze wydania 6000.4–6000.7 (nie LTS).

## Decyzje techniczne

Każda decyzja jest punktem do zatwierdzenia (sekcja Pytania).

| # | Decyzja | Dlaczego |
|---|---|---|
| D1 | Unity 6.3 LTS (najnowszy patch 6000.3.x w Hubie w dniu instalacji), wersja przypięta w `ProjectSettings/ProjectVersion.txt` | LTS = 2 lata poprawek, stabilne pakiety Burst/Collections |
| D2 | Projekt Unity w `game/`, dokumenty (SPEC, RESEARCH, PLAN) w katalogu głównym | dokumenty nie mieszają się z `Assets/` |
| D3 | Szablon „Universal 2D” (URP 2D), Input System, uGUI + TextMeshPro, pakiet Localization, Burst, Collections, Mathematics, Test Framework, Newtonsoft JSON | uGUI jest dojrzały na mobile i prosty z paczkami grafik 2D |
| D4 | **Symulacja tłumu w Burst:** wrogowie, pociski gracza i wrogów oraz kryształy jako tablice struktur (`NativeArray`), aktualizowane jednym zestawem zadań Burst co klatkę. Siatka przestrzenna (`NativeParallelMultiHashMap` po komórkach) do odpychania i trafień. Bez Physics2D dla tłumu. | RESEARCH §4: DRG Survivor (Burst bez pełnego ECS), Vampire Survivors (fizyka nie skaluje) |
| D5 | **Renderowanie tłumu, wariant A:** pula obiektów `SpriteRenderer` synchronizowana zadaniem `IJobParallelForTransform`, wspólny atlas. **Wariant B (zapasowy, gdy A nie przejdzie bramki E1):** `Graphics.RenderMeshInstanced` z własnym shaderem instancjonowanym (UV z atlasu, błysk trafienia) | A jest najprostszy i daje animację przez podmianę sprite'a. B to sprawdzona droga z DRG |
| D6 | Bossowie, gracz, UI i obiekty specjalne jako zwykłe GameObjecty (jest ich kilka naraz) | prostota tam, gdzie liczba jest mała |
| D7 | **Logika gry w czystym C#** (`Surv.Core`, bez `UnityEngine`): zasady kart, XP, skrzyń, ewolucji, merge'a, statystyk, nagród, harmonogramu osi czasu, zapisu. Losowość przez interfejs `IRandom` z ziarnem | testy EditMode bez sceny, deterministyczne |
| D8 | **Dane balansu w ScriptableObjectach** (`Assets/Data/`), konwertowane przy starcie na niemutowalne modele `Surv.Core`. Test walidacji danych | W41: zmiana liczby w edytorze, bez kodu |
| D9 | Zapis: JSON (Newtonsoft) w `Application.persistentDataPath`, zapis atomowy (plik tymczasowy → zamiana, kopia `.bak`), pole `version` i lista migracji | W42 |
| D10 | Build Android: IL2CPP, ARM64, min API 26, target API = najwyższe zainstalowane, `Application.targetFrameRate = 60`, tylko pion | W2, W43, W45 |
| D11 | Menu deweloperskie tylko w buildzie deweloperskim (`DEVELOPMENT_BUILD || UNITY_EDITOR`): FPS (średnia, 1% low z okna 60 s), scenariusz obciążeniowy, skok w osi czasu, dodanie przedmiotów i złota | W43 i szybkie testy ręczne |
| D12 | Assembly: `Surv.Core` (czyste C#), `Surv.Sim` (Burst), `Surv.Data` (ScriptableObjecty + konwerter do modeli Core + walidator danych), `Surv.Game` (MonoBehaviour, UI), `Surv.Core.Tests` i `Surv.Sim.Tests` (EditMode), `Surv.Game.Tests` (PlayMode) | granice zależności i szybkie testy |
| D13 | Umiejętności to klasy zachowań w `Surv.Game`, które wpisują pociski do tablic Sim albo pytają siatkę o wrogów w obszarze (aura co 0,5 s, orbita jako stale obracające się punkty kolizji, podpalenie jako obszar z tickiem). Każdy „+” z RESEARCH §5.1 (rozmiar+, obszar+, szybkość+, rakiety+) = +20% danej wielkości | jeden model dla pocisków i obszarów |
| D14 | Teksty od początku przez klucze Localization (nawet przed P26). Nagrody i dane rozdziałów w E2–E3 przez proste zaślepki (testowy rozdział w danych, nagroda „0”), zastępowane w E4 | brak przeróbek wstecz |

## Plan zmian

Etapy idą po kolei. E1 kończy się **bramką wydajności**: dopóki W43 nie przechodzi na telefonie, nie budujemy dalej. Rozmiar: S ≈ 1–3 wieczory, M ≈ tydzień wieczorów, L ≈ 2+ tygodnie.

### E0. Środowisko (S)

**P1. Instalacja i weryfikacja narzędzi.** Serves: W45.
Unity Hub z repozytorium apt Unity (instrukcja oficjalna), edytor 6000.3.x LTS z modułem Android Build Support (OpenJDK, Android SDK & NDK). Sesja GNOME na X11. git-lfs z apt. Sprawdzenie: pusty projekt 2D buduje APK i uruchamia się na telefonie autora (`adb install`). Jeśli Hub lub edytor nie działa na 26.04: obejście z wątku Unity (AppImage Huba, ręczna instalacja modułów), a w ostateczności maszyna wirtualna z Ubuntu 24.04 albo Windows (P31).
Komendy z `sudo` wykonuje autor. Asystent je przygotowuje.

**P2. Szkielet projektu.** Serves: W41, W45.
`game/` z szablonu URP 2D, pakiety z D3, assembly z D12, `.gitignore` dla Unity, `.gitattributes` dla LFS (png, wav, ogg, psd, fbx). Ustawienia Player: pion, IL2CPP, ARM64, API 26, nazwa robocza. Pusty test EditMode i PlayMode przechodzi w Test Runnerze i z linii komend (`Unity -batchmode -runTests -testPlatform EditMode`).
Repozytorium git: zakładane w tym kroku, jeśli autor odpowie „tak” na Q3 (wtedy P32 jest częścią P2).
Konwerter ScriptableObject → modele Core i walidator danych (zakresy, brakujące odwołania, reguły z SPEC) powstają tu, jako pusty szkielet rozbudowywany w kolejnych pozycjach. Test W41: walidator przechodzi na wszystkich danych; zmiana jednej wartości w ScriptableObjecie zmienia wynik testu zachowania bez zmiany kodu.

### E1. Prototyp wydajności, bramka (M)

**P3. Joystick i kamera.** Serves: W1, W2, W3.
Pływający joystick na Input System (Enhanced Touch): dolne 2/3 ekranu, tylko pierwszy palec, promień i martwa strefa z danych. Kamera ortograficzna, szerokość widoku 10 jednostek przy 9:16 (jednostka „ekran” ze SPEC), skalowanie przy 16:9–20:9, bezpieczny obszar dla UI (`Screen.safeArea`).
Testy: PlayMode, symulowany dotyk przez `InputTestFixture`: dotyk w górnej 1/3 nie tworzy joysticka; drugi palec ignorowany; puszczenie zatrzymuje postać.
Szerokość widoku jest zawsze 10 jednostek, a wyższy ekran pokazuje więcej w pionie. Dotyk, który zaczyna się w górnej 1/3, nie jest „pierwszym palcem” nawet po przeciągnięciu w dół. Na korytarzu (P16) kamera i gracz są ograniczeni w osi X tak, że gracz zawsze jest widoczny. `(decyzja autora)`

**P4. Symulacja tłumu.** Serves: W19, W20 (typy 1–2), W43.
`Surv.Sim`: tablice pozycji, prędkości, HP, typu i stanu. Zadania Burst w kolejności: (1) budowa siatki, (2) ruch do aktualnej pozycji gracza + odpychanie od sąsiadów z siatki, (3) kontakt z graczem, (4) przeniesienie odbiegających (W19: za krawędź po stronie ruchu, losowo gdy gracz stoi). Limit 400 żywych, nadwyżka w kolejce spawnu. Spawn poza ekranem. Renderowanie według D5 wariant A.
Testy: EditMode na funkcjach czystych (pozycja przeniesienia dla 4 kierunków ruchu i postoju, zapełnianie kolejki przy limicie).
Spawner hord: na mapie otwartej co najmniej 3 strony ekranu, na korytarzu oba końce, na mapie zamkniętej co najmniej 3 brzegi (W11). Przeniesienie odbiegających na korytarzu: do najbliższego otwartego końca. Test EditMode funkcji wyboru stron dla 3 formatów.

**P5. Pociski, broń domyślna, kryształy.** Serves: W4, W14 (zbieranie), W43.
Pociski w tablicach Sim z zapytaniem o trafienie przez siatkę. Broń B1 (pocisk w najbliższego wroga, statystyki w danych według D8). Kryształy: 3 rodzaje (zielony 1, niebieski 5, żółty 25), przyciąganie w promieniu zbierania. Zwykły wróg: zielony, z szansą 2% żółty. „Wzmocnieni” (warianty po bossie): niebieski. Elity i bossowie: zawsze żółty (zdanie W14 o żółtym jest bardziej szczegółowe niż „niebieski dla elit”, więc wygrywa). Przy 300 kryształach na mapie najstarsze łączą się w jeden o sumie wartości (bez zmian dla gracza). `(decyzja autora)` Liczby obrażeń jako jeden system cząsteczek z atlasem cyfr (RESEARCH §4).
Testy: EditMode, wybór celu „najbliższy” i brak strzału bez wrogów na ekranie (W4).
Test EditMode: rozkład kolorów kryształów (2% ±0,5 pp na 10 000 zabójstw), elita zawsze żółty.

**P6. Menu deweloperskie i scenariusz obciążeniowy.** Serves: W43.
D11. Scenariusz: 300 wrogów, 200 pocisków, 300 kryształów, 6 umiejętności aktywnych na poziomie 5 (w E1 zastąpionych równoważnym strumieniem pocisków), 60 s. W scenariuszu gracz jest nieśmiertelny i porusza się po okręgu, a liczby obiektów są stale uzupełniane do celu. Wynik: średnia FPS i 1% low (średni FPS 1% najwolniejszych klatek) na ekranie i w logu (`adb logcat`).

**P7. Pomiar na telefonie i bramka.** Serves: W43.
Build deweloperski (IL2CPP, ARM64, bez „Script Debugging”) na telefonie średniej klasy autora, bo scenariusz istnieje tylko w buildzie deweloperskim (W43). Profil Unity Profiler przez USB. Bramka: średnia ≥ 58 i 1% low ≥ 45. Telefon słabszej klasy: tylko jeśli autor go ma, próg średnio ≥ 30. Gdy nie przechodzi: wariant B z D5, ponowny pomiar. Wynik wpisany w `## Realizacja`.

### E2. Rdzeń runu (L)

**P8. Gracz, obrażenia, koniec runu.** Serves: W13, W5 (HP).
HP gracza z wartości bazowych, obrażenia kontaktowe na sekundę, 0,5 s nietykalności z miganiem, HP = 0 → porażka. Pasek HP pod postacią.
Testy: EditMode na modelu obrażeń (nietykalność blokuje drugie trafienie w 0,5 s; redukcja obrażeń mnoży się z talentem).
Obrażenia kontaktowe na trafienie = stawka % HP/s × 0,5 s (nietykalność). Obrażenia pocisków wrogów i ataków bossów w danych. `(decyzja autora)`

**P9. XP, awans, karty, sloty.** Serves: W14, W15 (karty), W16 (sloty).
`Surv.Core`: krzywa XP z danych, nadmiar XP, kolejka awansów. Losowanie kart: pula (nowe przy wolnym slocie, +1 poziom poniżej 5, ewolucje), wagi, ewolucja zawsze wśród kart, mniej niż 3 → tyle, ile jest, brak → złoto. Ekran wyboru zatrzymuje grę (`Time.timeScale = 0`, Sim nie liczy).
Testy: EditMode, każda reguła W15 osobno z ustalonym ziarnem; 6+6 slotów; pełne sloty → złoto.
Umiejętność założonej broni jest w puli jako +1 poziom (slot 1). Bronie niezałożone (B2, B3) nie pojawiają się jako karty. Gdy warunek spełnia kilka ewolucji, wszystkie są wśród kart (do 3), a nadmiar losowo. `(decyzja autora)`

**P10. Model statystyk i 12 pasywnych.** Serves: W16 (pasywne), W30 (łączenie z talentami).
Statystyki gracza jako suma bazowych + ekwipunek + talenty + pasywne, z mnożeniem bonusów (W30). Wartości Lv2–4 liniowo.
Testy: EditMode, liniowe Lv2–4; mnożenie talent × pasywna.
Uwaga do SPEC W16: pasywka „prędkość ruchu” jest w F1, ale jej para (piłka, A12) jest w F2, więc w F1 nie ma ewolucji. To nie zmienia wymagań (pasywka jest dostępna jako karta), poprawia tylko uzasadnienie w SPEC. `(decyzja autora)`

**P11. Umiejętności aktywne F1.** Serves: W4, W16 (A1, A2, A3, A5, A6, A8, A10, A11), W22 (B2, B3).
Wspólny interfejs zachowania wzorca. Statystyki na poziomach z danych (mnożniki z RESEARCH §5.1). Reguła celowania w danych. Kierunek „przed postacią” = ostatni kierunek ruchu, domyślnie w górę.
Testy: EditMode na statystykach poziomów odczytanych z danych; PlayMode dymny: każda umiejętność na poziomie 1 i 5 zadaje obrażenia wrogowi w zasięgu.
Model umiejętności według D13. Czasy odnowienia 1–4 s według wzorca, wartości B2 i B3 ustala autor w danych. Pasywka czasu odnowienia skraca też tick aury, a czas trwania wydłuża podpalenie i orbitę. `(decyzja autora)`

**P12. Ewolucje i skrzynie w runie.** Serves: W15 (skrzynie), W17 (F1).
Tabela par z W17 i bronie z W22 (B1–B3). Ewolucja zastępuje aktywną w slocie, pasywna zostaje. Skrzynia z elity i bossa pośredniego: 1/3/5 ulepszeń (70/25/5), ewolucja ma pierwszeństwo, brak ulepszeń → złoto, animacja, strzałka do skrzyni poza ekranem.
Testy: EditMode, warunek ewolucji (Lv5 + para na dowolnym poziomie); priorytet ewolucji w skrzyni; rozkład 1/3/5 na 10 000 losowań ±2 pp.
11 ewolucji F1 (8 aktywnych A + 3 bronie) jako warianty w danych: nowa nazwa, ikona, znacznik EVO, wartości z kolumny EVO w RESEARCH §5.1. Gdzie RESEARCH opisuje zachowanie ogólnie, ewolucja = mocniejsze wartości + zmiana efektu wizualnego + jedna zmiana zachowania (np. przebijanie). `(decyzja autora)` Test EditMode: każda z 11 ma parę, a wartość EVO ≥ wartość Lv5.

**P13. Przedmioty na mapie.** Serves: W18.
Niszczalne obiekty (częstość z danych), magnes, bomba (zwykli wrogowie na ekranie giną i upuszczają kryształy; elity i bossowie −10% maks. HP), mięso 30%, złoto. Zniknięcie po 60 s poza ekranem.
Testy: EditMode na efekcie bomby i zniknięciu po 60 s (zegar wstrzyknięty).
Licznik 60 s liczy czas ciągłego przebywania poza ekranem i zeruje się, gdy przedmiot wraca na ekran. `(decyzja autora)`

**P14. HUD i pauza.** Serves: W5, W44 (pauza).
Licznik mm:ss w górę, pasek XP z poziomem, zabici, złoto. Pauza: umiejętności z poziomami, Wznów, Ustawienia (zaślepka do P26), Wyjdź → porażka z częściowymi nagrodami. Przejście aplikacji w tło → pauza.
Alarm W11: pasek z napisem na środku górnej części ekranu (dźwięk w P25). Elementy UI P14 sprawdzane ręcznie na telefonie (lista w P29).

### E3. Struktura runu (L)

**P15. Harmonogram osi czasu.** Serves: W6, W7, W8, W10 (reguły czasu), W11.
`Surv.Core`: oś czasu rozdziału z danych (zdarzenia: zmiana tempa spawnu i typów, horda, elita, boss), licznik biegnie w arenie, zdarzenia nie-bossowe w arenie pomijane, boss opóźniony do końca areny, na mapie zamkniętej żywy boss opóźnia następnego, alarm 3 s przed hordą i bossem (nie elitą). Walidator osi czasu: reguły W6 (bossowie w 5/10/15, ≥3 elity i ≥3 hordy, w każdym odcinku między bossami co najmniej 1 elita albo horda, po każdym bossie pośrednim nowy lub wzmocniony typ wroga, mnożnik siły niemalejący w czasie) i W7 (bossowie 1:30/3:00/4:30/6:00/8:00, w każdym z 4 odcinków między kolejnymi bossami ≥2 hordy i 1 elita). Boss opóźniony przez arenę: alarm w chwili końca areny, boss wchodzi 3 s później. `(decyzja autora)`
Testy: EditMode na każdej regule, w tym boss 1 żyjący do 10:30 → boss 2 wchodzi po jego śmierci; walidator odrzuca złą oś czasu.

**P16. Formaty map.** Serves: W3, W7, W8, W9, W19.
Otwarta (nieskończone kafelki podłoża), zamknięta (3×3 ekrany, ściany dla gracza i Sim, kamera przycięta do granic, brak przeniesień), korytarz (szerokość 1,5 ekranu, kamera tylko w pionie, spawn z góry, dołu i boków). Ekran rozdziału pokazuje format, czas i liczbę bossów.
Testy: PlayMode, gracz nie przechodzi przez granicę zamkniętej mapy i korytarza; kamera nie wychodzi poza mapę zamkniętą.
Mapa zamknięta przy bossie: jak arena W10 bez koła. Kamera dalej śledzi gracza w granicach mapy, zdarzenia nie-bossowe są pomijane, boss pośredni zostawia skrzynię. `(decyzja autora)`

**P17. Typy wrogów 3–5, elity, pociski wrogów.** Serves: W20 (1–5), W21.
Dystansowy (zatrzymanie w zasięgu, wolne pociski w Sim), tank, szarżujący (zapowiedź ≥ 0,7 s, linia prosta). Elity jako wzmocnione warianty z danych, upuszczają skrzynię. Pociski wrogów: inny kolor i obrys niż pociski gracza.
Testy: EditMode na maszynie stanów szarżującego (zapowiedź ≥ 0,7 s przed ruchem).

**P18. Bossowie i arena.** Serves: W10, W12, W5 (pasek HP bossa).
Framework bossa: lista ataków z danych, każdy z zapowiedzią (czerwony obszar lub linia ≥ 0,7 s) przy obrażeniach ≥ 15% bazowego HP gracza. Salwy i odbijające się pociski z obrażeniami ≥ 15% mają zapowiedź liniami kierunków przed wystrzałem. Kontakt z bossem zadaje < 15%. Przywołanie sług jest atakiem bossa (nie spawnem osi czasu), a słudzy znikają przy śmierci bossa. `(decyzja autora)` Arena: znika tłum, pociski wrogów i elity, kryształy i przedmioty zostają, koło ~80% szerokości ekranu, kamera stoi. Śmierć bossa pośredniego → skrzynia, finałowego → zwycięstwo. **11 bossów F1** (3 + 3 + 5), każdy z ≥ 2 atakami, z kombinacji wspólnych klocków ataków (szarża, salwa pocisków, odbijające się pociski, obszar na ziemi, przywołanie), żeby nie pisać 11 osobnych.
Testy: PlayMode, wejście bossa czyści tłum i zostawia kryształy; gracz nie wychodzi z koła; zejście z zapowiedzianego obszaru = 0 obrażeń; walidator danych: każdy atak ≥ 15% HP ma zapowiedź ≥ 0,7 s, a każdy boss ma ≥ 2 ataki.

### E4. Meta (L)

**P19. Zapis.** Serves: W42.
D9. Zapis po każdej zmianie mety. Przerwanie w trakcie zapisu zostawia poprzedni plik. Wersja 1 formatu + szkielet migracji. Run w trakcie nie jest zapisywany (zabicie aplikacji = run stracony).
Testy: EditMode, uszkodzony plik główny → wczytanie z `.bak`; plik wersji 0 (testowy) → migracja do 1; zapis i odczyt dają ten sam stan.
Test ręczny: `adb install -r` nowszego buildu zachowuje postęp (W42).

**P20. Model ekwipunku.** Serves: W22, W23, W24, W25.
`Surv.Core`: typy przedmiotów, 15 stopni, merge według tabeli W23 (identyczny/dowolny, przedmiot główny, założony tylko jako główny, zachowanie poziomu, zwrot kosztów składników), „Połącz wszystko” do doskonałego, statystyki stopni (broń 10…231, legendarne +N po +10%), bonusy stopni, ulepszanie (koszt rosnący, limity 10…100, „Ulepsz maksymalnie”, reset zwraca 100%), broń wyznacza umiejętność slotu 1 (B1 domyślna).
Testy: EditMode, każdy wiersz tabeli merge'a (poprawne i błędne składniki), łańcuch „Połącz wszystko”, limity poziomów dla 15 stopni, zwrot przy resecie, bonus „doskonały = start z Lv2”.
Zakres danych F1: 3 typy broni i 2 typy w każdym z 5 pozostałych slotów (13 typów). Skala statystyk: sloty ATK jak broń, sloty HP ×10 skali broni. Bonusy sytuacyjne (3 na typ nie-broniowy) przez mały silnik wyzwalaczy (zdarzenie, np. „zjedzono mięso” → efekt z danych, limit). Zachowania legendarne B1–B3 jako dodatkowe efekty trafienia. `(decyzja autora)` Test EditMode: każdy wyzwalacz z danych ma obsługę w silniku.

**P21. Ekrany ekwipunku.** Serves: W22, W23, W24, W25.
Lista przedmiotów z ramkami (6 kolorów + znaczniki +N), karta przedmiotu (statystyki, bonusy z wyszarzeniem i stopniem odblokowania), zakładanie, merge z podświetleniem składników i podglądem wyniku, „Połącz wszystko”, ulepszanie i reset.
Sprawdzenie ręczne na telefonie (lista w P29).

**P22. Nagrody, ekran końca, poziom konta.** Serves: W28, W29.
Porażka: proporcja czas/pełny czas mapy, plany z szansą, bez przedmiotów. Wygrana: pełne złoto i XP (1000 + 150 × (N−1)), 1–3 przedmioty, plany. Ekran końca z przyciskami „Dalej” i „Zagraj ponownie”. Poziom konta do 10, awans daje złoto.
Testy: EditMode na formułach i limicie 10.
Złoto zebrane w runie liczy się w całości także przy porażce. Proporcja czas/pełny czas dotyczy premii złota i XP konta. Tabela stopni przedmiotów i szanse planów na rozdział są w danych. `(decyzja autora)`

**P23. Talenty F1.** Serves: W30 (F1).
6 talentów statystyk za złoto, poziom ≤ poziom konta, koszty i przyrosty z danych, talent mięsa +5% leczenia na poziom.
Testy: EditMode, blokada powyżej poziomu konta.
Talenty statystyk to procenty. Talent mięsa: leczenie = 30% × (1 + 0,05 × poziom). `(decyzja autora)`

**P24. Rozdziały, ekran główny, odblokowania.** Serves: W26, W37, W38.
3 rozdziały (1 otwarty, 2 korytarz, 3 zamknięty), liniowe odblokowanie, mnożnik HP i obrażeń ≥ 1,5× na rozdział, najlepszy czas. Ekran główny F1: postać, rozdział z „Graj”, przełączanie, waluty, menu Ekwipunek / Graj / Talenty. Brak elementów F2. Pierwsze uruchomienie: ekran startowy → run rozdziału 1 z podpowiedziami, flaga po pierwszym zakończonym runie, kłódki z podpisem.
Testy: EditMode na odblokowaniu, najlepszym czasie i walidatorze mnożników; PlayMode na przepływie pierwszego uruchomienia.

### E5. Treść, oprawa, akceptacja (L)

**P25. Oprawa.** Serves: W39, W21.
Błysk 0,1 s (w Sim jako czas błysku na instancji), animacja śmierci, dźwięk na umiejętność i ewolucję, wibracje przy trafieniu gracza (opcjonalne).
Dźwięk alarmu W11. Źródło audio: paczki CC0 albo kupione z licencją komercyjną, wpisane do listy W40. Ikona i nazwa aplikacji: robocze, na liście W40.

**P26. Ustawienia i lokalizacja.** Serves: W44.
Głośność muzyki i efektów, wibracje, liczby obrażeń, język PL/EN (domyślnie z systemu), zmiana na żywo, dostęp z ekranu głównego i pauzy. Wszystkie teksty w tabelach Localization.
Testy: EditMode, test skanujący sceny, prefaby i ScriptableObjecty: żaden tekst widoczny dla gracza nie jest bez klucza lokalizacji; test wyboru języka (PL → PL, EN → EN, inny → EN).

**P27. Treść 3 rozdziałów i grafika.** Serves: W6, W7, W8, W26, W40.
Wybór paczek grafiki 2D w jednym stylu (P14 w SPEC, kupuje autor). Dla każdego rozdziału: motyw strefy planety, zestaw wrogów (≥ 3 typy), własna oś czasu przechodząca walidator z P15, bossowie z P18. Lista kontrolna W40 przed każdym buildem testowym, w pliku `game/CHECKLIST-W40.md`.

**P28. Balans.** Serves: definicja ukończenia F1, W14.
Strojenie danych: poziomy 12/22/30 (±3) w 5/10/15 min na 3 runach z bronią domyślną; rozdział 1 wygrany w ≤ 3 próbach na nowym zapisie; rozdział 3 nie do wygrania w 3 próbach bez ulepszeń.
Kryterium wymaga gry autora (definicja ukończenia F1). Asystent przygotowuje zmiany danych, autor gra i zgłasza wyniki.

**P29. Akceptacja F1.** Serves: wszystkie W z F1.
Przejście listy W1–W45 (części F1) na telefonie z Androidem. Wynik każdego W w `## Realizacja` (test automatyczny, test ręczny albo pomiar).
Zawiera: ponowny pomiar W43 po E2, E3 i na końcu; sprawdzenie, że manifest Androida nie ma uprawnienia INTERNET (W45); ręczną listę ekranów UI (HUD, pauza, karty, skrzynia, koniec runu, ekwipunek, merge, talenty, ekran główny, ustawienia).

### Ryzyka techniczne i zapasowe drogi

**P30. Zapasowe renderowanie (warunkowe).** Serves: W43. Uruchamiane tylko, gdy P7 nie przejdzie z wariantem A.
**P31. Zapasowe środowisko (warunkowe).** Serves: W45. Uruchamiane tylko, gdy P1 nie działa na Ubuntu 26.04: maszyna wirtualna Ubuntu 24.04 z przekazaniem USB dla `adb`, albo edytor na Windows.
**P32. Repozytorium git (warunkowe).** Uruchamiane tylko po zgodzie autora: `git init` w katalogu głównym, LFS, bez zdalnego repozytorium, dopóki autor nie wskaże.

## Mapowanie wymagań

| W | Plan | Dowód |
|---|---|---|
| W1 | P3 | PlayMode (dotyk) + ręcznie |
| W2 | P2, P3 | ręcznie (obrót, 3 proporcje) |
| W3 | P3, P16 | PlayMode (granice kamery) + pomiar 2 s ręcznie |
| W4 | P5, P11 | EditMode (cel), PlayMode (dymny) |
| W5 | P14, P18 | ręcznie |
| W6 | P15, P27 | EditMode (walidator) |
| W7 | P15, P16, P27 | EditMode (walidator, opóźnienie bossa), PlayMode (granice) |
| W8 | P15, P16, P27 | EditMode (walidator), PlayMode (granice) |
| W9 | P16 | ręcznie |
| W10 | P15, P18 | EditMode (harmonogram), PlayMode (arena) |
| W11 | P15 | EditMode (alarm 3 s, bez elit) |
| W12 | P18 | EditMode (walidator danych), PlayMode (zejście z obszaru) |
| W13 | P8, P14 | EditMode + ręcznie |
| W14 | P5, P9, P28 | EditMode (krzywa), pomiar poziomów |
| W15 | P9, P12 | EditMode (reguły kart, skrzynie) |
| W16 | P9, P10, P11 | EditMode, PlayMode |
| W17 | P12 | EditMode |
| W18 | P13 | EditMode |
| W19 | P4, P16 | EditMode |
| W20 | P4, P17 | EditMode (szarża) + ręcznie |
| W21 | P17, P25 | ręcznie (test 5 klatek) |
| W22 | P11, P20, P21 | EditMode |
| W23 | P20, P21 | EditMode (każdy wiersz) |
| W24 | P20, P21 | EditMode |
| W25 | P20, P21 | EditMode |
| W26 | P24, P27 | EditMode (odblokowanie, mnożniki) |
| W28 | P22 | EditMode |
| W29 | P22 | EditMode |
| W30 | P10, P23 | EditMode |
| W37 | P24 | PlayMode (pierwsze uruchomienie) |
| W38 | P24 | ręcznie |
| W39 | P25 | ręcznie |
| W40 | P27 | lista kontrolna |
| W41 | P2, D8 | EditMode (walidacja danych) |
| W42 | P19 | EditMode |
| W43 | P4–P7, P30 | pomiar na telefonie |
| W44 | P14, P26 | EditMode (skan tekstów) + ręcznie |
| W45 | P1, P2 | build na telefonie |

W27, W31–W36 są w F2 i nie mają pozycji w tym planie.

## Review Focus

Miejsca, gdzie błąd jest najbardziej prawdopodobny i najdroższy:
- harmonogram osi czasu z areną (P15): opóźnianie bossów i pomijanie zdarzeń,
- tabela merge'a (P20): rozróżnienie „identyczny” i „dowolny”, zwrot kosztów,
- zapis atomowy i migracje (P19),
- wydajność Sim po dodaniu wszystkich umiejętności (powtórzyć pomiar P7 po E2 i E3).

## Pytania

Każde ma rekomendację. „tak” ją przyjmuje.

**Q1. Decyzje D1–D14.** Przyjmujesz je jak w tabeli?
Rekomendacja: tak.
Odpowiedź: tak, D1–D14 (2026-11-16, użytkownik).

**Q2. Instalacja.** Komendy wymagające `sudo` (repozytorium apt Unity, Unity Hub, git-lfs) przygotuję, a ty je uruchomisz. Instalację edytora i modułu Android zrobisz w Hubie (wymaga logowania na konto Unity, którego ja nie wykonuję).
Rekomendacja: tak.
Odpowiedź: tak (2026-11-16, użytkownik).

**Q3. Git.** Zakładamy lokalne repozytorium z Git LFS w `~/Developer/private/surv20` przy P2, bez zdalnego repozytorium?
Rekomendacja: tak. Commity robię tylko na twoje polecenie.
Odpowiedź: tak (2026-11-16, użytkownik).

**Q4. Telefon testowy.** Jaki masz telefon z Androidem do testów? Wg SPEC wzorzec średniej klasy to Pixel 6a / Galaxy A54.
Rekomendacja: podaj model. Jeśli jest wyraźnie mocniejszy, bramkę W43 sprawdzimy dodatkowo z obniżonym taktowaniem (tryb oszczędzania energii).
Odpowiedź: „tak” bez modelu (2026-11-16, użytkownik). Do P7 przyjmujemy klasę Pixel 6a. Model telefonu trzeba podać przed P1 (pierwszy build na telefonie).

**Q5. Kolejność.** E0 → E1 (bramka) → E2 → E3 → E4 → E5, bez równoległych etapów?
Rekomendacja: tak. Jedna osoba, a bramka E1 może zmienić renderowanie.
Odpowiedź: tak (2026-11-16, użytkownik).

## Ryzyka

- **Ubuntu 26.04 nie jest wspierane przez Unity** (oficjalnie 22.04/24.04, X11). Zapasowa droga: P31.
- **11 bossów w F1** to największa część pracy nad treścią. Klocki ataków z P18 mają to ograniczyć.
- **Paczki grafiki:** spójność stylu i licencja z prawem do użytku komercyjnego (W40). Wybór przed P27, ale zastępcze kształty od E1.
- **Burst i Jobs to nowe API dla autora.** P4 jest celowo wcześnie, żeby uczyć się na prototypie.
- Rozmiary S/M/L to szacunek autora planu, nie zobowiązanie.
- Test czytelnika planu: 24 punkty. Już znane: 3 (P2 a Q3, pomiar w buildzie deweloperskim, kolejność zależności przez zaślepki D14). Drobne, wpisane jako `(decyzja autora)`: 20. Do rozstrzygnięcia przez użytkownika: 1 (Q4, telefon, model do podania przed P1).
- Dwie interpretacje SPEC (bez zmiany wymagań): kolor kryształu z elity (P5) i uzasadnienie pasywki prędkości (P10).

## Odstępstwa

- 2026-11-16 | Logika `Surv.Core` (P9, P10, P12, P15, P19, P20, P22–P24 w częściach bez Unity) powstaje przed E0/E1, w czystym C# z testami NUnit uruchamianymi przez .NET SDK, bez edytora Unity. Powód: edytor i telefon nie są jeszcze gotowe, a bramka E1 może zmienić tylko renderowanie tłumu, nie reguły gry. | zatwierdzone przez użytkownika: tak
  - Układ: kod w `core/com.surv.core/Runtime/` jako lokalny pakiet Unity (później dołączony w `game/Packages/manifest.json` przez `file:`), projekt `core/build/Surv.Core.csproj` (netstandard2.1, C# 9, te same pliki) i testy `core/tests/Surv.Core.Tests/` (NUnit 3). Ograniczenia zgodności z Unity 6: C# 9, .NET Standard 2.1, bez `record` i `init`, bez System.Text.Json (zapis przez Newtonsoft.Json, jak D9).
  - Testy EditMode z planu dla tych pozycji są uruchamiane przez `dotnet test` zamiast Test Runnera Unity. Ścieżka Unity: P2 dołączy pakiet i może dodatkowo uruchomić te same testy w Unity.

## Realizacja 2026-11-16
branch: surv20 main @ brak commita (zmiany lokalne, niezacommitowane)
P20: done (Core) | core/com.surv.core/Runtime/Equipment/* | ./core/test.sh: Equipment/* green (merge każdy wiersz W23, Połącz wszystko, limity 15 stopni, reset, statystyki)
P15: done (Core) | core/com.surv.core/Runtime/Run/Timeline*.cs | ./core/test.sh: Run/TimelineTests green
P9: done (Core: XP, karty, sloty) | Run/Experience.cs, Run/Cards.cs, Run/Loadout.cs, Run/Skills.cs | ./core/test.sh: Run/SkillTests green
P12: done (Core: ewolucje, skrzynia) | Run/RunChest.cs, Run/Loadout.cs | ./core/test.sh: Run/SkillTests green
P5: częściowo (Core: CrystalDrops) | Run/Experience.cs | ./core/test.sh green; reszta P5 wymaga Unity
P8: częściowo (Core: PlayerHealth, ContactHit) | Run/Stats.cs | ./core/test.sh green; reszta wymaga Unity
P10: done (Core) | Run/Stats.cs | ./core/test.sh: Meta/ProgressTests green
P19: done (Core) | core/com.surv.core/Runtime/Save/* | ./core/test.sh: Save/SaveStoreTests green
P22: częściowo (Core: RewardCalculator, AccountProgress) | Meta/Progress.cs | ./core/test.sh green; ekran końca runu wymaga Unity
P23: częściowo (Core: TalentBook) | Meta/Progress.cs | ./core/test.sh green; ekran talentów wymaga Unity
P24: częściowo (Core: ChapterProgress, walidator mnożników) | Meta/Progress.cs | ./core/test.sh green; ekrany wymagają Unity
P1–P4, P6, P7, P11, P13, P14, P16–P18, P21, P25–P31: not done (wymagają edytora Unity / telefonu)
suite: core ./core/test.sh rc=0, 110 testów, red: brak
focus: tabela merge'a | Excellent_plus_two_takes_two_any_excellent | mutation MergeRules (2→1 doskonałe dla doskonały+1) w kopii w scratchpadzie: dotnet test rc≠0 → red
focus: harmonogram z areną | Boss_falling_due_during_a_fight_waits_then_enters_three_seconds_after_it | mutation (bez opóźnienia 3 s) → red
focus: zapis atomowy | Corrupt_main_file_loads_the_backup, Crash_between_the_two_moves_falls_back_to_the_backup | mutation (bez kopii .bak) → red
deviation: logika Core przed E0/E1, testy przez dotnet zamiast Test Runnera Unity | zatwierdzone przez użytkownika: tak
deviation: wartości domyślne do strojenia w kodzie fabryk (EquipmentBalance.CreateDefault, RewardBalance, ExperienceBar.DefaultCurve, AccountProgress.DefaultCurve) do czasu ScriptableObjectów z D8 | zatwierdzone przez użytkownika: nie (do decyzji przy P2)
scaffolding: brak (core/ zostaje docelowo jako lokalny pakiet Unity)
