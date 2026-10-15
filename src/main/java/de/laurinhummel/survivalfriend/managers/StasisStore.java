package de.laurinhummel.survivalfriend.managers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Only immutable coordinates and UUIDs are persisted; entities belong to their region. */
public final class StasisStore {
    public static final class Chamber {
        public final UUID id;
        public final UUID owner;
        public final String world;
        public final double x, y, z;
        public final int radius;
        public volatile boolean pending;

        public Chamber(UUID id, UUID owner, Location anchor, int radius) {
            this(id, owner, anchor.getWorld().getName(), anchor.getX(), anchor.getY(), anchor.getZ(), radius, false);
        }

        private Chamber(UUID id, UUID owner, String world, double x, double y, double z, int radius, boolean pending) {
            this.id = id;
            this.owner = owner;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.pending = pending;
        }

        public Location location(World loadedWorld) { return new Location(loadedWorld, x, y, z); }
    }

    private final JavaPlugin plugin;
    private final File file;

    public StasisStore(JavaPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "pearl-stasis.yml");
    }

    public List<Chamber> load() {
        List<Chamber> chambers = new ArrayList<>();
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection entries = data.getConfigurationSection("chambers");
        if (entries == null) return chambers;
        for (String key : entries.getKeys(false)) {
            try {
                ConfigurationSection entry = entries.getConfigurationSection(key);
                if (entry == null) throw new IllegalArgumentException("Missing chamber data");
                String world = entry.getString("world");
                double x = entry.getDouble("x", Double.NaN), y = entry.getDouble("y", Double.NaN), z = entry.getDouble("z", Double.NaN);
                if (world == null || world.isBlank() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                        || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000) {
                    throw new IllegalArgumentException("Invalid chamber coordinates");
                }
                chambers.add(new Chamber(UUID.fromString(key), UUID.fromString(entry.getString("owner", "")), world,
                        x, y, z, Math.max(0, Math.min(8, entry.getInt("chunk-radius", 1))), entry.getBoolean("pending")));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring invalid pearl stasis record " + key + ": " + e.getMessage());
            }
        }
        return chambers;
    }

    public synchronized boolean save(Collection<Chamber> chambers) {
        YamlConfiguration data = new YamlConfiguration();
        for (Chamber chamber : chambers) {
            String path = "chambers." + chamber.id + ".";
            data.set(path + "owner", chamber.owner.toString());
            data.set(path + "world", chamber.world);
            data.set(path + "x", chamber.x);
            data.set(path + "y", chamber.y);
            data.set(path + "z", chamber.z);
            data.set(path + "chunk-radius", chamber.radius);
            data.set(path + "pending", chamber.pending);
        }
        try {
            Files.createDirectories(file.toPath().getParent());
            var temporary = file.toPath().resolveSibling(file.getName() + ".tmp");
            Files.writeString(temporary, data.saveToString(), StandardCharsets.UTF_8);
            try { Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pearl stasis chambers: " + e.getMessage());
            return false;
        }
    }
}
