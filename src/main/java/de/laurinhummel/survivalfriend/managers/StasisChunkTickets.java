package de.laurinhummel.survivalfriend.managers;

import de.laurinhummel.survivalfriend.SF;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Overlapping chambers share one plugin ticket until the last chamber releases it. */
public final class StasisChunkTickets {
    private record ChunkKey(World world, int x, int z) {
        Location location() { return new Location(world, x * 16.0 + 8, world.getMinHeight(), z * 16.0 + 8); }
    }

    private final SF plugin;
    private final Map<UUID, List<ChunkKey>> areas = new HashMap<>();
    private final Map<ChunkKey, Integer> references = new HashMap<>();

    public StasisChunkTickets(SF plugin) { this.plugin = plugin; }

    public synchronized void acquire(UUID id, Location anchor, int radius) {
        if (areas.containsKey(id)) return;
        List<ChunkKey> area = new ArrayList<>();
        areas.put(id, area);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkKey key = new ChunkKey(anchor.getWorld(), (anchor.getBlockX() >> 4) + x, (anchor.getBlockZ() >> 4) + z);
                area.add(key);
                if (references.merge(key, 1, Integer::sum) == 1) reconcile(key);
            }
        }
    }

    public synchronized void release(UUID id) {
        List<ChunkKey> area = areas.remove(id);
        if (area == null) return;
        for (ChunkKey key : area) {
            int remaining = references.get(key) - 1;
            if (remaining > 0) references.put(key, remaining);
            else { references.remove(key); reconcile(key); }
        }
    }

    private void reconcile(ChunkKey key) {
        plugin.getTasks().at(key.location(), () -> {
            synchronized (this) {
                if (references.containsKey(key)) key.world.addPluginChunkTicket(key.x, key.z, plugin);
                else key.world.removePluginChunkTicket(key.x, key.z, plugin);
            }
        }).exceptionally(error -> {
            plugin.getLogger().warning("Could not update pearl stasis chunk ticket: " + error.getMessage());
            return null;
        });
    }

    public synchronized void clear() {
        // Removal does not load chunks and is also valid during plugin shutdown.
        for (ChunkKey key : references.keySet()) key.world.removePluginChunkTicket(key.x, key.z, plugin);
        references.clear();
        areas.clear();
    }
}
