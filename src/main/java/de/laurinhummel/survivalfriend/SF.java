package de.laurinhummel.survivalfriend;

import de.laurinhummel.survivalfriend.commands.*;
import de.laurinhummel.survivalfriend.events.*;
import de.laurinhummel.survivalfriend.compat.ServerPlatform;
import de.laurinhummel.survivalfriend.managers.ConfigAccess;
import de.laurinhummel.survivalfriend.scheduling.ServerTasks;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class SF extends JavaPlugin {
    private static SF plugin;
    private Rules rules;
    private ServerTasks tasks;
    private PearlStasis pearlStasis;
    private BlockTransformation blockTransformation;
    private BeaconRangeExtender beaconRangeExtender;
    private SoloPortalFarm soloPortalFarm;
    private TPA tpa;
    private MenuSF menu;
    private volatile FileConfiguration liveConfig;

    @Override
    public void onLoad() { plugin = this; }

    @Override
    public void onEnable() {
        tasks = new ServerTasks(this);
        saveDefaultConfig();
        try {
            liveConfig = loadSettings();
        } catch (Exception error) {
            throw new IllegalStateException("Could not load config.yml", error);
        }
        saveConfig();

        rules = new Rules(this);
        menu = new MenuSF();
        tpa = new TPA(this);
        Compass compass = new Compass();
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(menu, this);
        pluginManager.registerEvents(new CancelCreeper(), this);
        pluginManager.registerEvents(new CancelCropTrampling(), this);
        soloPortalFarm = new SoloPortalFarm(this);
        pluginManager.registerEvents(soloPortalFarm, this);
        beaconRangeExtender = new BeaconRangeExtender(this);
        pluginManager.registerEvents(beaconRangeExtender, this);
        beaconRangeExtender.start();
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
        getCommand("menusf").setTabCompleter(menu);
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
    public FileConfiguration getConfig() { return liveConfig == null ? super.getConfig() : liveConfig; }

    /** Parse before publishing: malformed YAML leaves the running configuration intact. */
    private FileConfiguration loadSettings() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new File(getDataFolder(), "config.yml"));
        try (var resource = getResource("config.yml")) {
            if (resource == null) throw new IllegalStateException("Missing default config.yml");
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8)));
        }
        // Preserve rules from configurations written before the command was renamed.
        if (config.isList("regeln")) {
            if (!config.contains("rules.entries", true)) {
                config.set("rules.entries", config.getStringList("regeln"));
            }
            config.set("regeln", null);
        }
        if (config.contains("block-transformation.enable", true)) {
            boolean previous = config.getBoolean("block-transformation.enable");
            for (String path : new String[]{"cauldron-concrete.enable", "oven-glass.enable"}) {
                if (!config.contains(path, true)) config.set(path, previous);
            }
            config.set("block-transformation", null);
        }
        config.options().copyDefaults(true);
        int range = config.getInt("beacon-range-extender.range", 100);
        int clamped = Math.max(50, Math.min(1000, range));
        if (!config.isInt("beacon-range-extender.range") || range != clamped) {
            getLogger().warning("beacon-range-extender.range must be an integer from 50 to 1000; using " + clamped);
        }
        config.set("beacon-range-extender.range", clamped);
        // A configuration carried over from Folia must not retain enabled Folia fixes.
        for (MenuSF.MenuItems item : MenuSF.MenuItems.values()) {
            if (!item.isAvailable() && config.getBoolean(item.getPath())) {
                config.set(item.getPath(), false);
                getLogger().info("Disabled " + item.getPath() + ": this feature requires Folia.");
            }
        }
        return config;
    }

    public synchronized void reloadSettings() throws Exception {
        synchronized (ConfigAccess.LOCK) {
            liveConfig = loadSettings();
            saveConfig();
        }
        soloPortalFarm.reload();
        tpa.reload();
        rules.reload();
        if (pearlStasis != null) pearlStasis.reload();
        Bukkit.getOnlinePlayers().forEach(player -> tasks.later(player, () -> {
            rules.refresh(player);
            menu.refresh(player);
        }));
    }

    @Override
    public void onDisable() {
        if (beaconRangeExtender != null) beaconRangeExtender.close();
        if (blockTransformation != null) blockTransformation.close();
        if (pearlStasis != null) pearlStasis.close();
        if (tasks != null) tasks.close();
    }

    public static SF getPlugin() { return plugin; }
    public Rules getRules() { return rules; }
    public ServerTasks getTasks() { return tasks; }
    public PearlStasis getPearlStasis() { return pearlStasis; }
}
