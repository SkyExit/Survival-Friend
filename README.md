# SurvivalFriend v0.7-BETA

Spigot-Plugin für Minecraft 1.19.4, benötigt Java 17 oder neuer.

## Installation und Regeln

`mvn clean verify` erstellt `target/survivalfriend-0.7-BETA.jar`. Die JAR in den
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
