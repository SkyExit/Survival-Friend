package de.laurinhummel.survivalfriend.managers;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import org.bukkit.configuration.file.FileConfiguration;

public class MenuClickManager {
    private final int position;

    public MenuClickManager(int position) {
        this.position = position;
    }

    public void updateConfig() {
        FileConfiguration config = SF.getPlugin().getConfig();
        boolean refreshRules = false;
        boolean refreshStasis = false;
        synchronized (config) {
            if (position == 49) {
                config.set("enabled", !config.getBoolean("enabled"));
                SF.getPlugin().saveConfig();
                refreshRules = true;
                refreshStasis = true;
            } else {
                for (MenuSF.MenuItems element : MenuSF.MenuItems.values()) {
                    if (element.getPosition() != position) continue;
                    if (!element.isAvailable()) return;
                    String path = element.getPath();
                    switch (String.valueOf(config.get(path))) {
                        case "true" -> config.set(path, false);
                        case "false" -> config.set(path, true);
                        case "1" -> config.set(path, 2);
                        case "2" -> config.set(path, 3);
                        case "3" -> config.set(path, 1);
                    }
                    SF.getPlugin().saveConfig();
                    refreshRules = element == MenuSF.MenuItems.RULES;
                    refreshStasis = element == MenuSF.MenuItems.PEARL_STASIS;
                    break;
                }
            }
        }
        if (refreshStasis && SF.getPlugin().getPearlStasis() != null) SF.getPlugin().getPearlStasis().refresh();
        if (refreshRules) org.bukkit.Bukkit.getOnlinePlayers().forEach(player ->
                SF.getPlugin().getTasks().execute(player, () -> SF.getPlugin().getRules().refresh(player)));
    }
}
