package de.laurinhummel.survivalfriend;

import de.laurinhummel.survivalfriend.commands.*;
import de.laurinhummel.survivalfriend.events.CancelCreeper;
import de.laurinhummel.survivalfriend.events.CancelCropTrampling;
import de.laurinhummel.survivalfriend.events.SoloPortalFarm;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;

public final class SF extends JavaPlugin {
    private static SF plugin;

    @Override
    public void onLoad() { plugin = this; }

    @Override
    public void onEnable() {
        PluginManager pluginManager = Bukkit.getPluginManager();
            pluginManager.registerEvents(new MenuSF(), this);
            pluginManager.registerEvents(new CancelCreeper(), this);
            pluginManager.registerEvents(new CancelCropTrampling(), this);
            pluginManager.registerEvents(new SoloPortalFarm(), this);

        getCommand("menusf").setExecutor(new MenuSF());
        getCommand("compass").setExecutor(new Compass());
        getCommand("compass").setTabCompleter(new Compass());
        getCommand("enderchest").setExecutor(new Enderchest());
        getCommand("workbench").setExecutor(new Workbench());
        getCommand("tpa").setExecutor(new TPA());

        FileConfiguration config = this.getConfig();
            config.addDefault("enabled", true);
            config.addDefault(MenuSF.MenuItems.CREEPER_EXPLOSIONS.getPath(), false);
            config.addDefault(MenuSF.MenuItems.CREEPER_DAMAGE.getPath(), true);
            config.addDefault(MenuSF.MenuItems.TRAMPLING_PLAYER.getPath(), false);
            config.addDefault(MenuSF.MenuItems.TRAMPLING_MOB.getPath(), false);
            config.addDefault(MenuSF.MenuItems.CRAFTING_TABLE.getPath(), 3);
            config.addDefault(MenuSF.MenuItems.PATHFINDER.getPath(), 1);
            config.addDefault(MenuSF.MenuItems.ENDER_CHEST.getPath(), 3);
            config.addDefault(MenuSF.MenuItems.SOLO_PORTAL_FARM.getPath(), false);
            config.addDefault(MenuSF.MenuItems.TPA.getPath(), true);
        config.options().copyDefaults(true);
        config.options().setHeader(Collections.singletonList("Usage: 1-Everyone  2-Admins  3-Disabled"));
        saveConfig();
    }

    @Override
    public void onDisable() {
    }

    public static SF getPlugin() { return plugin; }
    public static HashMap<Player, Player> tpa;
}
