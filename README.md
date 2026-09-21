# 🏋️ Abnehm-App

Eine persönliche Abnehm- & Fitness-Tracking-App als **einzelne HTML-Datei**. Läuft komplett offline im Handy-Browser, ohne Installation, ohne Server, ohne Framework. Alle Daten werden **lokal auf dem Gerät** (localStorage) gespeichert.

> **Wichtig:** Die gesamte App steckt in der Datei [`abnehm-app.html`](abnehm-app.html). Es gibt keinen Build-Schritt und keine Abhängigkeiten. „Neu aufbauen" = diese eine Datei ins Repo legen und öffnen.

---

## 🎯 Ziel des Projekts

Persönliches Ziel des Nutzers: **von 112 kg auf 105 kg in 2 Monaten** (perspektivisch weiter senkbar).
Rahmen: 31 Jahre, 180 cm, Bürojob (viel Sitzen), 1× Laufen/Woche, geplant 2× Krafttraining/Woche.

Strategie hinter der App: moderates Kaloriendefizit (~2.100 kcal/Tag, ~150–170 g Eiweiß), 2× Ganzkörper-Krafttraining, 1× Laufen, viel Alltagsbewegung (8.000+ Schritte).

---

## 📱 Nutzung auf dem Handy

1. Datei über GitHub Pages als URL öffnen (siehe **Deployment**) **oder** `abnehm-app.html` direkt im Handy-Browser öffnen.
2. Im Browser-Menü **„Zum Startbildschirm hinzufügen"** wählen → App-Icon.
3. Ab dann **immer über das Icon** öffnen (die Daten hängen an dieser Instanz).
4. **iPhone:** Safari verwenden (nur Safari kann Web-Apps zum Home-Bildschirm hinzufügen).

---

## 🚀 Deployment über GitHub Pages (URL fürs Handy)

1. `abnehm-app.html` (und diese README) ins Repo hochladen.
2. Repo muss **öffentlich** sein (oder GitHub Pro für private Pages).
3. **Settings → Pages → Branch: `main` → Save**.
4. Nach ~1 Min. verfügbar unter:
   `https://<DEIN-GITHUB-NAME>.github.io/<REPO-NAME>/abnehm-app.html`
5. Diese URL am Handy öffnen → zum Startbildschirm hinzufügen.

> Persönliche Tracking-Daten stehen **nicht** im Code (sie liegen nur lokal im Browser), ein öffentliches Repo ist also unbedenklich.

---

## 🧩 Aufbau: 5 Reiter (Bottom-Navigation)

### 1. 📊 Übersicht (Dashboard)
Aggregierte Statistiken über alle Daten:
- **Gewicht:** aktuell, Fortschrittsbalken Start→Ziel, abgenommen, bis Ziel, Δ diese Woche, Verlaufsdiagramm.
- **Prognose:** Ø Gewichtsänderung/Woche, voraussichtliches Zieldatum (Hochrechnung).
- **Diese Woche (7 Tage):** Krafttrainings, Läufe, Ø kcal/Tag, Ø Eiweiß, Ø Schritte, Ø Wasser.
- **Konsistenz:** Tage-Streak, Tage getrackt, wie oft Eiweißziel erreicht.

### 2. ✅ Heute
Datumsbasiert (Kalender + Pfeile, keine Zukunft). Für den gewählten Tag:
- **Fortschrittsring** (% der Tagesziele) + **Kalorien-Ist-Balken** (abgehakte Mahlzeiten + eigene Lebensmittel).
- **Ernährung (Plan):** tagesabhängige Mahlzeiten-Checkliste.
- **Eigene Lebensmittel:** Name + kcal + (optional) Eiweiß hinzufügen; Einträge **bearbeiten** (✏️) oder löschen.
- **Training & Bewegung:** aus dem Wochenplan; Eiweiß-Haken **automatisch** (setzt/entfernt sich bei ≥/< 150 g).
- **Wasser** (8 Gläser) und **Schritte** (Ziel 8.000).

### 3. ⚖️ Gewicht
Datumsbasiert (Kalender). Gewicht pro Tag eintragen/**ändern** (vorbefüllt bei vorhandenem Eintrag), Ziel anpassen (Start-/Zielgewicht variabel), Fortschritt, Verlaufsdiagramm mit Ziellinie, Einträge-Liste (anklickbar zum Bearbeiten, löschbar).

### 4. 🍽️ Essen
Datumsbasiert. Zeigt für den gewählten Tag den **Plan (Soll)** mit exakten Gramm-Angaben je Zutat + **Plan-Tagessumme** (kcal/Eiweiß) und die **Ist-Summe** (tatsächlich abgehakt + eigene Lebensmittel).

### 5. 🏋️ Gym
Datumsbasiert. Ganzkörper-Workout (7 Übungen), Gewichte **pro Tag** eintragen (automatisch gespeichert), „zuletzt"-Hinweis für Progression, Fortschritt je Übung, Workout-Historie (anklickbar), **anpassbare Wochenstruktur** (pro Tag Gym/Laufen/Ruhe).

---

## 💾 Daten & Backup

- Speicherung: **localStorage** (bleibt dauerhaft lokal auf dem Gerät, nicht im flüchtigen Cache). `navigator.storage.persist()` schützt zusätzlich.
- **Backup:** Im Gym-Tab „Komplett-Backup speichern" → exportiert **alle** Daten als `.json`; Wiederherstellen per Import.
- Daten sind **pro Gerät/Browser** — nicht synchronisiert. Backup nutzen bei Gerätewechsel.

### localStorage-Schlüssel (Datenmodell)
| Schlüssel | Inhalt |
|---|---|
| `ab_checks` | `{ "YYYY-MM-DD": { meal0:true, train0:true, habit_protein:true } }` |
| `ab_water` | `{ "YYYY-MM-DD": AnzahlGläser }` |
| `ab_steps` | `{ "YYYY-MM-DD": Schritte }` |
| `ab_weights` | `[ { date:"YYYY-MM-DD", weight:111.5 } ]` |
| `ab_gym_history` | `[ { date:"YYYY-MM-DD", weights:{ beinpresse:80, ... } } ]` |
| `ab_foodlog` | `{ "YYYY-MM-DD": [ { name, kcal, prot } ] }` |
| `ab_settings` | `{ start:112, goal:105 }` |
| `ab_dayplan` | `{ 0:"ruhe",1:"ruhe",2:"gym",3:"ruhe",4:"gym",5:"ruhe",6:"lauf" }` (0=So…6=Sa) |
| `ab_gym` | (veraltet, nur Abwärtskompatibilität) |

---

## 🛠️ Technik

- **Ein-Datei-App:** HTML + CSS + Vanilla-JavaScript in `abnehm-app.html`, keine externen Libraries (funktioniert offline).
- **Kern-Render-Funktionen:** `renderDashboard`, `renderHeute`, `renderGewicht`, `renderMealPlan`, `renderGym` — jeweils vom global gewählten Tag `viewDate` gesteuert.
- **Diagramm:** inline-SVG (`weightChartHTML`), ohne Chart-Library.
- **Datumsnavigation:** gemeinsamer `viewDate`, synchronisiert über `updateDateNav()`; native `<input type="date">` als Kalender.
- **Mahlzeiten/Übungen** sind als JS-Objekte oben in der Datei definiert (`MEALS`, `GYM_EXERCISES`) — dort anpassbar.

---

## 🔧 Anpassen

- **Andere Mahlzeiten/Gramm-Angaben:** `const MEALS = { ... }` in `abnehm-app.html` bearbeiten (Format: `{n:"Frühstück", d:"250 g Magerquark ...", k:"380 kcal · 35 g Eiweiß"}`). Kalorien-/Eiweiß-Summen werden automatisch aus dem `k`-Text geparst.
- **Andere Übungen:** `const GYM_EXERCISES = [ ... ]`.
- **Kalorien-/Eiweiß-Ziel:** Werte `2100` bzw. `PROTEIN_TARGET = 150` im Skript.

---

*Erstellt mit Claude Code.*
