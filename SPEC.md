status: approved | 2026-11-16 | stream: n/d (projekt prywatny) | zmiana W41, W45 (stos) zatwierdzona przez użytkownika 2026-11-16
repos: surv20 (github.com/fpodsiadly/survivor2) main

# surv20 (nazwa robocza): specyfikacja

Repo: brak (projekt prywatny). Ticket: brak. Wzorzec mechanik: Survivor.io (Habby, 2022), research w [RESEARCH.md](RESEARCH.md) (§1–4 runda 1, §5 runda 2 z danymi z wiki). Stos: Kotlin Multiplatform + Compose Multiplatform, tak jak w grze JumperIdle (zmiana z Unity 6 na polecenie użytkownika, 2026-11-16). Przykład kolegi `barteekpl/JumperIdle` jest niedostępny (404).

Wersja 2 (2026-11-16): użytkownik chce gry „maksymalnie podobnej do Survivor.io”. Wersja 1 była uproszczonym MVP (10 min, 4 rzadkości, bez energii). Ta wersja odwzorowuje **mechaniki i systemy** Survivor.io z okresu premiery (sierpień–grudzień 2022), z jego zasadami i liczbami tam, gdzie są znane.

## Granica podobieństwa (obowiązuje każde wymaganie)

- **Odwzorowujemy:** zasady, systemy, struktury, rytm i liczby: długość runów, harmonogram bossów, sloty, wzorce zachowań umiejętności, tabelę ewolucji (pary wzorców), drabinę rzadkości i zasady merge'a, energię, Patrol, Próby, misje, sklep, kolejność odblokowań.
- **Nie przenosimy:** nazwy gry, nazw postaci, wrogów, bossów, umiejętności, przedmiotów i rozdziałów, grafiki, wyglądu UI, ikon, muzyki, dźwięków, tekstów ani konkretnych poziomów (ich osi czasu z tymi samymi bossami). Wszystko to jest własne (W40).

## Problem

Chcę grać na Androidzie i iOS w grę, która gra się jak Survivor.io: jeden palec, auto-atak, fale wrogów, wybór 1 z 3 umiejętności, ewolucje, bossowie w arenie. Między runami rozwijam postać przez ekwipunek z merge'em, talenty, Patrol i Próby. Ma to być własna gra z własnym światem, nie kopia.

## Fazy

Każde wymaganie ma fazę. Faza N+1 zaczyna się po spełnieniu wszystkich wymagań fazy N.
- **F1 (grywalne MVP):** pełny run, ekwipunek z merge'em, talenty, 3 rozdziały (po jednym z każdego formatu mapy), zapis lokalny, Android.
- **F2 (pełna meta z premiery):** cała zawartość umiejętności, 10 rozdziałów, poziom konta, energia, Patrol, Próby, misje, sklep i skrzynie, kamienie milowe rozdziałów, wskrzeszenie, iOS.
- **F3 (monetyzacja i systemy późniejsze):** reklamy z nagrodą, zakupy, battle pass, później zwierzaki, części technologiczne, postaci. Wymagania F3 powstaną osobną specyfikacją (P22).

## Założenia dla wartości

Liczby pochodzą z RESEARCH §5 tam, gdzie są znane. Pozostałe są oznaczone `(decyzja autora)` i są punktem startowym do strojenia. Każda liczba z tego dokumentu jest w danych balansu, nie w kodzie (W41).

## Wartości bazowe (start do strojenia)

`(decyzja autora)`, wszystkie w danych (W41):
- gracz: 100 HP, ATK = suma ATK z ekwipunku (broń domyślna 10), prędkość 3 jednostki/s, promień zbierania 1,5 jednostki, ekran referencyjny ma 10 jednostek szerokości,
- wolny piechur: 60% prędkości gracza, kontakt zadaje 10% bazowego HP gracza na sekundę,
- maks. liczba żywych wrogów: 400 (nadwyżka czeka w kolejce spawnu),
- czasy odnowienia umiejętności: 1–4 s zależnie od wzorca.

---

## Wymagania

### A. Sterowanie i ekran

**W1. Pływający joystick jednym palcem.** F1
Dotknięcie dowolnego miejsca dolnych 2/3 ekranu w runie pokazuje joystick w punkcie dotyku. Przesuwanie palca porusza postać w tym kierunku z prędkością proporcjonalną do wychylenia do maksimum. Puszczenie zatrzymuje postać i chowa joystick. Steruje tylko pierwszy palec. Jedyny inny przycisk w runie to pauza. Maks. promień wychylenia i martwa strefa są w danych.
Drugi palec jest ignorowany, dopóki pierwszy nie zostanie podniesiony. Następne dotknięcie tworzy nowy joystick. Na ekranie wyboru kart może być przycisk odświeżenia (W15, F2). `(decyzja autora)`
Źródło: RESEARCH §1. Wariant pływający: `(decyzja autora)`.

**W2. Orientacja pionowa.** F1
Tylko pion. Obrót telefonu nic nie zmienia. UI mieści się w bezpiecznym obszarze ekranu przy proporcjach od 16:9 do 20:9.
Źródło: P3.

**W3. Kamera.** F1
Na mapie otwartej kamera trzyma gracza na środku ekranu. Na mapie zamkniętej (W7) kamera śledzi gracza, ale nie wychodzi poza granice mapy. Na korytarzu (W8) kamera śledzi gracza tylko w pionie. Kamera jest oddalona tak, że wróg o prędkości bazowej (prędkość wolnego piechura, W20) idzie z krawędzi ekranu do gracza co najmniej 2 s.
Jednostka „ekran” w tym dokumencie = szerokość widoku świata przy referencyjnej proporcji 9:16. `(decyzja autora)`
Źródło: RESEARCH §1. Próg 2 s: `(decyzja autora)`.

### B. Struktura runu

**W4. Auto-atak.** F1
Każda posiadana umiejętność aktywna działa sama w swoim cyklu odnowienia, bez akcji gracza, według reguły celowania jej wzorca (W16). Gdy na ekranie nie ma wrogów, umiejętności celujące nie strzelają.
Reguła celowania jest w danych każdej umiejętności. Domyślnie: najbliższy wróg. Umiejętności „przed postacią” (np. B2) używają ostatniego kierunku ruchu, a przed pierwszym ruchem kierunku w górę. `(decyzja autora)`
Źródło: RESEARCH §1.

**W5. Licznik i HUD.** F1
U góry ekranu: licznik czasu mm:ss liczący w górę, pasek XP z poziomem, liczba zabitych i zebrane złoto. Pasek HP jest pod postacią. Pauza zatrzymuje licznik. Licznik nie zatrzymuje się na czasie bossa finałowego: walka z nim może przedłużyć run. Czas przetrwania = stan licznika w chwili końca runu.
Źródło: RESEARCH §1.

**W6. Mapa otwarta: 15 minut, 3 bossy.** F1
Mapa nieskończona w obu osiach. Oś czasu każdego rozdziału tego formatu spełnia reguły:
- bossowie wchodzą w 5:00, 10:00 i 15:00 (finałowy),
- co najmniej 3 elity i co najmniej 3 hordy, rozłożone między bossami, w czasach wybranych dla rozdziału,
- po każdym bossie pośrednim dochodzi nowy typ wroga albo wzmocniona wersja istniejącego,
- liczba żywych wrogów i ich siła rosną z czasem.

Konkretne czasy elit i hord, typy wrogów i bossowie są projektowane osobno dla każdego rozdziału (W26). Oś czasu żadnego rozdziału nie powtarza osi czasu rozdziału Survivor.io (granica podobieństwa).
Źródło: RESEARCH §1 i §5.4 (format 15 min, 3 bossy). Reguła o hordach i elitach: `(decyzja autora)`.

**W7. Mapa zamknięta: 8 minut, 5 bossów.** F1
Prostokątna mapa o rozmiarze około 3 × 3 ekrany z granicami, przez które nie przechodzą gracz ani wrogowie. Bossowie wchodzą w 1:30, 3:00, 4:30, 6:00 i 8:00 (finałowy), a run kończy się śmiercią bossa finałowego. Między bossami są co najmniej 2 hordy i 1 elita. Bossowie walczą bez osobnego koła areny: zwykli wrogowie znikają jak w W10, a granicą jest brzeg mapy. Żywy boss opóźnia wejście następnego do swojej śmierci.
Źródło: RESEARCH §5.4. Czasy bossów i rozmiar: `(decyzja autora)`.

**W8. Korytarz pionowy: 15 minut, 3 bossy.** F1
Mapa ma stałą szerokość około 1,5 ekranu i jest nieskończona w górę i w dół. Oś czasu spełnia reguły W6. Wrogowie wchodzą od góry i od dołu oraz z boków widocznego fragmentu.
Źródło: RESEARCH §5.4 (rozdział 4). Szerokość: `(decyzja autora)`.

**W9. Formaty map.** F1
Każdy rozdział ma jeden z formatów W6, W7, W8. Ekran rozdziału pokazuje format, czas trwania i liczbę bossów.
Źródło: RESEARCH §5.4.

**W10. Arena bossa.** F1
Gdy wchodzi boss na mapie otwartej lub korytarzu:
- znikają zwykli wrogowie, elity i pociski wrogów (bez nagrody),
- kryształy XP i przedmioty na mapie zostają,
- wokół aktualnej pozycji gracza pojawia się nieruchome koło o średnicy około 80% szerokości ekranu, z którego gracz nie wyjdzie,
- kamera staje w środku koła, a nowi wrogowie się nie pojawiają,
- u góry ekranu jest pasek HP bossa.

Po śmierci bossa pośredniego koło znika, spawn wraca do osi czasu, a w miejscu bossa zostaje skrzynia (W15). Śmierć bossa finałowego kończy run zwycięstwem.
Licznik czasu biegnie w trakcie areny. Zdarzenia inne niż bossowie przypadające na czas areny są pomijane. Boss, którego czas wypada w trakcie areny, wchodzi zaraz po jej zakończeniu, więc każdy run ma wszystkich bossów. Wrogowie znikają w chwili wejścia bossa (po alarmie). `(decyzja autora)`
Źródło: RESEARCH §1. Rozmiar koła i pomijanie zdarzeń: `(decyzja autora)`.

**W11. Alarm.** F1
3 s przed hordą lub bossem pojawia się ostrzeżenie (napis i dźwięk). Elity nie mają alarmu. Czas w osi czasu to chwila wejścia. Horda to jednorazowa grupa wrogów (liczebność w danych), która na mapie otwartej wchodzi z co najmniej 3 stron, na korytarzu z obu końców, a na mapie zamkniętej z co najmniej 3 brzegów.
Źródło: RESEARCH §1.

**W12. Ataki bossów.** F1
Każdy atak bossa zadający co najmniej 15% bazowego maks. HP gracza ma zapowiedź (czerwony obszar lub linia) trwającą co najmniej 0,7 s. Gracz, który zejdzie z zapowiedzianego obszaru, nie dostaje obrażeń. Każdy boss ma co najmniej 2 rodzaje ataku.
„Bazowe maks. HP gracza” to wartość z sekcji Wartości bazowe.
Źródło: RESEARCH §1. Progi: `(decyzja autora)`.

**W13. HP, obrażenia, koniec runu.** F1
Kontakt z wrogiem lub jego pociskiem odejmuje HP. Po trafieniu gracz ma 0,5 s nietykalności z miganiem postaci. HP = 0 → wskrzeszenie (W33, F2) albo porażka. Pokonanie bossa finałowego → zwycięstwo i rozdział oznaczony jako ukończony. Wyjście z runu z pauzy liczy się jak porażka.
Wyjście z pauzy daje częściowe nagrody jak porażka (W28). Zabicie aplikacji nie daje nic (W42).
Źródło: RESEARCH §1. Nietykalność: `(decyzja autora)`.

### C. Poziomy i umiejętności w runie

**W14. Kryształy XP i awans.** F1
Zabity wróg upuszcza kryształ XP: zielony (zwykli wrogowie), niebieski (wzmocnieni i elity), żółty (rzadki, duży). Domyślne wartości: 1 / 5 / 25 XP. Kryształ w promieniu zbierania leci do gracza. Pełny pasek = awans, nadmiar przechodzi dalej. Krzywa XP rośnie tak, że gracz bez bonusów XP osiąga około 12. poziomu w 5:00, 22. w 10:00 i 30. w 15:00 na mapie otwartej.
Żółty kryształ wypada z szansą 2% z każdego zwykłego wroga i zawsze z elit i bossów. Wrogowie zabici bombą upuszczają kryształy. Wrogowie usunięci przy wejściu bossa nie upuszczają. Cele poziomów (12/22/30) sprawdza się z tolerancją ±3 poziomy na 3 runach testowych autora z bronią domyślną. `(decyzja autora)`
Źródło: RESEARCH §1 (kolory). Wartości i krzywa: `(decyzja autora)`.

**W15. Karty przy awansie i skrzynie.** F1
- **Awans:** gra się zatrzymuje i pokazuje 3 różne karty. Karta to nowa umiejętność (gdy jest wolny slot jej typu), +1 poziom posiadanej (gdy nie ma poziomu 5) albo ewolucja (gdy spełniony W17). Karta pokazuje ikonę, nazwę, poziom w gwiazdkach i jedno zdanie efektu. Każda dostępna karta ma domyślnie wagę 1 (wagi można zmienić w danych). Gdy dostępnych kart jest mniej niż 3, pokazane są tylko dostępne. Ewolucja, która spełnia warunek, zawsze jest jedną z kart. Brak dostępnych kart → złoto (kwota w danych). Kilka awansów naraz pokazuje kolejne wybory po kolei.
- **Reroll:** domyślnie brak. Talent DNA „odświeżenie” (W30) daje 1 odświeżenie kart na run.
- **Skrzynia z elity i bossa pośredniego:** podniesienie zatrzymuje grę i pokazuje animację losowania. Skrzynia daje 1, 3 albo 5 ulepszeń z szansami 70/25/5% (w danych). Każde ulepszenie to losowe +1 poziom posiadanej umiejętności aktywnej lub pasywnej albo ewolucja (liczy się jako jedno ulepszenie, ma pierwszeństwo). Gracz nie wybiera. Gdy nie ma czego ulepszać, daje złoto. Strzałka przy krawędzi ekranu wskazuje skrzynię poza ekranem.

Źródło: RESEARCH §1, §5.5. Szanse 70/25/5: `(decyzja autora)`.

**W16. Sloty, poziomy i zawartość umiejętności.** F1 / F2
- **6 slotów aktywnych** (slot 1 zajmuje umiejętność broni z ekwipunku) i **6 slotów pasywnych**. Poziomy 1–5 (gwiazdki), potem ewolucja. Ekran pauzy pokazuje posiadane umiejętności z poziomami.
- Gdy wszystkie 12 slotów jest pełnych i wymaksowanych (z ewolucjami), awans i skrzynie dają złoto.
- **Zawartość: wzorce z RESEARCH §5.1, z własnymi nazwami i wyglądem, z tymi samymi mnożnikami ×ATK i liczbą obiektów na poziomach jako wartości startowe:**

| # | Wzorzec | Faza |
|---|---|---|
| A1 | pocisk wracający | F1 |
| A2 | rzut łukiem, liczba rośnie 1→5 | F1 |
| A3 | pocisk rykoszetujący od krawędzi ekranu | F1 |
| A4 | toczący się kolczasty obiekt | F2 |
| A5 | aura obrażeń wokół gracza | F1 |
| A6 | orbitujące obiekty blokujące pociski | F1 |
| A7 | promień | F2 |
| A8 | piorun w losowe cele | F1 |
| A9 | miny | F2 |
| A10 | podpalenie obszaru | F1 |
| A11 | rakieta z eksplozją obszarową | F1 |
| A12 | odbijająca się piłka | F2 |
| A13 | dron rakietowy A | F2 |
| A14 | dron rakietowy B | F2 |

- **12 pasywnych** (wartości Lv1→Lv5 z RESEARCH §1): atak +10→50%, czas odnowienia −8→40%, maks. HP +20→100%, regeneracja 1→5% HP co 5 s, prędkość ruchu +10→50%, obszar/zasięg +10→50%, czas trwania +10→50%, prędkość pocisków +10→50%, zasięg zbierania +100→500%, XP +8→40%, złoto +8→40%, redukcja obrażeń −10→50%. Wszystkie 12 pasywnych jest w F1, bo każda jest parą jakiejś ewolucji F1. Wartości Lv2–4 rozkładają się liniowo między Lv1 i Lv5.

Wartości oznaczone w RESEARCH §5.1 jako „rozmiar+”, „obszar+”, „rakiety+” oraz brakujące dane (liczba obiektów dronów, bronie B2–B5) ustala autor w danych. `(decyzja autora)`
Źródło: RESEARCH §5.1. Podział na fazy: `(decyzja autora)`.

**W17. Ewolucje.** F1 / F2
- **Aktywna + pasywna:** aktywna na poziomie 5 + para posiadana na dowolnym poziomie → ewolucja dostępna w skrzyni i jako karta (W15). Ewolucja zastępuje aktywną w tym samym slocie, a pasywna zostaje. Ma własną nazwę, ikonę, znacznik „EVO” i inne lub mocniejsze zachowanie (wartości startowe z kolumny EVO w RESEARCH §5.1).
- **Aktywna + aktywna (F2):** obie na poziomie 5 → jedna ewolucja zajmuje slot pierwszej, slot drugiej się zwalnia. Pary: dron A + dron B, miny + podpalenie, miny + piorun. Ewolucja aktywna + aktywna jest dostępna w skrzyni i jako karta, tak jak pozostałe. Gdy możliwe są dwie (miny + podpalenie i miny + piorun), obie są osobnymi kartami, a skrzynia wybiera pierwszą według kolejności w danych.
- **Pary aktywna + pasywna** (struktura z RESEARCH §5.2): wracający + zasięg zbierania; rzut łukiem + maks. HP; piłka + prędkość ruchu; aura + regeneracja; rykoszet + prędkość pocisków; toczący się + obszar; orbita + czas trwania; promień + czas odnowienia; piorun + czas odnowienia; podpalenie + złoto; rakieta + obszar.
- **Bronie** mają własne pary (W22).

Źródło: RESEARCH §5.2.

### D. Wrogowie i przedmioty na mapie

**W18. Przedmioty na mapie.** F1
Niszczalne obiekty pojawiają się losowo. Po zniszczeniu dają jeden przedmiot:
- **magnes:** ściąga wszystkie kryształy XP z mapy,
- **bomba:** zabija zwykłych wrogów na ekranie, elitom i bossom zadaje 10% maks. HP,
- **mięso:** leczy 30% maks. HP,
- **złoto:** dodaje złoto do wyniku runu.

Przedmiot dłużej niż 60 s poza ekranem znika. Kryształy dają tylko XP.
Częstość pojawiania się obiektów, szanse na przedmioty i kwota złota są w danych. `(decyzja autora)`
Źródło: RESEARCH §1. Wartości: `(decyzja autora)`.

**W19. Daleko odbiegający wrogowie.** F1
Na mapie otwartej i korytarzu wróg dalej od gracza niż promień z danych zostaje przeniesiony tuż za krawędź ekranu po stronie, w którą gracz się porusza (gdy gracz stoi, po losowej stronie). Na mapie zamkniętej przeniesienia nie ma.
Źródło: `(decyzja autora)`.

**W20. Typy wrogów.** F1 / F2
Wzorce zachowań:
1. wolny piechur, idzie do aktualnej pozycji gracza (nie wyprzedza jego ruchu),
2. szybki, słaby rój,
3. dystansowy: zatrzymuje się w zasięgu i strzela wolnymi pociskami,
4. tank: wolny, dużo HP,
5. szarżujący: zapowiada kierunek (co najmniej 0,7 s), szarżuje w linii prostej,
6. pozostawiający po śmierci plamę zadającą obrażenia (F2),
7. latający, ignoruje odpychanie innych wrogów (F2; w Survivor.io jest już w rozdziale 1, przesunięcie do F2 to świadome uproszczenie).

Elity to wzmocnione, większe wersje typów, upuszczające skrzynię. Wrogowie odpychają się nawzajem. Kontakt z wrogiem zadaje obrażenia jego typu. Każdy rozdział używa co najmniej 3 typów. F1 obejmuje typy 1–5, F2 dodaje 6–7.
Źródło: RESEARCH §1, §5.4.

**W21. Pociski wrogów są czytelne.** F1
Pociski wrogów różnią się od pocisków gracza kolorem i obrysem. Sprawdzenie: na nagraniu ze 100+ wrogami autor wskazuje każdy pocisk wroga na 5 losowych klatkach bez pomyłki.
Źródło: RESEARCH §1 (krytyka Naavik).

### E. Ekwipunek

**W22. Sloty, bronie i typy przedmiotów.** F1 / F2
- **6 slotów:** broń, naszyjnik, rękawice dają ATK. Zbroja, pas, buty dają HP.
- Każdy slot ma **typy przedmiotów**. Typ ma własną nazwę i własne bonusy rzadkości (W24). F1: 3 typy broni i 2 typy w każdym innym slocie. F2: 5 typów na slot.
- **Broń wyznacza umiejętność startową runu** (slot 1). Bez założonej broni gracz ma broń domyślną: pocisk w najbliższego wroga. Typy broni (wzorce):

| Broń (wzorzec) | Zachowanie | Para ewolucji | Faza |
|---|---|---|---|
| B1 rzucany pocisk | w najbliższego, liczba rośnie z poziomem | XP | F1 (domyślna) |
| B2 cięcie | łuk cięcia przed postacią, fala ostrza | redukcja obrażeń | F1 |
| B3 pistolet | szybkie strzały w najbliższego, przeładowanie | atak | F1 |
| B4 strzelba | wachlarz śrutu w krótkim zasięgu | atak | F2 |
| B5 pałka | obrotowe uderzenie wokół postaci, odrzut | maks. HP | F2 |

Źródło: RESEARCH §5.2, §5.3.

**W23. Drabina rzadkości i merge.** F1
- 15 stopni: zwykły → dobry → lepszy → doskonały → doskonały+1 → doskonały+2 → epicki → epicki+1 → +2 → +3 → legendarny → legendarny+1 → +2 → +3 → +4. Każdy z 6 stopni podstawowych ma własny kolor ramki, a podstopnie +N mają ten kolor i N znaczników.
- Zasady merge'a zgodne z tabelą RESEARCH §5.3:

| Wynik | Składniki |
|---|---|
| dobry / lepszy / doskonały | 3 identyczne (ten sam typ, ten sam stopień) |
| doskonały+1 | doskonały + 1 dowolny doskonały tego slotu |
| doskonały+2 | doskonały+1 + 2 dowolne doskonałe tego slotu |
| epicki | doskonały+2 + identyczny doskonały+2 |
| epicki+1 / +2 / +3 | poprzedni + 1 dowolny epicki tego slotu |
| legendarny | epicki+3 + 2 identyczne epickie |
| legendarny+1…+4 | poprzedni + 1 dowolny legendarny tego slotu |

- „Identyczny” = ten sam typ przedmiotu co przedmiot główny, na stopniu podanym w tabeli (w przepisie na legendarny: 2 zwykłe epickie tego typu). „Dowolny” = dowolny typ tego samego slotu na stopniu podstawowym (bez +N). Założony przedmiot może być tylko przedmiotem głównym. Wynik ma typ przedmiotu głównego. Ekran merge'a: gracz wybiera przedmiot główny, gra podświetla pasujące składniki, a wynik jest widoczny przed potwierdzeniem.
- Wynik zachowuje poziom ulepszenia przedmiotu głównego. Złoto i plany wydane na ulepszenie składników (bez przedmiotu głównego) wracają do gracza. Jeśli przedmiot główny był założony, wynik zostaje założony.
- „Połącz wszystko” łączy automatycznie tylko stopnie zwykły → doskonały (3 identyczne, przedmiotem głównym jest ten z najwyższym poziomem ulepszenia), łańcuchowo, aż nie da się nic połączyć.
- Legendarny+4 to maksimum w F1–F2.

Źródło: RESEARCH §5.3. Zachowanie poziomu i zakres „Połącz wszystko”: `(decyzja autora)`.

**W24. Statystyki i bonusy rzadkości.** F1
- Bazowa statystyka broni na stopniach: 10 / 20 / 30 / 50 / 60 / 70 / 132 / 154 / 176 / 198 / 231, a dalej legendarny+1…+4 każdy o +10% statystyki legendarnej więcej (liniowo). Ulepszenie o poziom dodaje stałą wartość statystyki z danych. Pozostałe sloty mają analogiczną skalę ATK albo HP (w danych).
- Bonusy odblokowywane przez stopień (szkielet dla każdego typu broni): dobry ATK +10%; lepszy obrażenia umiejętności broni +30%; doskonały start runu z umiejętnością broni na poziomie 2; epicki ATK +15%; legendarny zmiana zachowania umiejętności broni (np. pocisk dzieli się po trafieniu).
- Przedmioty innych slotów mają po 3 bonusy sytuacyjne odblokowywane na stopniach lepszy, doskonały i legendarny (np. „+5% prędkości po zjedzeniu mięsa, maks. 30%”, „+X% obrażeń bossom”). Lista jest w danych.
- Karta przedmiotu pokazuje wszystkie bonusy, zablokowane wyszarzone, z informacją, od którego stopnia działają.

Bonusy się sumują. Procentowe bonusy ATK i HP liczą się od sumy ATK albo HP z ekwipunku. Każdy typ broni ma w danych własne zachowanie legendarne. Bonusy przedmiotów innych slotów projektuje autor, po 3 na typ. `(decyzja autora)`
Źródło: RESEARCH §5.3. Skala innych slotów i lista bonusów: `(decyzja autora)`.

**W25. Ulepszanie przedmiotów.** F1
Ulepszenie o 1 poziom kosztuje złoto i plany slotu, a koszt rośnie z poziomem. Limit poziomu zależy od stopnia: zwykły 10, dobry 20, lepszy 30, doskonały 40 (+1: 45, +2: 50), epicki 60 (+1…+3: 65/70/75), legendarny 80 (+1…+4: 85/90/95/100). „Ulepsz maksymalnie” wydaje zasoby, ile się da. Reset zwraca 100% złota i planów.
Źródło: RESEARCH §5.3. Limity: `(decyzja autora)`.

### F. Postęp konta i tryby

**W26. Rozdziały.** F1 / F2
- Liniowe odblokowanie: rozdział N+1 po wygranej w rozdziale N. F1: 3 rozdziały (1 otwarty, 2 korytarz, 3 zamknięty). F2: 10 rozdziałów, z czego co najmniej 2 zamknięte i 2 korytarze.
- Każdy rozdział ma własny motyw, zestaw wrogów, oś czasu i bossów. Każdy rozdział ma mnożnik HP i obrażeń wrogów. Mnożnik rozdziału N+1 jest co najmniej 1,5× mnożnika rozdziału N (ten sam typ wroga ma w N+1 co najmniej 1,5× więcej HP i obrażeń).
- Ekran rozdziału pokazuje najlepszy czas: najkrótszy czas zwycięstwa, a przed pierwszym zwycięstwem najdłuższy czas przetrwania.

Źródło: RESEARCH §5.4. Kolejność formatów i mnożnik: `(decyzja autora)`.

**W27. Kamienie milowe rozdziału.** F2
Każdy rozdział ma 3 jednorazowe skrzynie za: przetrwanie 5 min (zamknięty: 3 min, `(decyzja autora)`), przetrwanie 10 min (zamknięty: 6 min, `(decyzja autora)`) i ukończenie. Skrzynie odbiera się na ekranie rozdziału. Zawierają złoto, klejnoty, plany i klucze (tabela w danych).
Źródło: RESEARCH §5.4.

**W28. Nagrody za run.** F1
- Porażka: złoto i XP konta proporcjonalne do czasu przetrwania (czas / pełny czas mapy × nagroda za wygraną), plany z szansą, bez przedmiotów.
- Wygrana: pełne złoto i XP konta (rozdział 1: 1000 XP, każdy następny więcej), 1–3 przedmioty losowych slotów (stopień z tabeli rozdziału) i plany.
- Ekran końca runu: wynik, czas, zabici, złoto, XP konta, przedmioty i plany. Przyciski „Dalej” i „Zagraj ponownie”.

Kwoty złota, planów i tabela stopni przedmiotów na rozdział są w danych. XP konta za wygraną: 1000 w rozdziale 1, +150 za każdy następny. `(decyzja autora)`
Źródło: RESEARCH §1, §5.4. Formuła: `(decyzja autora)`.

**W29. Poziom konta.** F1
Run (a od F2 także Patrol) daje XP konta. Krzywa XP konta jest w danych. Maks. poziom konta w F1 to 10. Awans daje złoto, a od F2 także klejnoty i +5 energii (w F1 klejnotów nie ma, bo nie ma na co ich wydać). Poziom konta jest limitem poziomu każdego talentu (W30). `(decyzja autora)`
Źródło: RESEARCH §5.6.

**W30. Talenty.** F1 / F2
- **Talenty statystyk (F1):** atak, HP, obrona (procentowa redukcja obrażeń), regeneracja po zjedzeniu mięsa, zdobywane złoto, zasięg zbierania. Kupowane za złoto. Poziom talentu nie może przekroczyć poziomu konta. Koszt i przyrost na poziom są w danych. Talent mięsa zwiększa leczenie z mięsa o 5% na poziom. Bonusy talentów i pasywnych w runie mnożą się.
- **Talenty specjalne (F2):** kupowane za DNA z Prób (W31), koszt 1/2/3 DNA w kolejności poniżej: odświeżenie kart 1× na run (W15), start runu z +1 poziomem umiejętności broni, darmowe wskrzeszenie (W33: zamiast klejnotów, nadal maks. 1 na run).

Źródło: RESEARCH §5.5, §5.6. Lista talentów: `(decyzja autora)`.

**W31. Próby.** F2
Odblokowanie po rozdziale 2. Każdy ukończony rozdział ma 3 Próby, rozgrywane kolejno, bez energii i bez limitu podejść:
- modyfikatory: (M1) wrogowie +50% HP i +20% prędkości, (M2) połowa XP z kryształów, (M3) połowa przedmiotów z obiektów, (M4) podwójni bossowie, (M5) widoczność ograniczona do koła wokół gracza,
- Próba 1: M1, Próba 2: M1–M3, Próba 3: M1–M5.

Próby są dostępne dla każdego ukończonego rozdziału, także 1. Kolejne przejście tej samej Próby daje złoto i plany jak wygrana rozdziału, bez klejnotów i DNA.

Nagrody za pierwsze przejście: 200 klejnotów / 1 DNA / 200 klejnotów + 1 DNA. Lista modyfikatorów jest w danych. Lista M1–M5: `(decyzja autora)`.
Źródło: RESEARCH §5.6.

**W32. Patrol (nagrody idle).** F2
Odblokowanie po rozdziale 1. Patrol nalicza złoto, XP konta, plany i (rzadko) przedmioty co 10 min, do 24 h. Stawka na godzinę: złoto = 3000 + 300 × ukończone rozdziały, XP = 1200 + 120 × ukończone rozdziały. Odbiór pokazuje zebrane nagrody. Szybki patrol: 15 energii za natychmiastowe 5 h nagród, 3× dziennie.
Szanse na plany i przedmioty z Patrolu są w danych. `(decyzja autora)`
Źródło: RESEARCH §5.6.

**W33. Wskrzeszenie.** F2
Przy HP = 0 gra pyta o wskrzeszenie (licznik 5 s). Koszt: 100 klejnotów albo darmowe z talentu (W30). Maksymalnie 1 wskrzeszenie na run. Wskrzeszenie przywraca 50% HP i działa jak bomba z W18 (zabija zwykłych wrogów na ekranie, elitom i bossom zadaje 10% maks. HP). Odmowa → porażka.
Źródło: RESEARCH §5.5. Wartości: `(decyzja autora)`.

**W34. Energia.** F2
Run rozdziału kosztuje 5 energii, pobieranej przy starcie. Limit 30. Odnawianie 1 punkt na 12 min do limitu. Energia może przekroczyć limit z nagród. Próby nie kosztują energii. Bez energii przycisk „Graj” pokazuje czas do odnowienia i opcje doładowania: klejnoty (100 za 15, 3× dziennie). Reklama za energię dopiero w F3.
Źródło: RESEARCH §2, §5.6, P18. Odnawianie: `(decyzja autora)`. Przełącznik w danych wyłącza energię (run za darmo) na czas testów.

**W35. Misje dzienne i tygodniowe.** F2
- Dzienne: zadania po 10–20 pkt (np. zaloguj się, zagraj rozdział, ulepsz przedmiot, kup w sklepie, zabij N wrogów). Skrzynie przy 20, 40, 60, 80 i 100 pkt. Reset codziennie o 04:00 czasu lokalnego. `(decyzja autora)`
- Tygodniowe: 5 zadań po 20 pkt (np. pokonaj 50 bossów, przejdź 3 Próby), skrzynie przy 20/40/60/80/100 pkt, reset w poniedziałek o 04:00. Lista zadań i nagrody są w danych. `(decyzja autora)`

Źródło: RESEARCH §5.6.

**W36. Sklep i skrzynie.** F2
- Waluty: złoto, klejnoty, klucze (2 rodzaje), plany (po jednym rodzaju na slot), DNA. Klejnoty w F2 pochodzą tylko z gry.
- Sklep dzienny: 6 losowych ofert (przedmioty za klejnoty, plany za złoto), odświeżanie raz dziennie o 04:00 (w Survivor.io kilka razy dziennie, uproszczenie).
- Darmowa skrzynia: 1× na 8 h.
- Skrzynia podstawowa (zwykłe/dobre): klucz podstawowy albo 80 klejnotów.
- Skrzynia lepsza (dobre–doskonałe): klucz lepszy albo 300 klejnotów, 10 otwarć za 2680 (cena skrzyni S z Survivor.io użyta dla skrzyni lepszej). Gwarancja doskonałego co 10 otwarć, licznik widoczny, zeruje się po gwarancji albo po wylosowaniu doskonałego.
- Klucze pochodzą z kamieni milowych rozdziałów (W27) i skrzyń misji (W35). Szanse i ceny są w danych.
- Ekran każdej skrzyni pokazuje szanse na stopnie.

Źródło: RESEARCH §5.6. Wartości: `(decyzja autora)`.

**W37. Kolejność odblokowań.** F1 / F2
Pierwsze uruchomienie: krótki ekran startowy, potem od razu run rozdziału 1 z podpowiedziami (ruch, kryształ, karta). Ekwipunek i talenty po pierwszym runie. Patrol po rozdziale 1. Próby po rozdziale 2. Sklep i misje po rozdziale 1. Zablokowana funkcja ma kłódkę i podpis, co ją odblokuje. Funkcji z F2 w buildzie F1 nie ma na ekranie wcale. Flaga samouczka zapisuje się po pierwszym zakończonym runie (wygrana albo porażka).
Źródło: RESEARCH §1, §5.6.

### G. Ekran główny

**W38. Ekran główny.** F1
Postać w ekwipunku, rozdział z przyciskiem „Graj”, przełączanie rozdziałów (zablokowane z kłódką), waluty u góry. Dolne menu: Sklep (F2), Ekwipunek, Graj, Talenty, Próby (F2). Patrol i misje jako ikony na ekranie głównym (F2).
Źródło: RESEARCH §2.

### H. Jakość, dane, platformy

**W39. Oprawa i czytelność.** F1
Trafiony wróg błyska około 0,1 s. Nad trafieniem pojawia się liczba obrażeń (domyślnie włączona, do wyłączenia). Zabity wróg ma krótką animację śmierci. Każda umiejętność i ewolucja ma własny dźwięk. Opcjonalne wibracje przy trafieniu gracza.
Źródło: RESEARCH §1.

**W40. Oryginalna tożsamość.** F1
Nazwa gry, świat, postaci, wrogowie, bossowie, umiejętności, przedmioty, rozdziały, grafika, UI, ikony, muzyka, dźwięki i teksty są własne lub z licencjonowanych paczek z prawem do użytku komercyjnego. Żaden z tych elementów nie pochodzi z Survivor.io ani nie jest do niego łudząco podobny. Motyw świata (P19): sci-fi, gracz steruje dronem/mechem na obcej planecie, umiejętności to moduły, wrogowie to obca fauna i maszyny, rozdziały to strefy planety. Sprawdzenie: przed każdym buildem testowym autor przechodzi listę: nazwa i ikona, nazwy w tekstach, grafika postaci, wrogów i bossów, układ UI ekranu głównego i karty, dźwięki i muzyka. Żaden element nie pochodzi z Survivor.io ani go nie przypomina.
Źródło: RESEARCH §4 (Tetris v. Xio, Apple 4.1/4.3).

**W41. Balans w danych.** F1
Wszystkie wartości do strojenia z tego dokumentu (umiejętności, ewolucje, wrogowie, osie czasu, krzywa XP, skrzynie, merge, limity, koszty, nagrody, energia, Patrol, Próby, progi czasowe) są zebrane w dwóch plikach: liczby w `core/Balance.kt`, treść (umiejętności, przedmioty, wrogowie, bossowie, rozdziały) w `core/Content.kt`, jak w JumperIdle. Zmiana liczby nie wymaga zmiany logiki gry, tylko tych plików.
Liczby strukturalne (6+6 slotów, 3 karty, 15 stopni, 5 poziomów umiejętności) mogą być w kodzie.
Źródło: `(decyzja autora)`; forma plików zmieniona 2026-11-16 razem ze stosem (użytkownik: „ten sam silnik i wszystko” co JumperIdle).

**W42. Zapis lokalny.** F1
Postęp zapisuje się lokalnie po każdej zmianie i przetrwa zabicie aplikacji, restart i aktualizację. Przerwany zapis nie niszczy postępu: zostaje poprzedni poprawny. Format zapisu ma wersję i nowa aplikacja wczytuje starsze. Przejście w tło w runie pauzuje grę, a powrót pokazuje pauzę. Zabicie aplikacji w trakcie runu = run stracony bez nagrody, a energia nie wraca. Energia i Patrol liczą czas według zegara urządzenia (P23).
Źródło: P10, RESEARCH §2.

**W43. Wydajność.** F1
Scenariusz testowy (dostępny na żądanie, pokazuje FPS) przez 60 s: 300 żywych wrogów, 200 pocisków, 300 kryształów, 6 umiejętności aktywnych na poziomie 5. Telefon średniej klasy (Pixel 6a, Galaxy A54, iPhone 11): średnio ≥ 58 FPS, a 1% najwolniejszych klatek ≥ 45 FPS. Słabszy (Galaxy A14, iPhone XR): średnio ≥ 30 FPS. Mierzone na telefonach, które ma autor, z liczbą klatek ograniczoną do 60. W F1 tylko Android, iPhone w F2. Scenariusz uruchamia się z menu deweloperskiego w buildzie deweloperskim.
Źródło: RESEARCH §4, P11.

**W44. Ustawienia.** F1
Głośność muzyki i efektów, wibracje, liczby obrażeń, język polski/angielski (domyślnie język systemu, gdy PL lub EN, inaczej EN). Zmiana języka działa od razu. Ustawienia dostępne z ekranu głównego i z pauzy. Wszystkie teksty z plików tłumaczeń.
Ekran pauzy: lista umiejętności z poziomami, Wznów, Ustawienia, Wyjdź (porażka z częściowymi nagrodami, W13).
Źródło: P12.

**W45. Platformy i sieć.** F1 / F2
Kotlin Multiplatform + Compose Multiplatform, jedna baza kodu w module `shared` (jak JumperIdle). F1: Android 8+ (API 26), APK z GitHub Actions. F2: iOS 15+, IPA budowane w GitHub Actions na macOS (bez własnego Maca). Aplikacja w F1–F2 nie łączy się z siecią i nie ma SDK reklam, analityki ani raportów awarii (logowanie Google i czas z serwera z JumperIdle nie są w zakresie F1–F2).
Źródło: P2, P11, P16; stos zmieniony 2026-11-16 na polecenie użytkownika.

---

## Definicja ukończenia fazy

- Wymaganie oznaczone „F1 / F2” jest spełnione w F1, gdy działa jego część F1.
- **F1:** każde wymaganie oznaczone F1 sprawdzone testem lub ręcznie na telefonie z Androidem (W43 według progów). Balans, sprawdzany przez autora na nowym zapisie: rozdział 1 wygrany w co najwyżej 3 próbach z bronią domyślną; rozdziału 3 nie da się wygrać w 3 próbach bez ulepszeń ekwipunku lub talentów. `(decyzja autora)`
- **F2:** każde wymaganie F1 i F2 sprawdzone na Androidzie i iPhonie. Balans: rozdział 10 wymaga przedmiotów co najmniej doskonałych i około tygodnia gry dziennej po 3–5 runów (szacunek autora, nie kryterium akceptacji). `(decyzja autora)`

## Stan obecny

Projektu nie ma: katalog zawiera tylko `RESEARCH.md` i ten plik.

## Kontrakty i dane

Brak backendu i sieci w F1–F2. Dane: pliki balansu (W41), lokalny zapis (W42).

## Poza zakresem (F1–F2)

- reklamy, zakupy, battle pass, karty miesięczne, skarbonka, oferty (F3, P22),
- zwierzaki, części technologiczne, grywalne postaci, kolekcje, Astral Forge, stopnie S/SS/Relic (systemy dodane po premierze),
- konto online, zapis w chmurze, rankingi, klany, PvP, tryby eventowe, tryb bossa z rankingiem,
- powiadomienia push, osiągnięcia,
- ewolucja ewolucji (dron + dron + dron medyczny),
- tablet, tryb poziomy, kontroler.

---

## Pytania

Wersja 1 miała P1–P16 z odpowiedziami z 2026-11-16. Odpowiedzi nadal obowiązują, z wyjątkiem pytań unieważnionych przez wersję 2:
- P4 (10 min) → zastąpione: 15/8 min jak w Survivor.io (W6–W8),
- P5 (3 rozdziały) → zastąpione fazami (P17),
- P6 (8/8/8) → zastąpione zawartością W16,
- P7 (4 rzadkości) → zastąpione drabiną 15 stopni (W23),
- P8 (bez wskrzeszenia) → zastąpione W33,
- P9 (bez energii) → zastąpione P18,
- P13 (motyw sci-fi) → ponownie otwarte jako P19.

Obowiązują dalej (odpowiedzi z 2026-11-16, użytkownik):
- P1/P2: silnik Unity 6 (C#), zastąpione 2026-11-16: stos jak w JumperIdle (Kotlin Multiplatform + Compose Multiplatform), W45,
- P3: tylko pion,
- P10: zabicie aplikacji w trakcie runu = run stracony,
- P11: urządzenia średnie Pixel 6a / Galaxy A54 / iPhone 11, słabsze Galaxy A14 / iPhone XR, minimum Android 8 (API 26) i iOS 15,
- P12: polski i angielski od początku, teksty z plików tłumaczeń,
- P14: grafika z kupionych paczek 2D w jednym stylu (pixel-art albo flat), wymiana później,
- P15: najpierw hobby z celem „grywalna gra na własnym telefonie”, decyzja o wydaniu później,
- P16: rozwój i testy najpierw na Androidzie, iOS z Maca na końcu, darmowe Apple ID do instalacji testowej.

Nowe pytania, każde z rekomendacją. „tak” ją przyjmuje.

**P17. Fazy.** Podział F1 (run + ekwipunek z merge'em + talenty + 3 rozdziały, Android) → F2 (pełna meta z premiery, iOS) → F3 (monetyzacja i systemy późniejsze)?
Rekomendacja: tak. Pełna meta przed grywalnym runem to ryzyko, że rdzeń nie będzie dobrze działał.
Odpowiedź: tak (2026-11-16, użytkownik).

**P18. Energia.** Energia jak w Survivor.io (5 za run, limit 30, Próby za darmo) od F2, odnawianie 1 na 12 min, bez reklam do F3?
Rekomendacja: tak. To kluczowy element rytmu sesji Survivor.io. Do F3 możemy ją wyłączyć jednym przełącznikiem w danych, gdyby przeszkadzała w testach.
Odpowiedź: tak (2026-11-16, użytkownik).

**P19. Motyw świata.** Survivor.io ma postapokalipsę z zombie w mieście. Sam motyw zombie jest gatunkowy (wiele gier go używa), więc możemy go wziąć, z własną grafiką i postaciami. Im bliżej wygląd Survivor.io, tym wyższe ryzyko odrzucenia w App Store (4.1/4.3) i sporu z Habby. Zostajemy przy sci-fi (dron/mech na obcej planecie)?
Rekomendacja: tak, sci-fi. Mechanika zostaje 1:1, a gra wygląda jak coś innego. Jeśli wolisz zombie, odpowiedz „zombie”. Wtedy wymaganie W40 dostanie dodatkowe kryterium: inny styl graficzny niż Survivor.io (np. pixel-art zamiast jego kreskówkowego stylu), inne miejsca niż miasto/park/parking i własne typy potworów.
Odpowiedź: tak, sci-fi: dron/mech na obcej planecie (2026-11-16, użytkownik).

**P20. Zawartość F2.** 14 wzorców aktywnych, 12 pasywnych, 5 broni, 5 typów przedmiotów na slot, 10 rozdziałów (W16, W22, W26)?
Rekomendacja: tak. To odpowiada zawartości Survivor.io z pierwszych miesięcy.
Odpowiedź: tak (2026-11-16, użytkownik).

**P21. Liczby Survivor.io jako start.** Mnożniki umiejętności, tabela merge'a, statystyki broni, Patrol i Próby biorę 1:1 z researchu jako wartości startowe, a potem stroimy?
Rekomendacja: tak. Liczby to reguły gry, nie twórczość. Część jest i tak niepewna (dane fanów).
Odpowiedź: tak (2026-11-16, użytkownik).

**P22. Monetyzacja F3.** Decyzję o reklamach i zakupach podejmujemy po F2, osobną specyfikacją?
Rekomendacja: tak.
Odpowiedź: tak (2026-11-16, użytkownik).

**P23. Zegar.** Energia, Patrol i resety dzienne liczą według zegara urządzenia, bez ochrony przed cofaniem czasu (gra jest offline i bez zakupów)?
Rekomendacja: tak. Ochrona ma sens dopiero przy monetyzacji (F3).
Odpowiedź: tak (2026-11-16, użytkownik).

---

## Ryzyka

- **Liczby z researchu pochodzą od fanów** (wiki, poradniki 2022–2024) i bywają sprzeczne. Statystyki bazowe, krzywa XP, szanse w skrzyniach i odnawianie energii nie są nigdzie publikowane, więc to `(decyzja autora)`.
- **Zakres F2 jest duży** (14 umiejętności, 5 broni, 30 typów przedmiotów, 10 rozdziałów, 6 systemów meta). Dla jednej osoby po godzinach to miesiące pracy. F1 jest ustawione tak, żeby grywalna gra była możliwie wcześnie.
- **Podobieństwo:** im bliżej Survivor.io w wyglądzie i nazewnictwie, tym większe ryzyko prawne i sklepowe (W40, P19). Mechaniki są bezpieczne.
- **Wydajność (W43)** to największe ryzyko techniczne. Pierwszy prototyp powinien ją sprawdzić.
- **Energia (W34) w grze offline** łatwo obejść zmianą zegara (P23).
- Test czytelnika wersji 2: 28 punktów. Już znane (odwołania, brakujące odpowiedzi z wersji 1): 4. Drobne, wpisane jako `(decyzja autora)` i zatwierdzone przez użytkownika (2026-11-16): 24. Do rozstrzygnięcia przez użytkownika: 0.
- **Oś czasu bez kopiowania:** W6 nie ma już konkretnej osi czasu rozdziału 1 Survivor.io, tylko reguły formatu. Konkretne osie czasu rozdziałów projektuje autor.
