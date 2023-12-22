package de.laurinhummel.survivalfriend.managers;

import de.laurinhummel.survivalfriend.commands.Menu;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class PermissionManager {

    CommandSender sender;
    @Nullable
    Menu.MenuItems level;

    public PermissionManager(CommandSender sender, @Nullable Menu.MenuItems level) {
        this.sender = sender;
        this.level = level;
    }

    public Object check() {
        if(!(sender instanceof Player)) { return ChatColor.RED + "This command is for players only!"; }
        else if(level == null || level.getPosition() == 2) { if(!sender.isOp()) return ChatColor.RED + "Sorry, but you need admin privileges to use this feature!"; }
        else if(level.getPosition() == 3) { return ChatColor.RED + "Sorry, but this feature has been disabled!"; }

        return true;
    }
}
