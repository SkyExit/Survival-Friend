# v0.8.6-BETA: disable Folia features after an engine switch

Verified on 16 October 2026 with JDK 25.0.3. Both offline Maven builds passed:

- Spigot 1.19.4: `mvn -o clean verify -Dspigot-api.version=1.19.4-R0.1-SNAPSHOT`
- Spigot 26.2: `mvn -o clean verify`

The release JAR declares `0.8.6-BETA` and targets Java 17 (class version 61).
Maven has no automated test sources; the separate native harness below is not
included in the release JAR.

## Native engine switch verification

A disposable server in the ignored `run/engine-switch-0.8.6/` directory binds
only to `127.0.0.1:25578`. The same plugin configuration, chamber data and worlds
were reused across three clean starts, changing only the server JAR and harness
stage. The existing test server was not modified.

| Stage | Engine | Result |
| --- | --- | --- |
| Initial start, both Folia features enabled | Folia 26.3 build 3 (`597e413`), two region threads | 29 passed |
| Switch to Paper with the saved enabled settings | Paper 26.3 build 190 (`a0ded75`) | 29 passed |
| Switch back with Paper's saved configuration | Folia 26.3 build 3 (`597e413`) | 29 passed |

All **87 assertions passed**, with no SurvivalFriend or region ownership errors.
Windows performance-counter errors were unrelated to plugin execution. An initial
sandboxed startup could not establish the JVM's loopback connection; the completed
verification runs used the local server outside that network restriction.
Logs are retained as `folia-initial.log`, `paper.log` and `folia-return.log`
inside the verification directory. All three servers stopped cleanly.

Covered behavior:

- Folia preserves enabled Gravity Duplication and Pearl Stasis settings. Their
  listeners are registered, menu items are available, and toggles still work.
- On Paper startup, both settings become `false` in memory and in the saved
  `config.yml`. The log identifies each automatically disabled setting.
- Paper registers neither feature's listeners and does not construct the stasis
  manager. Both menu slots contain placeholders. Direct menu-manager calls and
  simulated OP inventory clicks cannot enable either feature.
- Even setting either value to `true` in memory on Paper cannot bypass the
  central enabled/permission checks.
- Returning to Folia leaves both settings disabled until explicitly enabled.
  The main plugin switch continues to gate both features.
- An unrelated feature switch and a custom TPA cooldown survive every start.
  A saved chamber record, including its pending trigger, survives the round trip.
  Its file hash is identical after Paper and the return to Folia.

The harness uses native Bukkit inventories and registered listeners with simulated
Player objects; no Minecraft client or visual menu rendering is involved. The
saved chamber references an unloaded world so these checks do not resume or
trigger an actual chamber. Portal duplication physics and active stasis behavior
were not repeated for this configuration/platform change. Spigot was compiled
against but not run; its restrictions use the same non-Folia availability guard.

## Manual server check

1. Install `survivalfriend-0.8.6-BETA.jar` on Folia and enable Gravity Duplication
   and Pearl Stasis in `/menusf`.
2. Stop the server normally, replace Folia with Paper and start it with the same
   plugin data. Both settings must be saved as `false`, and both feature slots
   must be unavailable in `/menusf`.
3. Stop Paper and return to Folia. Both settings must remain disabled. Enable
   either feature explicitly to use it again. Saved chamber data is preserved.
