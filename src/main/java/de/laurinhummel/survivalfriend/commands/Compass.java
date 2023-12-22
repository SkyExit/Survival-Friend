package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Compass implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        PermissionManager pm = new PermissionManager(sender, Menu.MenuItems.PATHFINDER);
            if(!(boolean) pm.check()) { sender.sendMessage((String) pm.check()); }


        return false;
    }
}
