package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Workbench implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!PermissionManager.checkPermission(sender, MenuSF.MenuItems.CRAFTING_TABLE)) { return true; }

            Player player = (Player) sender;
                SkyLogger.sendPlayer(player, "Opening workbench...", SkyLogger.LogType.INFO);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_FLETCHER, 1, 1);
                player.openWorkbench(null, true);
        return true;
    }
}