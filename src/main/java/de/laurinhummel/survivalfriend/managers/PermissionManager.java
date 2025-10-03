package de.laurinhummel.survivalfriend.managers;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class PermissionManager {

    /**
     *
     * @param sender Player object to check for access level
     * @param feature Feature to check access level against
     * @return Whether the user has access to the feature
     */
    public static boolean checkPermission(CommandSender sender,  @Nullable MenuSF.MenuItems feature) {
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is for players only!");
            return false;
        }
        else if(feature == null || getPerm(feature) == 2) {
            if(!sender.isOp()) { sender.sendMessage(ChatColor.RED + "Sorry, but you need admin privileges to use this feature!");
                return false;
            }
        }
        else if(getPerm(feature) == 3) { sender.sendMessage(ChatColor.RED + "Sorry, but this feature has been disabled!");
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
        Object o = SF.getPlugin().getConfig().get(item.getPath());

        if(o instanceof Integer) return (int) o;
        if(o instanceof Boolean) return (boolean) o ? 1 : 3;
        return 3;
    }
}
