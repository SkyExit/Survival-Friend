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
        if (position == 49) {
            config.set("enabled", !config.getBoolean("enabled"));
            SF.getPlugin().saveConfig();
            if (config.getBoolean("enabled")) org.bukkit.Bukkit.getOnlinePlayers().forEach(SF.getPlugin().getRules()::prompt);
            return;
        }
        for(MenuSF.MenuItems element : MenuSF.MenuItems.values()) {
            if(element.getPosition() != position) continue;

            String path = element.getPath();
            String object = String.valueOf(config.get(path));

            switch(object) {
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
