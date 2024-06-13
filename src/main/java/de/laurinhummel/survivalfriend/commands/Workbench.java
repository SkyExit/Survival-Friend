package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Workbench implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PermissionManager pm = new PermissionManager(sender, Menu.MenuItems.CRAFTING_TABLE);
        if(!(pm.check() instanceof Boolean)) { sender.sendMessage((String) pm.check()); return true; }

            Player player = (Player) sender;
                SkyLogger.sendPlayer(player, "Opening workbench...", SkyLogger.LogType.INFO);
                player.openWorkbench(null, true);
        return true;
    }
}