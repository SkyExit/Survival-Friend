# v0.9.0-BETA verification

Verified on 8 November 2026. The release targets Spigot 26.3 and Java 17
(class-file version 61). The Maven build passed with JDK 25.0.3. The earlier
compatibility build against Spigot 1.19.4 also passed; the final release JAR
was built against 26.3 and used unchanged in every runtime below.

## Native server matrix

| Server | Java | Integration result |
| --- | --- | --- |
| Folia 26.3 build 5 (`8cffa90`) | 25.0.3 | 120 passed, 0 failed |
| Spigot 26.3 (`bed90fc-e89124d`, BuildTools revision 4664) | 25.0.3 | 120 passed, 0 failed |
| Folia 1.21.8 build 6 (`612d9bd`) | 25.0.3 | 120 passed, 0 failed |
| Spigot 1.19.4 (`7d7b241-5a5e43e`, revision 3763) | 17.0.21 | 120 passed, 0 failed |

All **480 assertions passed** and all four servers stopped cleanly. The exact
release JAR used on all four servers has SHA-256:

```text
525e2d0c57831a6bb48abcec678be0d9dc6214a85b52bd1efa65802a6e0caf9b
```

Each server uses its own disposable world and binds only to loopback, on ports
25579–25582. Folia uses two region threads. The harness stops each server after
reporting its result. Logs are retained at
`run/verification-0.9.0/<server>/release-console.log` (ignored by Git).
Spigot was built with the official Spigot BuildTools. The older Folia download
and portable Adoptium JDK were checked against their published SHA-256 values.
There were no SurvivalFriend execution or region-ownership errors. Test-fixture
logs contain flat-world generator warnings; the older Folia also warns about
the copied newer global-config schema. Windows performance-counter warnings in
the 26.3 environment are unrelated to plugin behavior.

## Coverage

- Migration of a disabled legacy `block-transformation.enable` to both new
  switches, removal of the old saved section, and preservation of explicitly
  configured independent switches.
- Separate cauldron, oven and beacon menu items; read-only clicks cannot change
  any of their settings. Operator menu switches persist to disk.
- All 16 native cauldron colors, whole-stack amounts and unchanged water levels;
  all 16 native furnace recipes, their 200-tick cooking time and actual output.
  Cauldrons work with ovens disabled, and ovens work with cauldrons disabled.
  Disabled ovens preserve their input and fresh fuel. Re-enabling converts
  waiting powder and starts blocked furnaces.
- Console `/menusf reload`, permission denial and completion, reading edited
  YAML, refreshing cached TPA times, portal-farm mobs, rules and Folia stasis
  radius. Unchanged rules preserve in-memory acceptance; changed rules require
  acceptance again. Existing TPA requests survive cooldown changes.
- Invalid YAML retains the previous live configuration. Beacon range is clamped
  to 50–1000. Reloading on Spigot disables both Folia-only settings in memory
  and on disk.
- Native four-layer beacon activation, Speed II outside vanilla range, selected
  secondary Regeneration, horizontal bounds, square corners and vertical bounds.
  Stone, tinted glass and double slabs block extension; single slabs and bedrock
  allow it. Destroying the pyramid stops extension.
- Live range changes, the beacon switch and the plugin's main switch, plus the
  recurring player watcher. A second loaded chunk 512 blocks from the beacon
  exercises the handoff to a distant Folia region using immutable snapshots.
  Chunk unload removes cached effects. No force-loaded chunks or SurvivalFriend
  beacon chunk tickets are created.

These are native-server integration checks: blocks, items, furnaces, beacons,
inventories, events, schedulers, configuration files and plugin lifecycle are
real server objects. Player objects are simulated; effect applications are
captured by the harness. No Minecraft client, visual menu review, real network
player movement or large-player-count performance benchmark is included.
Versions between the listed releases are not individually runtime-tested.
Existing portal-duplication and active saved-chamber physics were not repeated.

The first Folia pass exposed an incorrectly scheduled force-load query in the
test harness; it was moved to Folia's global scheduler. The first 1.19.4 pass
exposed a newer-only item-drop overload in the harness; the fixture now uses
the common two-argument Bukkit overload. Neither required an extra dependency
or a scheduler fallback in the released plugin.

## Reference review

Reviewed the Kotlin source of
[BeaconRanger at commit 6511481](https://github.com/TheLion102009/BeaconRanger/blob/651148161da3cd1f44be01f1890bd2bda922e0c7/src/main/kotlin/de/thelion/beaconranger/BeaconRanger.kt).
The implementation here is independent; no binaries, source code, Gradle wrapper
or dependencies from that repository were incorporated. In particular it avoids
forcing beacon chunks to remain loaded, removing other owners' force-load flags,
the immediate execution fallback after region-scheduler failures, and the
reference's Spigot fallback that does not actually extend the range.
The release contains no new network, process-execution or NMS access.

## Repeating the integration checks

1. Build the plugin with `mvn clean verify` using JDK 25.
2. Run `verification/v0.9.0/Build-Harness.ps1`. It compiles the main harness
   against the 26.3 API and a separate inventory-view adapter against 1.19.4.
   Explicit API JAR paths can be supplied as script parameters.
3. Prepare an empty disposable server directory with the chosen server JAR,
   its required Java runtime, EULA configuration and a loopback-only address.
   Put the release plugin and `run/verification-0.9.0/ReleaseVerification.jar`
   in `plugins/`. The harness changes blocks and stops the server, so use only
   a test world.
4. Seed `plugins/SurvivalFriend/config.yml` with:

   ```yaml
   enabled: true
   block-transformation:
     enable: false
   ```

5. Start with `java -Xms256M -Xmx1G -jar server.jar --nogui` and check the
   `VERIFICATION RESULT` line and clean shutdown. On Folia, configure
   `threaded-regions.threads: 2` in `config/paper-global.yml`.

The harness and its adapter are outside `src/main` and are not packaged into
the release plugin. Maven itself has no unit-test suite; the integration checks
are performed by this separate native-server harness.
