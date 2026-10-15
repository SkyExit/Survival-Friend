# SurvivalFriend v0.8.0-BETA

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
- **Admin menu:** `/menusf`, also available as `/menufs`, lets operators toggle
  individual features and the plugin as a whole.
- **Gravity duplication (Folia only):** Restore gravity block duplication at End
  portals. Toggle the Sand item directly above the bottom-right corner of the
  admin menu (slot 44). Disabled by default; changes take effect immediately and
  are saved as `gravity-duplication.enable`. The feature is ignored on Paper and
  Spigot, even if enabled in the config. The plugin's main switch also disables it.
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

Run `mvn clean package` and copy `target/survivalfriend-0.8.0-BETA.jar` into your
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

Entity tasks follow their player or dragon on Paper and Folia; Spigot uses the
Bukkit scheduler. TPA uses asynchronous teleportation where supported. Shared
request, consent and configuration state is synchronized across regions.

See [the v0.8.0 verification record](docs/verification-0.8.0.md) for the gravity
duplication and Paper status checks, and
[the v0.7.2 record](docs/verification-0.7.2.md) for previous feature checks.
Java test sources and test-only Maven dependencies are not included in this release.

Gravity duplication is integrated into SurvivalFriend and does not require
FoliaFlow. Remove the standalone FoliaFlow plugin when using this feature to
avoid both plugins duplicating the same falling block. Motion defaults are
adapted from [FoliaFlow](https://github.com/Hynse/FoliaFlow); its MIT license is
included in the plugin JAR at `META-INF/licenses/FoliaFlow.txt`.

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
