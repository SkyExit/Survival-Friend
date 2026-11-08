package verification;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.compat.ServerApi;
import de.laurinhummel.survivalfriend.compat.ServerPlatform;
import de.laurinhummel.survivalfriend.events.BeaconRangeExtender;
import de.laurinhummel.survivalfriend.managers.*;
import de.laurinhummel.survivalfriend.scheduling.ServerTasks;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Slab;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;

import java.io.File;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

/** Disposable native-server integration checks; never included in the release JAR. */
public final class ReleaseVerification extends JavaPlugin {
    private SF sf;
    private ServerTasks tasks;
    private World world;
    private Location anchor;
    private BeaconRangeExtender extender;
    private int passed, failed;
    private final List<Item> powders = new ArrayList<>();
    private final List<Furnace> furnaces = new ArrayList<>();
    private Item waiting, ovenOffPowder;
    private Furnace independent, disabledFurnace;
    private FakePlayer recipient;

    @Override public void onEnable() {
        sf = SF.getPlugin(); tasks = new ServerTasks(this);
        globalLater(40, () -> {
            world = Bukkit.getWorlds().get(0); anchor = new Location(world, 8, 85, 8);
            tasks.at(anchor, () -> guarded(this::setup));
        });
    }

    private void globalLater(long delay, Runnable action) {
        if (!ServerPlatform.isFolia()) { Bukkit.getScheduler().runTaskLater(this, action, delay); return; }
        try {
            Method getter = Bukkit.class.getMethod("getGlobalRegionScheduler");
            getter.getReturnType().getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                    .invoke(getter.invoke(null), this, (Consumer<Object>) ignored -> action.run(), delay);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private void guarded(Checked action) {
        try { action.run(); }
        catch (Throwable error) { failed++; getLogger().log(java.util.logging.Level.SEVERE, "HARNESS FAILURE", error); finish(); }
    }
    private interface Checked { void run() throws Exception; }
    private void later(long ticks, Checked action) { tasks.later(anchor, () -> guarded(action), ticks); }
    private void check(boolean result, String label) {
        if (result) { passed++; getLogger().info("PASS " + label); }
        else { failed++; getLogger().severe("FAIL " + label); }
    }
    private void setting(String path, Object value) { synchronized (ConfigAccess.LOCK) { sf.getConfig().set(path, value); } }
    private Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private YamlConfiguration disk() throws Exception {
        YamlConfiguration data = new YamlConfiguration(); data.load(new File(sf.getDataFolder(), "config.yml")); return data;
    }
    private void edit(Map<String, Object> changes) throws Exception {
        YamlConfiguration data = disk(); changes.forEach(data::set); data.save(new File(sf.getDataFolder(), "config.yml"));
    }
    private Block block(int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z); block.setType(material, false); return block;
    }
    private Item powder(int x, int z, Material material, int amount) {
        Block cauldron = block(x, 79, z, Material.WATER_CAULDRON);
        Levelled water = (Levelled) cauldron.getBlockData(); water.setLevel(3); cauldron.setBlockData(water, false);
        Item item = world.dropItem(cauldron.getLocation().add(.5, .4, .5), new ItemStack(material, amount));
        item.setGravity(false); item.setVelocity(new org.bukkit.util.Vector()); item.setPickupDelay(Integer.MAX_VALUE);
        return item;
    }
    private Furnace furnace(int x, int z, Material input) {
        Furnace furnace = (Furnace) block(x, 75, z, Material.FURNACE).getState();
        furnace.getInventory().setSmelting(new ItemStack(input)); furnace.getInventory().setFuel(new ItemStack(Material.COAL)); return furnace;
    }
    private void setup() throws Exception {
        world.addPluginChunkTicket(0, 0, this);
        check(sf.isEnabled() && sf.getDescription().getVersion().equals("0.9.0-BETA"), "release loads with correct version");
        check(!sf.getConfig().getBoolean("cauldron-concrete.enable") && !sf.getConfig().getBoolean("oven-glass.enable"), "legacy disabled switch migrates to both features");
        check(!disk().contains("block-transformation"), "legacy key removed from saved config");
        check(!sf.getConfig().getBoolean("beacon-range-extender.enable"), "beacon extender defaults off");
        extender = (BeaconRangeExtender) HandlerList.getRegisteredListeners(sf).stream().map(listener -> listener.getListener())
                .filter(BeaconRangeExtender.class::isInstance).findFirst().orElseThrow();
        configurationChecks(); menuChecks();
        setting("enabled", true); setting("cauldron-concrete.enable", true); setting("oven-glass.enable", true);
        setting("beacon-range-extender.enable", true); setting("beacon-range-extender.range", 100);
        int i = 0;
        for (DyeColor color : DyeColor.values()) {
            Material input = Material.valueOf(color + "_CONCRETE_POWDER");
            powders.add(powder(1 + (i % 4) * 2, 1 + (i / 4) * 2, input, 7));
            furnaces.add(furnace(1 + (i % 4) * 2, 1 + (i / 4) * 2, input));
            Recipe recipe = Bukkit.getRecipe(new NamespacedKey(sf, "block_transformation/" + input.name().toLowerCase(Locale.ROOT)));
            check(recipe instanceof FurnaceRecipe && ((FurnaceRecipe) recipe).getCookingTime() == 200, "native recipe " + color);
            i++;
        }
        for (int layer = 1; layer <= 4; layer++) for (int x = 8 - layer; x <= 8 + layer; x++)
            for (int z = 8 - layer; z <= 8 + layer; z++) block(x, 85 - layer, z, Material.IRON_BLOCK);
        Beacon beacon = (Beacon) block(8, 85, 8, Material.BEACON).getState();
        beacon.setPrimaryEffect(PotionEffectType.SPEED); beacon.setSecondaryEffect(PotionEffectType.SPEED); beacon.update();
        extender.onLoad(new ChunkLoadEvent(world.getChunkAt(0, 0), false));
        recipient = new FakePlayer(true); recipient.location = anchor.clone().add(80.5, 0, .5);
        later(100, this::cauldronChecks);
    }

    private void configurationChecks() throws Exception {
        MenuSF menu = new MenuSF(); FakePlayer unprivileged = new FakePlayer(false);
        var before = sf.getConfig(); edit(Map.of("tpa.cooldown-seconds", 37));
        menu.onCommand(unprivileged.player, sf.getCommand("menusf"), "menusf", new String[]{"reload"});
        check(sf.getConfig() == before && unprivileged.messages.stream().anyMatch(m -> m.contains("permission")), "unauthorized reload denied");
        check(menu.onTabComplete(unprivileged.player, sf.getCommand("menusf"), "menusf", new String[]{"r"}).isEmpty(), "reload completion hidden without permission");
        menu.onCommand(Bukkit.getConsoleSender(), sf.getCommand("menusf"), "menusf", new String[]{"reload"});
        check(sf.getConfig() != before && sf.getConfig().getInt("tpa.cooldown-seconds") == 37, "console reload applies disk config");
        check(menu.onTabComplete(Bukkit.getConsoleSender(), sf.getCommand("menusf"), "menusf", new String[]{"RE"}).equals(List.of("reload")), "reload tab completion");
        Object tpa = field(sf, "tpa"); Object requests = field(tpa, "requests");
        check((long) field(requests, "cooldownMillis") == 37_000, "cached TPA cooldown refreshed");
        edit(Map.of("tpa.request-timeout-seconds", 9, "spf.mobs", List.of("ZOMBIE"), "rules.entries", List.of("A new rule"), "pearl-stasis.chunk-radius", 0));
        sf.reloadSettings();
        check((long) field(requests, "timeoutMillis") == 9_000, "cached TPA timeout refreshed");
        check(field(field(sf, "soloPortalFarm"), "farmMobs").equals(Set.of(EntityType.ZOMBIE)), "portal mob list refreshed");
        RulesAgreement agreement = (RulesAgreement) field(sf.getRules(), "agreement");
        check(agreement.getRules().equals(List.of("A new rule")), "rule text refreshed");
        UUID accepted = UUID.randomUUID(); agreement.restore(accepted, agreement.getRevision());
        sf.reloadSettings(); check(((RulesAgreement) field(sf.getRules(), "agreement")).hasAccepted(accepted), "unchanged rules retain in-memory consent");
        edit(Map.of("rules.entries", List.of("Changed again"))); sf.reloadSettings();
        check(!((RulesAgreement) field(sf.getRules(), "agreement")).hasAccepted(accepted), "changed rules require new consent");
        if (sf.getPearlStasis() != null) {
            Object settings = field(sf.getPearlStasis(), "settings"); Method radius = settings.getClass().getDeclaredMethod("radius"); radius.setAccessible(true);
            check((int) radius.invoke(settings) == 0, "stasis radius refreshed on Folia");
        }
        File configFile = new File(sf.getDataFolder(), "config.yml"); String valid = Files.readString(configFile.toPath());
        before = sf.getConfig(); Files.writeString(configFile.toPath(), "enabled: [broken\n");
        boolean rejected = false; try { sf.reloadSettings(); } catch (Exception expected) { rejected = true; }
        check(rejected && sf.getConfig() == before, "malformed YAML retains running config");
        Files.writeString(configFile.toPath(), valid);
        edit(Map.of("beacon-range-extender.range", 999999, "gravity-duplication.enable", true, "pearl-stasis.enable", true)); sf.reloadSettings();
        check(sf.getConfig().getInt("beacon-range-extender.range") == 1000, "range upper bound enforced");
        if (!ServerPlatform.isFolia()) check(!disk().getBoolean("gravity-duplication.enable") && !disk().getBoolean("pearl-stasis.enable"), "reload enforces Folia-only guards");
        edit(Map.of("beacon-range-extender.range", -5, "gravity-duplication.enable", false, "pearl-stasis.enable", false)); sf.reloadSettings();
        check(sf.getConfig().getInt("beacon-range-extender.range") == 50, "range lower bound enforced");
        // Explicit split keys must win over a leftover legacy switch.
        edit(Map.of("block-transformation.enable", true, "cauldron-concrete.enable", false, "oven-glass.enable", true)); sf.reloadSettings();
        check(!sf.getConfig().getBoolean("cauldron-concrete.enable") && sf.getConfig().getBoolean("oven-glass.enable"), "explicit split settings preserved during migration");
        long[] now = {1000}; TpaRequests state = new TpaRequests(() -> now[0], 10000, 3600000);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(); state.send(a, b); now[0] += 2000; state.reconfigure(1000, 0);
        check(state.remainingCooldownMillis(a) == 0 && state.isPending(a, b), "TPA reload removes cooldown without dropping pending request");
    }

    private void menuChecks() throws Exception {
        MenuSF menu = new MenuSF(); FakePlayer viewer = new FakePlayer(false);
        menu.onCommand(viewer.player, sf.getCommand("menusf"), "menusf", new String[0]);
        check(viewer.inventory.getItem(37).getType() == Material.CAULDRON, "cauldron menu item");
        check(viewer.inventory.getItem(38).getType() == Material.FURNACE, "oven menu item");
        check(viewer.inventory.getItem(42).getType() == Material.BEACON, "beacon menu item");
        for (int slot : new int[]{37, 38, 42}) {
            String path = MenuSF.MenuItems.values()[slot == 37 ? 12 : slot == 38 ? 13 : 14].getPath();
            boolean before = sf.getConfig().getBoolean(path);
            InventoryClickEvent event = new InventoryClickEvent(viewer.view, InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
            menu.onInventoryClick(event); check(event.isCancelled() && sf.getConfig().getBoolean(path) == before, "read-only click cannot toggle at " + slot);
        }
        setting("cauldron-concrete.enable", true); setting("oven-glass.enable", true);
        new MenuClickManager(37).updateConfig();
        check(!sf.getConfig().getBoolean("cauldron-concrete.enable") && sf.getConfig().getBoolean("oven-glass.enable"), "cauldron toggle independent");
        new MenuClickManager(38).updateConfig(); new MenuClickManager(37).updateConfig();
        check(sf.getConfig().getBoolean("cauldron-concrete.enable") && !sf.getConfig().getBoolean("oven-glass.enable"), "oven toggle independent");
        setting("beacon-range-extender.enable", false); new MenuClickManager(42).updateConfig();
        check(sf.getConfig().getBoolean("beacon-range-extender.enable"), "beacon menu toggle");
        check(disk().getBoolean("beacon-range-extender.enable"), "menu setting persisted");
    }

    private void cauldronChecks() throws Exception {
        for (int i = 0; i < powders.size(); i++) {
            DyeColor color = DyeColor.values()[i]; ItemStack stack = powders.get(i).getItemStack();
            check(stack.getType() == Material.valueOf(color + "_CONCRETE") && stack.getAmount() == 7, "native cauldron stack " + color);
            check(((Levelled) world.getBlockAt(1 + (i % 4) * 2, 79, 1 + (i / 4) * 2).getBlockData()).getLevel() == 3, "water preserved " + color);
        }
        setting("cauldron-concrete.enable", false);
        waiting = powder(13, 11, Material.BLUE_CONCRETE_POWDER, 31); independent = furnace(13, 11, Material.BLUE_CONCRETE_POWDER);
        later(240, this::ovenChecks);
    }

    private void ovenChecks() throws Exception {
        for (int i = 0; i < furnaces.size(); i++) {
            ItemStack output = furnaces.get(i).getInventory().getResult();
            check(output != null && output.getType() == Material.valueOf(DyeColor.values()[i] + "_STAINED_GLASS") && output.getAmount() == 1, "native glass smelting " + DyeColor.values()[i]);
        }
        check(waiting.getItemStack().getType() == Material.BLUE_CONCRETE_POWDER, "cauldron disabled while oven active");
        check(independent.getInventory().getResult() != null, "oven works with cauldron disabled");
        setting("cauldron-concrete.enable", true); setting("oven-glass.enable", false);
        ovenOffPowder = powder(13, 13, Material.GREEN_CONCRETE_POWDER, 17); disabledFurnace = furnace(13, 13, Material.GREEN_CONCRETE_POWDER);
        later(240, this::splitChecks);
    }

    private void applyAt(double x, double y, double z) throws Exception {
        recipient.location = new Location(world, x, y, z); recipient.effects.clear();
        Method apply = BeaconRangeExtender.class.getDeclaredMethod("apply", Player.class, int.class); apply.setAccessible(true);
        apply.invoke(extender, recipient.player, sf.getConfig().getInt("beacon-range-extender.range"));
    }
    private void splitChecks() throws Exception {
        check(waiting.getItemStack().getType() == Material.BLUE_CONCRETE, "live re-enable converts waiting powder");
        check(ovenOffPowder.getItemStack().getType() == Material.GREEN_CONCRETE, "cauldron works with oven disabled");
        check(disabledFurnace.getInventory().getResult() == null && disabledFurnace.getInventory().getFuel().getAmount() == 1, "disabled oven preserves input and fuel");
        Beacon beacon = (Beacon) world.getBlockAt(8, 85, 8).getState(); check(beacon.getTier() == 4, "native four-tier beacon activates");
        applyAt(88.5, 85, 8.5);
        check(recipient.effects.containsKey(PotionEffectType.SPEED) && recipient.effects.get(PotionEffectType.SPEED).getAmplifier() == 1, "beacon extends Speed II beyond vanilla range");
        applyAt(109.5, 85, 8.5); check(recipient.effects.isEmpty(), "beacon excludes out-of-range player");
        applyAt(88.5, -16, 8.5); check(recipient.effects.isEmpty(), "beacon lower vertical bound");
        applyAt(-91.5, 180, -91.5); check(!recipient.effects.isEmpty(), "beacon square range and upward extension");
        block(8, 86, 8, Material.STONE);
        later(170, () -> {
            applyAt(88.5, 85, 8.5); check(recipient.effects.isEmpty(), "opaque block disables beacon extension");
            block(8, 86, 8, Material.TINTED_GLASS);
            later(170, () -> {
                applyAt(88.5, 85, 8.5); check(recipient.effects.isEmpty(), "tinted glass blocks extension");
                Block slab = block(8, 86, 8, Material.OAK_SLAB);
                Slab data = (Slab) slab.getBlockData(); data.setType(Slab.Type.DOUBLE); slab.setBlockData(data, false);
                later(170, () -> {
                    applyAt(88.5, 85, 8.5); check(recipient.effects.isEmpty(), "double slab blocks extension");
                    Slab half = (Slab) slab.getBlockData(); half.setType(Slab.Type.BOTTOM); slab.setBlockData(half, false);
                    later(170, () -> {
                        applyAt(88.5, 85, 8.5); check(!recipient.effects.isEmpty(), "single slab permits beacon beam");
                        block(8, 86, 8, Material.BEDROCK);
                        Beacon secondaryBeacon = (Beacon) world.getBlockAt(8, 85, 8).getState();
                        secondaryBeacon.setSecondaryEffect(PotionEffectType.REGENERATION); secondaryBeacon.update();
                        later(170, () -> {
                            applyAt(88.5, 85, 8.5);
                            check(recipient.effects.containsKey(PotionEffectType.REGENERATION), "selected secondary effect extends too");
                            block(8, 84, 8, Material.AIR);
                            later(170, () -> {
                                applyAt(88.5, 85, 8.5); check(recipient.effects.isEmpty(), "broken pyramid stops extension");
                                block(8, 84, 8, Material.IRON_BLOCK); later(170, this::beaconReloadChecks);
                            });
                        });
                    });
                });
            });
        });
    }

    private void beaconReloadChecks() throws Exception {
        applyAt(88.5, 85, 8.5); check(!recipient.effects.isEmpty(), "bedrock permits beacon beam");
        edit(Map.of("enabled", true, "cauldron-concrete.enable", true, "oven-glass.enable", true, "beacon-range-extender.enable", true, "beacon-range-extender.range", 60));
        sf.getCommand("menusf").execute(Bukkit.getConsoleSender(), "menusf", new String[]{"reload"});
        applyAt(88.5, 85, 8.5); check(recipient.effects.isEmpty(), "hot reload shrinks extension without restart");
        later(240, () -> {
            check(disabledFurnace.getInventory().getResult() != null, "hot reload starts previously blocked furnace");
            edit(Map.of("beacon-range-extender.range", 100, "beacon-range-extender.enable", false)); sf.reloadSettings();
            // Run the actual player watcher, not just the effect-lookup helper.
            recipient.location = new Location(world, 88.5, 85, 8.5); recipient.effects.clear();
            Method watch = BeaconRangeExtender.class.getDeclaredMethod("watch", Player.class); watch.setAccessible(true); watch.invoke(extender, recipient.player);
            later(100, () -> {
                check(recipient.effects.isEmpty(), "disabled beacon player watcher grants no effects");
                edit(Map.of("beacon-range-extender.enable", true)); sf.reloadSettings();
                later(180, () -> {
                    check(!recipient.effects.isEmpty(), "re-enabled beacon player watcher grants effects");
                    recipient.effects.clear(); setting("enabled", false);
                    later(100, () -> {
                        check(recipient.effects.isEmpty(), "main plugin switch gates beacon effects"); setting("enabled", true);
                        setting("beacon-range-extender.range", 600);
                        Location far = new Location(world, 520.5, 85, 8.5);
                        tasks.at(far, () -> {
                            world.addPluginChunkTicket(32, 0, this);
                            recipient.location = far; recipient.effects.clear();
                        }).thenRun(() -> later(180, this::finishBeaconChecks));
                    });
                });
            });
        });
    }

    private void finishBeaconChecks() throws Exception {
        check(!recipient.effects.isEmpty(), "beacon snapshots reach player watcher in a distant region");
        extender.onUnload(new ChunkUnloadEvent(world.getChunkAt(0, 0), true));
        applyAt(88.5, 85, 8.5); check(recipient.effects.isEmpty(), "unload removes cached beacon effects");
        check(world.getPluginChunkTickets(0, 0).stream().allMatch(p -> p == this), "beacon extender adds no chunk tickets");
        globalLater(1, () -> guarded(() -> {
            check(!world.isChunkForceLoaded(0, 0), "beacon extender never force-loads chunks");
            finish();
        }));
    }

    private void finish() {
        getLogger().info("VERIFICATION RESULT: " + passed + " passed, " + failed + " failed on " + Bukkit.getVersion());
        globalLater(5, Bukkit::shutdown);
    }

    private final class FakePlayer {
        private final UUID id = UUID.randomUUID();
        private final boolean op;
        private volatile Location location = anchor;
        private Inventory inventory = Bukkit.createInventory(null, 9);
        private InventoryView view;
        private final List<String> messages = new ArrayList<>();
        private final Map<PotionEffectType, PotionEffect> effects = new java.util.concurrent.ConcurrentHashMap<>();
        private final Player player;
        private FakePlayer(boolean op) throws Exception {
            this.op = op;
            player = (Player) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
                case "isOp", "hasPermission" -> this.op;
                case "getUniqueId" -> id;
                case "getName", "toString" -> "ReleaseVerifier";
                case "getLocation" -> location.clone();
                case "getWorld" -> location.getWorld();
                case "isOnline" -> true;
                case "getGameMode" -> GameMode.SURVIVAL;
                case "getScheduler" -> Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{method.getReturnType()}, (p, m, a) -> {
                    if (m.getName().equals("execute")) { tasks.later(location, (Runnable) a[1], (long) a[3]); return true; }
                    return defaultValue(m.getReturnType());
                });
                case "sendMessage" -> { messages.add(String.valueOf(args[0])); yield null; }
                case "addPotionEffect" -> { PotionEffect effect = (PotionEffect) args[0]; effects.put(effect.getType(), effect); yield true; }
                case "openInventory" -> { inventory = (Inventory) args[0]; view = makeView((Player) proxy, inventory); yield view; }
                case "getOpenInventory" -> view;
                case "hashCode" -> id.hashCode();
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            });
            view = makeView(player, inventory);
        }
    }
    private InventoryView makeView(Player player, Inventory inventory) throws Exception {
        if (!InventoryView.class.isInterface()) return (InventoryView) Class.forName("verification.LegacyView")
                .getConstructor(Player.class, Inventory.class).newInstance(player, inventory);
        return (InventoryView) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{InventoryView.class}, (p, m, a) -> switch (m.getName()) {
            case "getTopInventory", "getInventory" -> inventory;
            case "getPlayer" -> player;
            case "getTitle", "getOriginalTitle" -> "MenuSF";
            case "getType" -> InventoryType.CHEST;
            case "getItem" -> (int) a[0] >= 0 && (int) a[0] < inventory.getSize() ? inventory.getItem((int) a[0]) : null;
            case "countSlots" -> 90;
            case "convertSlot" -> a[0];
            default -> defaultValue(m.getReturnType());
        });
    }
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false; if (type == int.class) return 0; if (type == long.class) return 0L;
        if (type == double.class) return 0D; if (type == float.class) return 0F; if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0; if (type == char.class) return (char) 0; return null;
    }
}
