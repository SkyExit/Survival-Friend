package de.laurinhummel.survivalfriend.managers;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PermissionManager {

    /**
     * Checks whether a given feature is enabled or not.
     * @param item MenuSF.MenuItems to check for
     * @return boolean - Whether the feature is enabled or not
     */
    public static boolean checkEnabled(MenuSF.MenuItems item) {
        return ConfigAccess.read(SF.getPlugin(), config -> config.getBoolean("enabled") && config.getBoolean(item.getPath()));
    }

    /**
     * @param sender Player object to check for access level
     * @param feature Feature to check access level against
     * @return Whether the user has access to the feature
     */
    public static boolean checkPermission(CommandSender sender, MenuSF.MenuItems feature) {
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is for players only!");
            return false;
        }
        if (feature != null && !ConfigAccess.enabled(SF.getPlugin(), "enabled")) {
            sender.sendMessage(ChatColor.RED + "Sorry, but this plugin has been disabled!");
            return false;
        }
        if(feature == null || getPerm(feature) == 2) {
            if(!sender.isOp()) { sender.sendMessage(ChatColor.RED + "Sorry, but you need admin privileges to use this feature!");
                return false;
            }
        }
        else if(getPerm(feature) != 1) { sender.sendMessage(ChatColor.RED + "Sorry, but this feature has been disabled!");
            return false;
        }
        return true;
    }

    /**
     * Returns the config-value of a given string
     * @param item the item
     * @return access level of the requested function
     */
    private static int getPerm(MenuSF.MenuItems item) {
        Object o = ConfigAccess.read(SF.getPlugin(), config -> config.get(item.getPath()));

        if(o instanceof Integer) return (int) o;
        if(o instanceof Boolean) return (boolean) o ? 1 : 3;
        return 3;
    }
}
