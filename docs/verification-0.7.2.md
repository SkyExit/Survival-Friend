# v0.7.2-BETA verification

Verified on 15 October 2026 with JDK 25.0.3 on Windows 11.

## Automated checks before removing Java test sources

| API used for compilation and tests | Result |
| --- | --- |
| Spigot 26.2-R0.1-SNAPSHOT | 96 passed, 0 failures, 0 errors |
| Spigot 1.19.4-R0.1-SNAPSHOT | 93 passed, 0 failures, 0 errors |

The different counts come from damage causes added to newer APIs. The checks
covered every damage cause exposed by each API, cancelled resurrection, missing
damage, synthetic resurrection without a totem, both hands, projectile shooters,
dispensers, TNT igniters, evoker fangs, lingering clouds, empty hands and foreign
region ownership. Mob list checks covered custom names, whitespace, case,
unknown entries, non-mob entries, empty lists and disabled protection. Existing
TPA, compass, rule acceptance, persistence and scheduler checks also passed.

Additional rule checks covered a missing enable setting, immediate release when
disabled, closing open menus, prompting on re-enable and queued menu updates
that must not reopen disabled rules.

The Java test sources, test fixtures, test-only constructor, JUnit, Mockito and
Mockito agent configuration were then removed as requested. The release build
was checked with `mvn clean verify`; that final command builds the plugin without
running the removed tests. The packaged plugin version is `0.7.2-BETA` and the
JAR contains no test classes or bundled test libraries.

## Live multiplayer checks

The existing test installation supplied the server JARs, libraries and accepted
EULA. All tests ran in a separate local instance inside this project's ignored
`run` directory, bound to `127.0.0.1:25575`, using newly generated test worlds.
The original test installation and its worlds were not changed.

| Server | Result |
| --- | --- |
| Folia 26.3 build 3, commit 597e413, two region threads | All portal, totem, dragon and rule menu checks passed |
| Folia after a clean stop and restart | All five protected mobs retained their saved protection |
| Paper 26.3 build 190 | All portal, totem, dragon and rule menu checks passed |

The supplied Folia startup script says 26.2 in its title, but launches
`folia-26.3-3.jar`. The live tests therefore used Minecraft 26.3. Compilation and
automated API checks used 26.2 and 1.19.4; no 26.2 server was run.

Two protocol clients, `PortalUser` and `RemoteUser`, connected as real players.
The available client protocol was 26.1, so the test instance used ViaVersion and
ViaBackwards 5.12.1 as protocol adapters. These adapters are not plugin dependencies.

### Portal farms

One player remained within 128 blocks of the Overworld source portal. The other
was in the Nether at X/Z 1024.5, approximately 1450 blocks from the destination
portal. Matching portals and forced chunks kept the test area ticking; chunk
loading was supplied by the test harness, not by SurvivalFriend.

Creepers, Wither Skeletons, Iron Golems, Guardians and Zombified Piglins were
spawned with distance despawning initially enabled. All five types crossed an
actual Nether portal and were found alive in the Nether after 12 seconds, with
`getRemoveWhenFarAway()` returning `false`. A Zombie not included in the list
was removed with `EntityRemoveEvent.Cause.DESPAWN`.

The Folia instance was stopped normally and restarted. All five protected mob
types were still present and retained their protection. No plugin ownership or
cross-region access exceptions occurred in the successful runs.

### Totems and dragon health

Both players received the totem messages for actual lethal fall and drowning
damage. A totem in the off hand and then in the main hand was consumed, and the
player survived at 1 HP. A spawned dragon with `hd.health: 200` had exactly 200 HP.

### Rules and admin menu

The feature started disabled. `/rules` reported that it was disabled, and
`/menufs` opened the operator menu. The Server Rules item was present in slot 23.
Programmatic inventory clicks dispatched through Bukkit's event system enabled
and disabled the actual menu setting. Enabling required acceptance and opened
the rules menu; disabling immediately removed the restriction and prevented
queued tasks from reopening that menu. These checks passed on Folia and Paper.

A legacy configuration containing a custom rule was upgraded on Folia. That rule
was preserved under `rules.entries`, and the old list key was removed.

## Scope

These checks establish the plugin's portal despawn protection under the tested
multiplayer conditions. They do not benchmark farm output, change mob caps, keep
production chunks ticking, test every possible farm layout or cover interactions
with arbitrary mob-clearing plugins. The existing five default mob types are
unchanged; ordinary Piglins are a separate configurable type.

Implementation references:

- [Paper and Folia scheduler guidance](https://docs.papermc.io/paper/dev/folia-support/)
- [Bukkit portal contact event](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/EntityPortalEnterEvent.html)
- [Bukkit resurrection event and totem hand](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/EntityResurrectEvent.html)
- [Folia 26.2 portal transfer source](https://github.com/PaperMC/Folia/blob/ver/26.2.x/folia-server/minecraft-patches/features/0001-Region-Threading-Base.patch)
