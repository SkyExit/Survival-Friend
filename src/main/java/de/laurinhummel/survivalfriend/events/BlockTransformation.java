package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockTransformation implements Listener, AutoCloseable {
    private record Transformation(Material concrete, Material glass) { }

    private static final Map<Material, Transformation> TRANSFORMATIONS = transformations();
    private final SF plugin;
    private final List<NamespacedKey> recipes = new ArrayList<>();
    private final Map<UUID, PowderWatcher> watched = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public BlockTransformation(SF plugin) { this.plugin = plugin; }

    private static Map<Material, Transformation> transformations() {
        Map<Material, Transformation> result = new EnumMap<>(Material.class);
        for (DyeColor color : DyeColor.values()) {
            result.put(Material.valueOf(color.name() + "_CONCRETE_POWDER"), new Transformation(
                    Material.valueOf(color.name() + "_CONCRETE"),
                    Material.valueOf(color.name() + "_STAINED_GLASS")));
        }
        return Map.copyOf(result);
    }

    public void start() {
        // Keep recipes registered: live switches only change event guards, never the
        // global recipe registry from a player's Folia region thread.
        TRANSFORMATIONS.forEach((powder, transformation) -> {
            NamespacedKey key = new NamespacedKey(plugin,
                    "block_transformation/" + powder.name().toLowerCase(Locale.ROOT));
            if (!Bukkit.addRecipe(new FurnaceRecipe(key, new ItemStack(transformation.glass()), powder, 0.1F, 200))) {
                throw new IllegalStateException("Cannot register Block Transformation recipe: " + key);
            }
            recipes.add(key);
        });
        // Startup snapshots only; all subsequent entity reads run on the item's scheduler.
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) watch(item);
        }
    }

    private boolean enabled() {
        return !closed && PermissionManager.checkEnabled(MenuSF.MenuItems.BLOCK_TRANSFORMATION);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (TRANSFORMATIONS.containsKey(event.getEntity().getItemStack().getType())) watch(event.getEntity());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Item item) watch(item);
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) watched.remove(entity.getUniqueId());
    }

    private void watch(Item item) {
        if (closed) return;
        PowderWatcher watcher = new PowderWatcher(item);
        if (watched.putIfAbsent(watcher.id, watcher) == null) watcher.schedule();
    }

    private final class PowderWatcher implements Runnable {
        private final Item item;
        private final UUID id;

        private PowderWatcher(Item item) {
            this.item = item;
            id = item.getUniqueId();
        }

        private void stop() { watched.remove(id, this); }

        private void schedule() {
            plugin.getTasks().later(item, this, this::stop, 1L);
        }

        @Override
        public void run() {
            if (closed || watched.get(id) != this) return;
            if (item.isDead()) { stop(); return; }
            // Asynchronous region/dimension transfers can temporarily invalidate an item.
            if (!item.isValid()) { schedule(); return; }
            ItemStack stack = item.getItemStack();
            Transformation transformation = TRANSFORMATIONS.get(stack.getType());
            if (transformation == null) { stop(); return; }
            if (enabled() && inCauldronWater(item)) {
                ItemStack concrete = stack.clone();
                concrete.setType(transformation.concrete());
                // Replace the same entity's whole stack; no extra drops or water changes.
                item.setItemStack(concrete);
                stop();
                return;
            }
            // Continue while disabled so items already in a cauldron react to a live switch.
            schedule();
        }
    }

    private boolean inCauldronWater(Item item) {
        Location location = item.getLocation();
        Block block = location.getBlock();
        if (block.getType() != Material.WATER_CAULDRON) return false;
        double x = location.getX() - block.getX();
        double z = location.getZ() - block.getZ();
        if (x < 0.125 || x > 0.875 || z < 0.125 || z > 0.875) return false;
        Levelled water = (Levelled) block.getBlockData();
        double surface = block.getY() + (6.0 + 3.0 * water.getLevel()) / 16.0;
        return location.getY() <= surface && item.getBoundingBox().getMaxY() > block.getY() + 0.25;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (!(event.getBlock().getState() instanceof Furnace furnace)) return;
        ItemStack input = furnace.getInventory().getSmelting();
        if (input != null && TRANSFORMATIONS.containsKey(input.getType())
                && (event.getBlock().getType() != Material.FURNACE || !enabled())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (TRANSFORMATIONS.containsKey(event.getSource().getType())
                && (event.getBlock().getType() != Material.FURNACE || !enabled())) {
            // Also stop a batch that was already cooking when an operator disabled it.
            event.setCancelled(true);
        }
    }

    @Override
    public void close() {
        closed = true;
        watched.clear();
        for (NamespacedKey key : recipes) Bukkit.removeRecipe(key);
        recipes.clear();
    }
}
