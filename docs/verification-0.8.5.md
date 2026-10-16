# v0.8.5-BETA: read-only menu and Block Transformation

Checked on 15 October 2026 with JDK 25.0.3. Both offline Maven builds passed:

- Spigot 1.19.4: `mvn -o clean verify -Dspigot-api.version=1.19.4-R0.1-SNAPSHOT`
- Spigot 26.2: `mvn -o clean verify`

The release JAR targets Java 17 and contains no verification plugin or test
dependencies. The Maven project has no automated test sources; the native checks
below run separately from Maven.

## Native test setup

A disposable Folia 26.3 build 3 (`597e413`) server uses two region threads and
binds to `127.0.0.1:25577`. Its files and verification harness are under the ignored
`run/block-transformation-0.8.5/` directory. The existing test server's JARs and
libraries were copied; its plugins, configuration and worlds were not changed.

Menu checks use real Bukkit inventories and events with simulated Player objects
to control OP status. They invoke the command executor and listeners directly;
no Minecraft client or visual menu rendering is part of these checks. Cauldron
checks use real dropped item entities and normal gravity. Furnace checks use
native ticking block entities, registered recipes and real fuel consumption.
The harness does not simulate completion events or shorten cooking times.

All **308 assertions passed**, with no SurvivalFriend or region ownership errors.
The completed run's log is retained at
`run/block-transformation-0.8.5/verified.log`. Windows performance-counter warnings
were unrelated to plugin execution. The test server shut down cleanly afterward.

## Covered behavior

- A non-OP player opens the existing menu with `Read Only` in its title. The
  transformation item appears in that same inventory.
- All menu controls, the main switch, lower inventory and outside clicks reject
  non-OP actions. Left/right clicks, shift clicks, number keys, double clicks,
  offhand swaps and drop actions leave every configuration value unchanged.
  Dragging is cancelled as well.
- OP players keep the normal title and can toggle Block Transformation. Removing
  OP while the inventory remains open prevents the next click from changing it.
- All 16 powder colors become matching concrete in water cauldrons. The cases
  cover water levels 1, 2 and 3, and stack sizes 1, 7 and 64. No item amount or
  cauldron water level changes.
- Empty and powder snow cauldrons, an item outside the cauldron cavity and an
  ordinary concrete stack do not transform.
- All 16 registered recipes produce one matching stained glass per powder at
  the normal 200 ticks per item. Native furnaces consume coal and process two
  powder into exactly two glass of the same color. Without fuel, input stays put.
- Disabling the feature during cooking prevents output and preserves all input.
  A cold furnace does not consume new fuel while disabled. Ordinary sand still
  smelts into glass, and blast furnaces and smokers leave powder and coal alone.
- Re-enabling the feature processes items already inside cauldrons and resumes
  existing furnace inputs. The plugin's main switch also gates both conversions
  and new fuel consumption, and re-enabling it resumes both.

## Manual server check

1. Install `survivalfriend-0.8.5-BETA.jar` and restart. Missing defaults are added
   to existing configurations; `block-transformation.enable` defaults to `true`.
2. Open `/menusf` without OP. Confirm the `Read Only` title and that clicking or
   dragging items cannot move anything or change any feature.
3. As OP, toggle **Block Transformation**, the Cauldron in slot 38. Drop a stack
   of concrete powder into a water cauldron. Confirm matching concrete, the same
   stack size and an unchanged water level.
4. Smelt the powder in a normal furnace with fuel. Confirm matching stained
   glass, one per input. Check a blast furnace and smoker do not process it.
5. Disable the feature during cooking and drop powder into another cauldron.
   Confirm neither produces new output. Re-enable it and confirm both continue.
   Repeat with the plugin's main switch.
