# v0.8.4-BETA: pearl restart and solo gravity duplication

Verified on 15 October 2026 using JDK 25.0.3 and Folia 26.3 build 3
(`597e413`), with two region threads. Tests ran in a disposable server bound to
`127.0.0.1:25576`. The existing server installation was only read.

## Reproduced failures

A real Minecraft protocol client logged in as the server's only player and
launched a native Ender pearl into an upward bubble chamber at
`(4096.5, 80.87, 4096.5)`. SurvivalFriend saved the chamber and its protected
placeholder. After a clean restart with v0.8.3, the chamber and its chunk tickets
were restored, but its position watcher stopped after its first correction.
The bubble column pushed the placeholder up to Y=90.64 instead of the saved
Y=80.87. Checking that a chamber record and an active map entry existed had
missed this failure in the previous verification.

Folia temporarily reports an entity as invalid while an asynchronous teleport
detaches and restores it. The watcher interpreted this as permanent removal and
stopped rescheduling itself. The fix waits for position correction to finish
before scheduling the next observation, and retries temporarily invalid entities
through their entity scheduler. Feature switches and generation checks still
stop old watchers; the scheduler's retirement callback removes retired entries.

The same premature return affected gravity copies during native portal travel
and the subsequent height correction. With v0.8.3, a native copy arrived in the
End with gravity disabled and never landed. A second, diagonal source was missed
entirely because checking the centre and one dominant-axis neighbour did not
include its edges. Portal detection now checks the falling block's bounding box
expanded in its direction of motion. Arrival and motion restoration keep waiting
through temporary invalidity. The portal timeout counts valid waiting ticks.

## Native checks

The solo scenario used the saved pearl chamber as the only persistent chunk
loader in the Overworld. Exactly one connected player entered a real End portal
and stayed in the End. No second player, spectator, harness force-load, or
additional plugin chunk ticket kept the source ticking. After a delay, native
falling blocks approached the portal beside a frame, first sideways and then
diagonally. Folia performed the portal transfers; the harness never teleported
sand into the End. An obsidian collector caught blocks leaving the End platform.

With v0.8.4, both copies arrived and produced either a placed sand block or a
collectible sand item in the End. The source chamber remained saved. No temporary
copy remained in the Overworld. Restarts also preserved exactly one placeholder
at its anchor, and the connected client received that placeholder's entity spawn
packet. All nine plugin tickets for the default 3x3 chamber area were restored.

| Scenario | Native assertions |
| --- | ---: |
| Clean restart: one protected placeholder at the anchor, visible to the client, nine tickets | 7 passed |
| Solo End travel, sideways and diagonal sand, native landing, no source copies left | 5 passed |
| Restored chamber triggers its owner back from the End, consumes the pearl and releases tickets | 3 passed |
| Cancelled portal entries expire without End copies | 6 passed |
| Disabling gravity duplication removes waiting copies | 6 passed |

All 27 assertions passed. No SurvivalFriend or region-ownership errors occurred.
Windows performance-counter warnings were unrelated to plugin execution. Local
logs are retained in the ignored `run/fixes-0.8.4/` directory.

The regression tests use controlled falling-block trajectories, not a complete
piston/redstone farm build. The manual procedure below covers a player's own
duper layout. Native server objects, the single client's login and dimension
change, persistence, entity scheduling, chunk tickets, portal travel and landing
are exercised directly.

Both `mvn -o clean verify -Dspigot-api.version=1.19.4-R0.1-SNAPSHOT` and the final
`mvn -o clean verify` against Spigot 26.2 passed. Release classes target Java 17.
The release JAR contains no test harness or test dependencies.

## Manual solo procedure

1. Replace the old SurvivalFriend JAR with `survivalfriend-0.8.4-BETA.jar` and
   restart Folia. Use only one SurvivalFriend JAR. Remove standalone FoliaFlow
   and PearlStasisFix when using the integrated features.
2. Enable the main switch, Gravity Duplication (slot 44) and Pearl Stasis
   (slot 52) in `/menusf`. Both Folia fixes are disabled by default.
3. Throw a pearl into an upward bubble chamber beside the duper's End portal.
   Wait for the stationary placeholder. The complete duper and its clock must
   lie inside the chamber's loaded chunk area. The default radius of 1 covers
   3x3 chunks around the chamber; increase `pearl-stasis.chunk-radius` and restart
   if the machine extends farther.
4. Stop and restart the server. Return to the chamber: exactly one pearl should
   remain at its previous position and still respond to the chamber's trigger.
5. Start the duper's clock and go through its portal alone. Stay by the End
   collector: sand must continue arriving and land as blocks or collectible
   items while no player remains in the Overworld. Test after a restart as well.

An actual collection platform is required: sideways momentum can carry a block
beyond the vanilla 5x5 obsidian arrival platform.
