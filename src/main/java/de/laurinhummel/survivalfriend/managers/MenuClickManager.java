package de.laurinhummel.survivalfriend.managers;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class MenuClickManager {
    int position;

    public MenuClickManager(int position) {
        this.position = position;
    }

    public void updateConfig() {
        for(MenuSF.MenuItems element : MenuSF.MenuItems.values()) {
            if(element.getPosition() != position) continue;

            String path = element.getPath();
            FileConfiguration config = SF.getPlugin().getConfig();
            String object = Objects.requireNonNull(config.get(path)).toString();

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
