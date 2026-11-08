package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.managers.ConfigAccess;
import org.bukkit.*;
import org.bukkit.block.Beacon;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Bukkit-only effects work on Spigot too, without changing persistent beacon data. */
public final class BeaconRangeExtender implements Listener, AutoCloseable {
    private static final long INTERVAL = 80L;
    private static final long MAX_AGE_NANOS = 12_000_000_000L;
    private record BlockKey(UUID world, int x, int y, int z) {
        Cell cell() { return new Cell(world, x >> 7, z >> 7); }
    }
    // 128-block cells keep player lookups local even on worlds with many beacons.
    private record Cell(UUID world, int x, int z) { }
    private record Effects(int tier, PotionEffect primary, PotionEffect secondary, long refreshed) { }
    private record Settings(boolean enabled, int range) { }

    private final SF plugin;
    private final Map<BlockKey, BeaconWatch> beacons = new ConcurrentHashMap<>();
    private final Map<Cell, Map<BlockKey, Effects>> cells = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerWatch> players = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public BeaconRangeExtender(SF plugin) { this.plugin = plugin; }

    private Settings settings() {
        return ConfigAccess.read(plugin, config -> new Settings(!closed && config.getBoolean("enabled")
                && config.getBoolean("beacon-range-extender.enable"),
                Math.max(50, Math.min(1000, config.getInt("beacon-range-extender.range", 100)))));
    }

    public void start() {
        for (World world : Bukkit.getWorlds()) scanLoaded(world);
        Bukkit.getOnlinePlayers().forEach(this::watch);
    }

    private void scanLoaded(World world) {
        for (Chunk chunk : world.getLoadedChunks()) {
            int x = chunk.getX(), z = chunk.getZ();
            plugin.getTasks().later(new Location(world, x * 16.0, 0, z * 16.0), () -> {
                if (!closed && world.isChunkLoaded(x, z)) scan(world.getChunkAt(x, z));
            }, 1L);
        }
    }

    private void scan(Chunk chunk) {
        if (closed) return;
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Beacon) watch(state.getLocation());
        }
    }

    private void watch(Location location) {
        if (closed) return;
        BlockKey key = new BlockKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BeaconWatch watcher = new BeaconWatch(key, location.clone());
        if (beacons.putIfAbsent(key, watcher) == null) watcher.schedule(1L);
    }

    private void publish(BlockKey key, Effects effects) {
        cells.compute(key.cell(), (cell, previous) -> {
            Map<BlockKey, Effects> next = previous == null ? new HashMap<>() : new HashMap<>(previous);
            if (effects == null) next.remove(key);
            else next.put(key, effects);
            return next.isEmpty() ? null : Map.copyOf(next);
        });
    }

    private void remove(BlockKey key, BeaconWatch watcher) {
        if (beacons.remove(key, watcher)) publish(key, null);
    }

    private final class BeaconWatch implements Runnable {
        private final BlockKey key;
        private final Location location;
        private BeaconWatch(BlockKey key, Location location) { this.key = key; this.location = location; }
        private void schedule(long delay) { plugin.getTasks().later(location, this, delay); }

        @Override public void run() {
            if (closed || beacons.get(key) != this) return;
            World world = location.getWorld();
            if (!world.isChunkLoaded(key.x >> 4, key.z >> 4)) { remove(key, this); return; }
            if (!settings().enabled) { publish(key, null); schedule(INTERVAL); return; }
            if (!(location.getBlock().getState() instanceof Beacon beacon)) { remove(key, this); return; }
            int tier = beacon.getTier();
            PotionEffect primary = beacon.getPrimaryEffect();
            if (tier > 0 && primary != null && clearBeam(world, key)) {
                publish(key, new Effects(tier, primary, tier >= 4 ? beacon.getSecondaryEffect() : null, System.nanoTime()));
            } else publish(key, null);
            schedule(INTERVAL);
        }
    }

    private static boolean clearBeam(World world, BlockKey key) {
        // Only the beacon's own column is read, so this never crosses a chunk/region.
        for (int y = key.y + 1; y < world.getMaxHeight(); y++) {
            var block = world.getBlockAt(key.x, y, key.z);
            Material type = block.getType();
            if (type.isAir() || type == Material.BEDROCK) continue;
            if (type == Material.TINTED_GLASS || (type.isOccluding() && type != Material.BEDROCK)) return false;
            // These materials become opaque only in their full-block state.
            var data = block.getBlockData();
            if (data instanceof Slab slab && slab.getType() == Slab.Type.DOUBLE) return false;
            if (data instanceof Snow snow && snow.getLayers() == snow.getMaximumLayers()) return false;
        }
        return true;
    }

    private void watch(Player player) {
        if (closed) return;
        PlayerWatch watcher = new PlayerWatch(player);
        if (players.putIfAbsent(player.getUniqueId(), watcher) == null) watcher.schedule();
    }

    private final class PlayerWatch implements Runnable {
        private final Player player;
        private final UUID id;
        private PlayerWatch(Player player) { this.player = player; id = player.getUniqueId(); }
        private void schedule() {
            plugin.getTasks().later(player, this, () -> players.remove(id, this), INTERVAL);
        }

        @Override public void run() {
            if (closed || players.get(id) != this) return;
            if (!player.isOnline()) { players.remove(id, this); return; }
            Settings settings = settings();
            if (settings.enabled && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR) {
                apply(player, settings.range);
            }
            schedule();
        }
    }

    private void apply(Player player, int range) {
        Location at = player.getLocation();
        UUID world = at.getWorld().getUID();
        Map<PotionEffectType, PotionEffect> strongest = new HashMap<>();
        long now = System.nanoTime();
        for (int x = (at.getBlockX() - range - 1) >> 7; x <= (at.getBlockX() + range) >> 7; x++) {
            for (int z = (at.getBlockZ() - range - 1) >> 7; z <= (at.getBlockZ() + range) >> 7; z++) {
                Map<BlockKey, Effects> candidates = cells.get(new Cell(world, x, z));
                if (candidates == null) continue;
                candidates.forEach((key, effects) -> {
                    if (now - effects.refreshed > MAX_AGE_NANOS || !inRange(at, key, range)) return;
                    collect(strongest, effects.primary, effects.tier);
                    collect(strongest, effects.secondary, effects.tier);
                });
            }
        }
        strongest.values().forEach(player::addPotionEffect);
    }

    private static boolean inRange(Location at, BlockKey key, int range) {
        // Match the vanilla beacon's block-sized box: square X/Z and unbounded upward.
        return at.getX() >= key.x - range && at.getX() < key.x + 1.0 + range
                && at.getZ() >= key.z - range && at.getZ() < key.z + 1.0 + range
                && at.getY() >= key.y - range;
    }

    private static void collect(Map<PotionEffectType, PotionEffect> result, PotionEffect source, int tier) {
        if (source == null) return;
        PotionEffect next = new PotionEffect(source.getType(), (9 + tier * 2) * 20, source.getAmplifier(), true, true);
        result.merge(next.getType(), next, (a, b) -> a.getAmplifier() > b.getAmplifier()
                || (a.getAmplifier() == b.getAmplifier() && a.getDuration() >= b.getDuration()) ? a : b);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) { if (event.getBlock().getType() == Material.BEACON) watch(event.getBlock().getLocation()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        var block = event.getBlock();
        BlockKey key = new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        BeaconWatch watcher = beacons.get(key);
        if (watcher != null) remove(key, watcher);
    }

    @EventHandler public void onLoad(ChunkLoadEvent event) { scan(event.getChunk()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnload(ChunkUnloadEvent event) {
        UUID world = event.getWorld().getUID();
        int x = event.getChunk().getX(), z = event.getChunk().getZ();
        beacons.forEach((key, watcher) -> {
            if (key.world.equals(world) && key.x >> 4 == x && key.z >> 4 == z) remove(key, watcher);
        });
    }

    @EventHandler public void onWorldLoad(WorldLoadEvent event) { scanLoaded(event.getWorld()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        UUID world = event.getWorld().getUID();
        beacons.forEach((key, watcher) -> { if (key.world.equals(world)) remove(key, watcher); });
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { watch(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { players.remove(event.getPlayer().getUniqueId()); }

    @Override public void close() { closed = true; beacons.clear(); cells.clear(); players.clear(); }
}
