package de.laurinhummel.survivalfriend.managers;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class PermissionManager {

    CommandSender sender;
    @Nullable
    MenuSF.MenuItems level;

    public PermissionManager(CommandSender sender, @Nullable MenuSF.MenuItems level) {
        this.sender = sender;
        this.level = level;
    }

    public Object check() {
        if(!(sender instanceof Player)) { return ChatColor.RED + "This command is for players only!"; }
        else if(level == null || getPerm(level) == 2) { if(!sender.isOp()) return ChatColor.RED + "Sorry, but you need admin privileges to use this feature!"; }
        else if(getPerm(level) == 3) { return ChatColor.RED + "Sorry, but this feature has been disabled!"; }

        return true;
    }

    private int getPerm(MenuSF.MenuItems item) {
        Object o = SF.getPlugin().getConfig().get(item.getPath());

        if(o instanceof Integer) return (int) o;
        if(o instanceof Boolean) return (boolean) o ? 1 : 3;
        return 3;
    }
}
