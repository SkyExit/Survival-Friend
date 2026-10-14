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
        boolean promptPlayers = false;
        synchronized (config) {
            if (position == 49) {
                config.set("enabled", !config.getBoolean("enabled"));
                SF.getPlugin().saveConfig();
                promptPlayers = config.getBoolean("enabled");
            } else {
                for (MenuSF.MenuItems element : MenuSF.MenuItems.values()) {
                    if (element.getPosition() != position) continue;
                    String path = element.getPath();
                    switch (String.valueOf(config.get(path))) {
                        case "true" -> config.set(path, false);
                        case "false" -> config.set(path, true);
                        case "1" -> config.set(path, 2);
                        case "2" -> config.set(path, 3);
                        case "3" -> config.set(path, 1);
                    }
                    SF.getPlugin().saveConfig();
                    return;
                }
            }
        }
        if (promptPlayers) org.bukkit.Bukkit.getOnlinePlayers().forEach(player ->
                SF.getPlugin().getTasks().execute(player, () -> SF.getPlugin().getRules().prompt(player)));
    }
}
