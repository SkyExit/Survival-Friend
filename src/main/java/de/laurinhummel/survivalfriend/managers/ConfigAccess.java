package de.laurinhummel.survivalfriend.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Function;

/** One stable lock also protects readers while hot reload replaces the configuration. */
public final class ConfigAccess {
    public static final Object LOCK = new Object();
    private ConfigAccess() { }

    public static <T> T read(JavaPlugin plugin, Function<FileConfiguration, T> reader) {
        synchronized (LOCK) { return reader.apply(plugin.getConfig()); }
    }

    public static boolean enabled(JavaPlugin plugin, String path) {
        return read(plugin, config -> config.getBoolean(path));
    }

    public static int integer(JavaPlugin plugin, String path) {
        return read(plugin, config -> config.getInt(path));
    }
}
