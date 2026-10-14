package de.laurinhummel.survivalfriend.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Function;

/** YAML configuration is not thread-safe; menu writes and region reads share this lock. */
public final class ConfigAccess {
    private ConfigAccess() { }

    public static <T> T read(JavaPlugin plugin, Function<FileConfiguration, T> reader) {
        FileConfiguration config = plugin.getConfig();
        synchronized (config) { return reader.apply(config); }
    }

    public static boolean enabled(JavaPlugin plugin, String path) {
        return read(plugin, config -> config.getBoolean(path));
    }

    public static int integer(JavaPlugin plugin, String path) {
        return read(plugin, config -> config.getInt(path));
    }
}
