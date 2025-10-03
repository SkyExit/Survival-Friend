package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Enderchest implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!PermissionManager.checkPermission(sender, MenuSF.MenuItems.ENDER_CHEST)) { return true; }

            Player player = (Player) sender;
                SkyLogger.sendPlayer(player, "Opening endechest...", SkyLogger.LogType.INFO);
                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
                player.openInventory(player.getEnderChest());
        return true;
    }
}
