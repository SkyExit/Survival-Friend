# SurvivalFriend v0.8.5-BETA

A survival server plugin for Spigot, Paper and Folia. Build with JDK 25 and Maven;
this project targets the Spigot 26.2 API and produces Java 17 compatible plugin
classes. Use the Java version required by your Minecraft server.

## Features

- **Creeper protection:** Control whether creepers can destroy blocks or damage
  players. Both protections can be enabled independently.
- **Crop trampling:** Prevent players, mobs, or both from trampling farmland.
- **TPA:** Send teleport requests with `/tpa <player>` and accept them with
  `/tpa <player> accept` or the clickable chat button. The default cooldown is
  60 minutes and can be changed with `tpa.cooldown-seconds`; use `0` for no
  cooldown. Requests expire after 120 seconds by default, configurable through
  `tpa.request-timeout-seconds`.
- **Portal farms:** Prevent selected mobs from despawning because they are far
  from a player after entering a portal. The default list includes Guardians,
  Iron Golems, Creepers, Wither Skeletons and Zombified Piglins. Edit `spf.mobs`
  to choose other mobs; the config includes a commented list of mob names.
  This feature does not keep chunks loaded or increase the mob cap.
- **Harder Dragon:** Set the Ender Dragon's maximum health through `hd.health`.
  With `hd.enable` enabled, the value applies to newly spawned and respawned
  dragons. The plugin default is 1000 HP; vanilla base health is 200 HP
  (100 hearts). Existing dragons are not changed when the configuration is edited.
- **Totem pop notifications:** Broadcast a death-style message when a player
  uses a Totem of Undying. Messages support both hands, environmental damage,
  projectile shooters, dispensers, TNT igniters, lingering clouds and evoker
  fangs. Cancelled resurrection and resurrection without a totem do not announce
  a pop. Cross-region notifications use each recipient's entity scheduler.
- **Ender chest and workbench:** Open your ender chest with `/ec` or a crafting
  table with `/wb`. Each command can be available to everyone, restricted to
  admins, or disabled.
- **Compass:** Point a compass towards a player or an X/Z location with
  `/compass <player>` or `/compass <x> <z>`. Relative coordinates and `reset`
  are supported. Player targets must be in the same world.
- **Server rules:** `/rules` opens a paginated menu where players confirm each
  rule and then accept the complete list. This feature is **disabled by default**.
  Enable it with `rules.enable` or the **Server Rules** item in the admin menu.
- **Feature menu:** Everyone can open `/menusf`. Operators can toggle individual
  features and the plugin as a whole. Other players see the same inventory with
  `Read Only` in its title; every click checks current OP status before changing
  settings. Item movement and dragging are blocked for all menu viewers.
- **Block Transformation:** Enabled by default; toggle the Cauldron in slot 38
  of `/menusf` or set `block-transformation.enable`. Drop concrete powder into a
  water cauldron to turn the entire stack into concrete of the same color without
  consuming water. Smelt concrete powder in a normal furnace to obtain matching
  stained glass, one glass block per powder, with normal fuel consumption and
  cooking time (200 ticks per item). All 16 colors are supported. Empty, lava and
  powder snow cauldrons, blast furnaces, smokers and campfires do not perform these
  transformations. The feature switch and the plugin's main switch take effect
  immediately, including for items already in cauldrons and batches being smelted.
- **Gravity duplication (Folia only):** Restore gravity block duplication at End
  portals. Toggle the Sand item directly above the bottom-right corner of the
  admin menu (slot 44). Disabled by default; changes take effect immediately and
  are saved as `gravity-duplication.enable`. The feature is ignored on Paper and
  Spigot, even if enabled in the config. The plugin's main switch also disables it.
  Sideways entries that break into items are supported. Temporary copies stay
  inside the portal until Folia completes the actual dimension change; only then
  are their momentum and gravity restored. Temporary invalidity during an
  asynchronous transfer no longer stops this recovery. Portal detection includes
  the falling block's edges and diagonal motion. Blocked transfers are cleaned up.
- **Pearl stasis fix (Folia only):** Restore Ender pearl stasis chambers using
  persistent pearl placeholders. Toggle the Eye of Ender in slot 52, directly
  left of the bottom-right corner. Disabled by default and ignored on Paper and
  Spigot. Upward bubble columns and honey chambers are supported; ordinary water
  and downward bubble columns do not arm pearls. Saved chambers survive player
  disconnects, server restarts and disabling the feature. Colliding blocks or
  interacting with a placeholder trigger its owner; offline triggers wait for
  the owner to reconnect. Failed or cancelled teleports keep the chamber.
  While enabled, each chamber keeps a configurable area of chunks ticking
  (`pearl-stasis.chunk-radius`, default 1 for 3x3 chunks); overlapping chambers
  share tickets. Disabling the feature or the plugin's main switch releases
  these tickets. The server must remain running for its chunks to tick.
  Restored placeholders stay at their saved position after a restart, including
  in bubble columns that push dropped items upward.
- **Paper exploit status:** The Comparator in the bottom-right corner (slot 53)
  shows the four loaded Paper settings below, green for enabled and red for
  disabled. Available on Paper and Folia; clicking refreshes the display.

| Setting under `unsupported-settings` | Mechanic |
| --- | --- |
| `allow-piston-duplication` | TNT, carpet and rail duplication |
| `allow-headless-pistons` | Headless piston mechanics |
| `allow-permanent-block-break-exploits` | Bedrock and End portal frame breaking |
| `allow-unsafe-end-portal-teleportation` | End portal exploits; alone, this does not fix sand duplication on Folia |

These are Paper's settings in `config/paper-global.yml`. The display reads the
active configuration, including when a custom configuration directory is used.
Edit the file and restart to apply changes; the menu does not change Paper's
settings. If a value cannot be read on an unsupported server version, it is
shown as `UNKNOWN` in red.

## Installation and configuration

Run `mvn clean package` and copy `target/survivalfriend-0.8.5-BETA.jar` into your
server's `plugins` folder. Start the server to create
`plugins/SurvivalFriend/config.yml`. Existing settings are preserved and missing
defaults are added automatically.

Restart after editing health, cooldowns, the mob list or the rule text. Feature
switches in the admin menu take effect immediately and are saved to the config.

```yaml
hd:
  enable: false
  health: 1000 # Vanilla: 200 HP = 100 hearts.
tpa:
  enable: true
  cooldown-seconds: 3600
  request-timeout-seconds: 120
spf:
  enable: true
  mobs:
    - WITHER_SKELETON
    - CREEPER
    - IRON_GOLEM
    - ZOMBIFIED_PIGLIN
    - GUARDIAN
rules:
  enable: false
  entries:
    - 'Treat other players with respect.'
    - 'Griefing and stealing are not allowed.'
```

Unknown mob names and non-mob entries are skipped with a startup warning. Mob
names are case insensitive; newer mob types require a matching Minecraft version.
An empty `spf.mobs` list protects no additional mobs. Protection already applied
to a mob remains in place even after the feature is disabled.

When rules are enabled, movement, teleportation, portals and mounting are blocked
until all rules have been accepted. Menus show up to 45 rules per page. Acceptance
is stored by UUID and rule revision in `accepted-rules.yml`; editing the rules
requires renewed acceptance. Disabling rules immediately releases players and
closes their rule menus. Re-enabling them prompts players who still need to accept.
An empty `rules.entries` list also disables the movement restriction.

The old rule list is migrated automatically to `rules.entries`, preserving custom
rules and existing acceptance when the text is unchanged. The command is now
`/rules`.

## Compatibility and verification

See [the v0.8.5 verification record](docs/verification-0.8.5.md) for the read-only
menu and native Block Transformation checks.

Entity tasks follow their player or dragon on Paper and Folia; Spigot uses the
Bukkit scheduler. TPA uses asynchronous teleportation where supported. Shared
request, consent and configuration state is synchronized across regions.

See [the pearl stasis verification record](docs/verification-pearl-stasis.md)
for the current feature checks, and
[the v0.7.2 record](docs/verification-0.7.2.md) for previous feature checks.
Java test sources and test-only Maven dependencies are not included in this release.

See [the v0.8.3 gravity verification record](docs/verification-gravity-0.8.3.md)
for the reproduced sideways-entry failure and real End arrival and collection
checks. Folia's portal behavior differs from Paper; see
[the upstream explanation](https://github.com/PaperMC/Folia/issues/297).

See [the v0.8.4 regression record and solo test instructions](docs/verification-0.8.4.md)
for the reproduced restart and frozen-sand failures. A pearl chamber beside the
duper keeps its Overworld chunks ticking while the only player waits in the End.
The chamber's configured chunk radius must cover the complete duper and its
redstone clock; both Pearl Stasis and Gravity Duplication must be enabled.

Gravity duplication is integrated into SurvivalFriend and does not require
FoliaFlow. Remove the standalone FoliaFlow plugin when using this feature to
avoid both plugins duplicating the same falling block. Motion defaults are
adapted from [FoliaFlow](https://github.com/Hynse/FoliaFlow); its MIT license is
included in the plugin JAR at `META-INF/licenses/FoliaFlow.txt`.

Pearl stasis behavior is adapted from
[PearlStasisFix](https://github.com/0xEEF/PearlStasisFix). Its MIT license is
included at `META-INF/licenses/PearlStasisFix.txt`; the standalone plugin is not
required. Chamber data is stored in `plugins/SurvivalFriend/pearl-stasis.yml`.
Existing data from the standalone plugin is not imported automatically.
Use SurvivalFriend's implementation without the standalone PearlStasisFix plugin.

The Paper setting descriptions follow the
[Paper global configuration reference](https://docs.papermc.io/paper/reference/global-configuration/#unsupported-settings).

## Changes since v0.6

- Added persistent, paginated rule acceptance, now named `/rules`, disabled by
  default and configurable through the admin menu.
- Added Folia scheduling and safer cross-region TPA, compass and chat handling.
- Made the portal-farm mob list configurable and documented dragon health and
  TPA cooldown settings.
- Expanded totem pop messages and corrected indirect damage attribution.
- Fixed TPA expiration, disconnect cleanup and retrying cancelled teleports;
  improved compass arguments, admin menu checks, dragon damage and creeper protection.
- Standardized plugin text, configuration comments and documentation in English;
  removed Java test sources and their build dependencies.
