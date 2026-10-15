# Gravity duplication verification — v0.8.3-BETA

Verified on 2026-10-15 using JDK 25.0.3 and an isolated Folia 26.3 build 3
(`597e413`) server with two region threads. The existing server installation was
only read; its plugins, configuration and worlds were not changed.

## Failure and implementation

The previous implementation missed falling blocks that broke into items beside
the portal: they did not report a successful block placement. Its two-tick
position adjustment also ran without establishing that the copy had changed
dimensions. Moving copies could leave the portal or hit its support before
Folia completed the native transfer.

The fix handles `EntityDropItemEvent` as well as block placement and fence portal
entry. It checks both the current block and the adjacent block in the direction
of motion. A marked copy stays stationary inside the portal, above the fence's
collision surface, until its world changes. Its height adjustment and restored
motion run on its entity scheduler in the destination. A blocked transfer expires
after 100 scheduled ticks; disabling the feature also removes pending copies.
Temporary copies cannot drop items or persist while waiting.

The native portal chooses the destination and loads its chunks. This is a plugin
workaround, not a modification of Folia's entity ticking. Relevant primary sources:

- [Folia issue 297](https://github.com/PaperMC/Folia/issues/297): why continuing to
  tick a removed source entity cannot safely reproduce vanilla duplication.
- [Folia region overview](https://docs.papermc.io/folia/reference/overview/):
  entity ownership and asynchronous dimension transfers.
- [Paper entity teleport documentation](https://docs.papermc.io/paper/dev/entity-teleport/):
  asynchronous chunk loading and teleport completion.
- [Original FoliaFlow portal watcher](https://github.com/MidnightTale/FoliaFlow/blob/master/src/main/java/xyz/hynse/foliaflow/watcher/PortalWatcher.java):
  the original placement/fence event paths and motion coefficients.

## Native server checks

The sideways reproduction used a real End portal at `(8, 80, 8)`, an End portal
frame at `(7, 80, 8)`, and a native falling block starting at
`(7.1, 81.2, 8.5)` with velocity `(1.2, -0.6, 0)`. The server performed all falling
physics and portal transfers. No test code teleported the block into the End.
This is a controlled reproduction of the entry motion, not a complete piston
farm build.

The previous code produced no copy and no End arrival for that reproduction.
The fixed build passed these checks:

| Scenario | Result | Checks |
| --- | --- | ---: |
| Sideways sand, red sand, gravel and white concrete powder | Exactly four marked copies arrived in the End and landed with their original materials | 8 |
| Destination End chunk initially unloaded | Native portal loaded the destination; the sand copy arrived and landed | 6 |
| Fence beneath the portal | Copy arrived in the End and dropped a collectible sand item after landing against existing sand | 5 |
| Portal entry cancelled for the copy | No End copy; temporary entity removed after timeout | 4 |
| Feature disabled during a pending transfer | No End copy; temporary entity removed | 4 |

Arrival was measured with `EntityAddToWorldEvent` in the End and the
`survivalfriend_gravity_duplicate` tag, distinguishing copies from source blocks.
Collection was measured through an uncancelled block-placement or item-drop
event in the End. The four-material scenario recorded four arrivals and four
placements. All 27 native assertions passed without plugin or region-thread
errors. Windows performance-counter warnings were unrelated to plugin execution.

## Additional checks and packaging

All 21 focused Java assertions passed. They covered restored velocity and entity
flags, scheduler ownership, successful/failed/exceptional height adjustments,
deduplication across event paths, timeouts, removal, feature switches, foreign
region access, legacy FoliaFlow tags, and the Paper/Spigot platform guard.

`mvn -o clean verify` passed against the default Spigot 26.2 API. A separate build
against Spigot 1.19.4 also passed; the final artifact was rebuilt with the default
API. Plugin classes target Java 17. The feature remains Folia-only, disabled by
default, and controlled by menu slot 44 and the main switch.

Temporary Java verification sources, compiled harnesses and the disposable
server were removed after testing. No test dependency was added to the project;
test classes and test libraries are absent from the release JAR. Local result
logs are retained under the ignored `run/gravity-0.8.3/` directory.

## Deployment

Replace the old SurvivalFriend JAR with `survivalfriend-0.8.3-BETA.jar`, remove the
standalone FoliaFlow plugin, restart the server, and enable Gravity Duplication in
slot 44. The inspected original installation was still running SurvivalFriend
0.8.0-BETA together with FoliaFlow 3.0, and its SurvivalFriend gravity setting was
disabled. The build does not automatically change that installation.
