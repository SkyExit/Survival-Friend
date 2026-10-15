# Pearl stasis verification for v0.8.0-BETA

Verified on 15 October 2026 with JDK 25.0.3 on Windows 11.

## Automated checks

25 isolated checks passed against the Spigot 1.19.4 API. They covered the
default-disabled setting, slot 52, the Folia platform guard, ordinary throws,
plain water, downward bubble columns, brief medium contact, bubble and honey
capture thresholds, placeholder protection, persistence, offline triggers,
owner-scheduler handoff, cancelled and successful asynchronous teleports,
feature toggles, the main plugin switch and overlapping chunk tickets.

Both `mvn -o clean verify` and the build with
`-Dspigot-api.version=1.19.4-R0.1-SNAPSHOT` passed. The release is built against
Spigot 26.2-R0.1-SNAPSHOT with Java 17 compatible bytecode.

## Native server checks

| Server and scenario | Result |
| --- | --- |
| Folia 26.3 build 3, commit 597e413: capture and menu controls | 19 checks passed |
| Same Folia instance after a clean restart | 11 checks passed |
| Folia: block collision and main plugin switch | 9 checks passed |
| Paper 26.3 build 183, commit ec0cd5c: platform restrictions | 4 checks passed |

The supplied server JARs and libraries ran in a disposable instance in the
project's ignored `run` directory, bound to `127.0.0.1:25575`. Existing server
worlds and configuration were not changed. A harness force-loaded the test
chunk and supplied Player proxies for command execution and projectile ownership;
no multiplayer clients were connected. The pearls, placeholder items, blocks,
entity schedulers, event dispatcher, configuration and chunk tickets were native
server objects.

On Folia, native pearls held in an upward bubble column and beside a honey block
were captured, while a pearl in ordinary water was ignored. Both chambers
belonged to the same owner and produced separate protected placeholders. An
offline interaction set a persistent pending trigger without consuming its
pearl. Disabling released chunk tickets and removed active placeholders;
re-enabling restored the saved chambers, including during a rapid off/on cycle.

A clean stop and restart restored both chambers without duplicate placeholders,
reinstated chunk tickets and retained the offline trigger. A newly placed block
intersecting the second chamber triggered it through normal ticking. The main
plugin switch also released and restored chamber resources. On Paper, slot 52
contained only a filler item, the stasis listener was absent and clicking that
slot could not toggle the Folia setting.

Successful and cancelled player teleports were checked with controlled futures
in the isolated tests; this run did not exercise an actual connected player's
cross-dimension teleport. No plugin or region ownership errors occurred during
the native checks.

## Release cleanup

Temporary Java tests, decompiled reference sources, test fixtures, decompiler
and disposable server files were removed. Test dependencies were not added to
the project. The release JAR includes the PearlStasisFix MIT attribution and
contains no test classes or libraries.
