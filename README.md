# SurvivalFriend v0.7.1-BETA

Plugin für Spigot, Paper und Folia, einschließlich Minecraft 26.2. Der Build
verwendet die Spigot-API 26.2 und benötigt JDK 25. Die Plugin-Klassen bleiben
Java-17-kompatibel; Minecraft 26.2 benötigt zum Ausführen Java 25.
Siehe die [Java-Anforderungen von Spigot](https://www.spigotmc.org/?id=33).

## Folia-Unterstützung

Dieselbe JAR kann auf allen drei Serverplattformen verwendet werden. Auf Folia
und Paper mit Region-Scheduler-API laufen verzögerte Aufgaben auf dem Scheduler
des betroffenen Spielers oder Drachen. Auf Spigot und älteren Paper-Versionen
wird weiterhin der Bukkit-Hauptthread verwendet. Es ist keine zusätzliche
Bibliothek und keine Änderung der Konfiguration erforderlich.

- TPA nutzt auf Folia und Paper mit Region-Scheduler-API `teleportAsync`.
  Zielposition und Teleport werden auf den jeweiligen Spieler-Threads verarbeitet.
  Eine Anfrage kann während eines laufenden Teleports nur einmal angenommen
  werden; fehlgeschlagene oder abgebrochene Teleports erlauben eine erneute Annahme
  innerhalb der Gültigkeitsdauer. Disconnects entwerten wartende Anfragen.
- Spielerziele des Kompasses werden auf dem Thread des Zielspielers gelesen und
  auf dem Thread des Kompassbesitzers gesetzt. Der Zielort ist eine Momentaufnahme.
- Jeder Admin bekommt ein eigenes Menü. Änderungen an der Konfiguration und
  gespeicherte Regelzustimmungen werden serialisiert; TPA- und Zustimmungsdaten
  sind gegen parallele Zugriffe aus verschiedenen Regionen geschützt.
- Regelmenüs und Drachen-Aufgaben folgen ihrer Entity beim Regionswechsel.
  Ausstehende Aufgaben verändern nach dem Deaktivieren des Plugins keinen Zustand.
- Bukkit-Konstanten, das verschobene Mount-Event und die Änderung von
  `InventoryView` von einer Klasse zu einem Interface werden zur Laufzeit
  aufgelöst, damit dieselbe JAR auch die ältere 1.19.4-API verwenden kann.
- Totemmeldungen werden pro Empfänger auf dessen Spieler-Thread zugestellt.
  Ist ein Verursacher in einer anderen Region, wird dessen Inventar nicht gelesen.

Die Umsetzung folgt der [Paper/Folia-Dokumentation](https://docs.papermc.io/paper/dev/folia-support/).
Alle 43 automatisierten Tests bestehen mit der Spigot-API 26.2 und 1.19.4; auch
die gegen 26.2 kompilierten Plugin-Klassen wurden mit der 1.19.4-API getestet.
Die Tests prüfen den Spigot-Fallback, die optionalen Scheduler-APIs,
simulierte getrennte Spieler-Threads und parallele Datenzugriffe. Für die ältere
API lässt sich derselbe Testlauf mit
`mvn clean verify -Dspigot-api.version=1.19.4-R0.1-SNAPSHOT` ausführen. Ein Live-Test
auf einem echten Folia-, Paper- oder Spigot-Server wurde für diese Version noch
nicht durchgeführt. Vor dem produktiven Einsatz sollten insbesondere zwei
Spieler in weit getrennten Regionen bzw. Welten TPA und Kompass sowie Join,
Regelzustimmung, Adminmenü, Totemmeldungen und Drachen testen.

## Installation und Regeln

`mvn clean verify` erstellt `target/survivalfriend-0.7.1-BETA.jar`. Die JAR in den
`plugins`-Ordner kopieren und den Server starten. Bestehende Einstellungen bleiben
erhalten; neue Standardwerte werden in `plugins/SurvivalFriend/config.yml` ergänzt.

Die Regeln als YAML-Liste eintragen:

```yaml
regeln:
  - 'Behandle andere Spieler respektvoll.'
  - 'Griefing und Diebstahl sind verboten.'
  - '&cCheats sind verboten.'
```

Nach Änderungen den Server neu starten. `/regeln` öffnet ein eigenes Chestmenü
mit einem Item pro Regel und bis zu 45 Regeln pro Seite. Spieler bestätigen jede
Regel per Linksklick und schließen mit **Allen Regeln zustimmen** ab. Bis dahin
sind Bewegung, Teleports, Portale und das Aufsteigen auf Fahrzeuge gesperrt.
Das Menü öffnet sich beim Beitreten automatisch und bleibt bis zur Zustimmung
verfügbar, auch wenn es geschlossen wird. Das gilt auch für Admins.

Die endgültige Zustimmung wird anhand der UUID und des Regelstands in
`accepted-rules.yml` gespeichert. Nach einer Änderung der Regeln ist erneut eine
Zustimmung nötig. Teilweise bestätigte Regeln werden beim Verlassen zurückgesetzt.
Mit `regeln: []` lässt sich die Bewegungssperre deaktivieren.

## Weitere Korrekturen

- Adminmenü: Eigene Inventarslots können keine Einstellungen mehr ändern;
  Adminrechte werden bei jedem Klick geprüft. Der globale Schalter funktioniert
  auch für Befehle und zeigt den tatsächlichen Zustand an.
- TPA: Sichere Argumentprüfung, keine Offline- oder Selbstanfragen, Ablauf nach
  120 Sekunden, Aufräumen bei Disconnects und korrekte Behandlung abgebrochener
  Teleports. `tpa.cooldown-seconds` bleibt standardmäßig bei 3600 Sekunden;
  `tpa.request-timeout-seconds` steuert die Gültigkeit einer Anfrage.
- Kompass: Benannte Kompasse und Stapel werden akzeptiert, `reset` und relative
  Koordinaten (`/compass ~10 ~-5`) funktionieren. Ziele in anderen Welten werden
  abgewiesen und ungültige Koordinaten abgefangen.
- Verstärkter Drache: Beachtet den Schalter und die konfigurierten Lebenspunkte,
  verarbeitet Schaden nur einmal und verwaltet Widerstand pro Drache.
- Totemmeldungen: Keine Meldung bei abgebrochener Wiederbelebung; fehlende
  Schadensursachen, leere Hände und Projektilschützen werden korrekt behandelt.
- Creeper: Blockschutz und Spielerschadenschutz sind unabhängig; auch hängende
  Dekorationen werden geschützt. Keine doppelten Explosionseffekte.

Die automatisierten Tests prüfen die Zustimmungs- und TPA-Logik sowie simulierte
Bukkit-Ereignisse. Ein zusätzlicher Funktionstest auf dem Minecraft-Server ist
für das Zusammenspiel mit anderen Plugins sinnvoll.
