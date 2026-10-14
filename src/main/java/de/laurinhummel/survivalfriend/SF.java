package de.laurinhummel.survivalfriend;

import de.laurinhummel.survivalfriend.commands.*;
import de.laurinhummel.survivalfriend.events.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SF extends JavaPlugin {
    private static SF plugin;
    private Rules rules;

    @Override
    public void onLoad() { plugin = this; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        rules = new Rules(this);
        MenuSF menu = new MenuSF();
        TPA tpa = new TPA(this);
        Compass compass = new Compass();
        PluginManager pluginManager = Bukkit.getPluginManager();
            pluginManager.registerEvents(menu, this);
            pluginManager.registerEvents(new CancelCreeper(), this);
            pluginManager.registerEvents(new CancelCropTrampling(), this);
            pluginManager.registerEvents(new SoloPortalFarm(), this);
            pluginManager.registerEvents(new HarderDragon(), this);
            pluginManager.registerEvents(new TotemPop(), this);
            pluginManager.registerEvents(rules, this);
            pluginManager.registerEvents(tpa, this);

        getCommand("menusf").setExecutor(menu);
        getCommand("compass").setExecutor(compass);
        getCommand("compass").setTabCompleter(compass);
        getCommand("enderchest").setExecutor(new Enderchest());
        getCommand("workbench").setExecutor(new Workbench());
        getCommand("tpa").setExecutor(tpa);
        getCommand("tpa").setTabCompleter(tpa);
        getCommand("regeln").setExecutor(rules);
        getCommand("regeln").setTabCompleter((sender, command, alias, args) -> java.util.Collections.emptyList());
        Bukkit.getOnlinePlayers().forEach(rules::prompt);
    }

    @Override
    public void onDisable() {
    }

    public static SF getPlugin() { return plugin; }
    public Rules getRules() { return rules; }
}
