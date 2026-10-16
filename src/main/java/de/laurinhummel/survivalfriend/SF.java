package de.laurinhummel.survivalfriend;

import de.laurinhummel.survivalfriend.commands.*;
import de.laurinhummel.survivalfriend.events.*;
import de.laurinhummel.survivalfriend.compat.ServerPlatform;
import de.laurinhummel.survivalfriend.scheduling.ServerTasks;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SF extends JavaPlugin {
    private static SF plugin;
    private Rules rules;
    private ServerTasks tasks;
    private PearlStasis pearlStasis;
    private BlockTransformation blockTransformation;

    @Override
    public void onLoad() { plugin = this; }

    @Override
    public void onEnable() {
        tasks = new ServerTasks(this);
        saveDefaultConfig();
        // Preserve rules from configurations written before the command was renamed.
        if (getConfig().isList("regeln")) {
            if (!getConfig().contains("rules.entries", true)) {
                getConfig().set("rules.entries", getConfig().getStringList("regeln"));
            }
            getConfig().set("regeln", null);
        }
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
        pluginManager.registerEvents(new SoloPortalFarm(this), this);
        blockTransformation = new BlockTransformation(this);
        pluginManager.registerEvents(blockTransformation, this);
        blockTransformation.start();
        if (ServerPlatform.isFolia()) {
            pluginManager.registerEvents(new GravityDuplication(this), this);
            pearlStasis = new PearlStasis(this);
            pluginManager.registerEvents(pearlStasis, this);
            pearlStasis.refresh();
        }
        pluginManager.registerEvents(new HarderDragon(), this);
        pluginManager.registerEvents(new TotemPop(), this);
        pluginManager.registerEvents(rules, this);
        rules.registerMountListener(pluginManager);
        pluginManager.registerEvents(tpa, this);

        getCommand("menusf").setExecutor(menu);
        getCommand("compass").setExecutor(compass);
        getCommand("compass").setTabCompleter(compass);
        getCommand("enderchest").setExecutor(new Enderchest());
        getCommand("workbench").setExecutor(new Workbench());
        getCommand("tpa").setExecutor(tpa);
        getCommand("tpa").setTabCompleter(tpa);
        getCommand("rules").setExecutor(rules);
        getCommand("rules").setTabCompleter((sender, command, alias, args) -> java.util.Collections.emptyList());
        Bukkit.getOnlinePlayers().forEach(player -> tasks.execute(player, () -> rules.prompt(player)));
    }

    @Override
    public void onDisable() {
        if (blockTransformation != null) blockTransformation.close();
        if (pearlStasis != null) pearlStasis.close();
        if (tasks != null) tasks.close();
    }

    public static SF getPlugin() { return plugin; }
    public Rules getRules() { return rules; }
    public ServerTasks getTasks() { return tasks; }
    public PearlStasis getPearlStasis() { return pearlStasis; }
}
