package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.McColors;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPA implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!PermissionManager.checkPermission(sender, MenuSF.MenuItems.TPA)) {return true; }

        Player player = (Player) sender;
        Player target;
        try {
            target = Bukkit.getPlayer(args[0]);
        } catch (Exception ex) {
            SkyLogger.sendPlayer(player, "Player " + args[0] + " is not online!", SkyLogger.LogType.INFO);
            return true;
        }

        if(args.length == 2 && args[1].equalsIgnoreCase("accept")) {
            if(SF.tpa.get(target) == player) {
                assert target != null;
                target.teleport(player.getLocation());
                SF.tpa.remove(target);
            } else {
                SkyLogger.sendPlayer(player, "Player " + args[0] + " hasn't sent a request!", SkyLogger.LogType.ERROR);
                return true;
            }
            return true;

        } else if(args.length == 1) {
            if(SF.tpa.containsKey(player)) { SF.tpa.remove(player); }
            SF.tpa.put(player, target);
            SkyLogger.sendPlayer(player, "A TPA request has been sent to " + target.getName() + "!", SkyLogger.LogType.INFO);
            SkyLogger.sendPlayer(target, "TPA request from " + player.getName() + "!", SkyLogger.LogType.INFO);
            TextComponent textComponent = new TextComponent(McColors.GREEN + "[ACCEPT]");
                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpa " + player.getName() + "accept"));
            target.spigot().sendMessage(textComponent);
        }

        player.openWorkbench(null, true);
        return true;
    }
}
