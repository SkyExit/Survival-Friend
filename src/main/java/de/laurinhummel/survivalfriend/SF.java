package de.laurinhummel.survivalfriend;

import de.laurinhummel.survivalfriend.commands.Compass;
import de.laurinhummel.survivalfriend.commands.Enderchest;
import de.laurinhummel.survivalfriend.commands.Menu;
import de.laurinhummel.survivalfriend.commands.Workbench;
import de.laurinhummel.survivalfriend.events.CancelCreeper;
import de.laurinhummel.survivalfriend.events.CancelCropTrampling;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public final class SF extends JavaPlugin {
    private static SF plugin;

    @Override
    public void onLoad() { plugin = this; }

    @Override
    public void onEnable() {
        PluginManager pluginManager = Bukkit.getPluginManager();
            pluginManager.registerEvents(new Menu(), this);
            pluginManager.registerEvents(new CancelCreeper(), this);
            pluginManager.registerEvents(new CancelCropTrampling(), this);

        getCommand("menu").setExecutor(new Menu());
        getCommand("compass").setExecutor(new Compass());
            getCommand("compass").setTabCompleter(new Compass());
        getCommand("enderchest").setExecutor(new Enderchest());
        getCommand("workbench").setExecutor(new Workbench());

        FileConfiguration config = this.getConfig();
            config.addDefault("enabled", true);
            config.addDefault("creeper.explosion", false);
            config.addDefault("creeper.damage", true);
            config.addDefault("trampling.player", false);
            config.addDefault("trampling.mob", false);
            config.addDefault("utils.wb", 3);
            config.addDefault("utils.compass", 1);
            config.addDefault("utils.ec", 3);
            config.addDefault("utils.home", 3);
            config.addDefault("utils.spawn", 3);
        config.options().copyDefaults(true);
        config.options().setHeader(Collections.singletonList("Usage: 1-Everyone  2-Admins  3-Disabled"));
        saveConfig();
    }

    @Override
    public void onDisable() {
    }

    public static SF getPlugin() { return plugin; }
}
