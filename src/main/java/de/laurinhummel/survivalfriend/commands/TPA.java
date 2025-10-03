package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.McColors;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import de.laurinhummel.survivalfriend.misc.SkyStrings;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Timestamp;

public class TPA implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!PermissionManager.checkPermission(sender, MenuSF.MenuItems.TPA)) { return true; }

        int cooldown = 60;                  // Minutes
        Player player = (Player) sender;
        Player target;
        try {
            target = Bukkit.getPlayer(args[0]);
        } catch (Exception ex) {
            SkyLogger.sendPlayer(player, "Player is not online!", SkyLogger.LogType.INFO);
            return true;
        }

        if(args.length == 2 && args[1].equalsIgnoreCase("accept")) {
            if(SF.tpa.get(target) == player) {
                assert target != null;
                target.teleport(player.getLocation());

                Bukkit.getWorld(player.getWorld().getName()).spawnParticle(Particle.PORTAL, player.getLocation(), 20);
                Bukkit.getWorld(player.getWorld().getName()).playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);

                SkyLogger.sendPlayer(player, McColors.GOLD + target.getName() + McColors.AQUA + " has been teleported to you!", SkyLogger.LogType.INFO);
                SkyLogger.sendPlayer(target, "You have been teleported to " + McColors.GOLD + player.getName() + McColors.AQUA + "!", SkyLogger.LogType.INFO);

                SF.tpa.remove(target);
            } else {
                SkyLogger.sendPlayer(player, "Player " + args[0] + " hasn't sent a request!", SkyLogger.LogType.ERROR);
                return true;
            }
            return true;

        } else if(args.length == 1) {
            if(SF.tpaCooldown.containsKey(player)) {
                if(!(SF.tpaCooldown.get(player).getTime() + (cooldown*60*1000) <= System.currentTimeMillis())) {
                    SkyLogger.sendPlayer(player, "You have to wait " + Math.round((float) Math.abs(SF.tpaCooldown.get(player).getTime() + (cooldown*60*1000) - System.currentTimeMillis())/1000/60) + " more minutes to do that again!", SkyLogger.LogType.ERROR);
                    return true;
                }
            }
            SF.tpaCooldown.remove(player);

            if(SF.tpa.containsKey(player)) { SF.tpa.remove(player); }
            SF.tpa.put(player, target);
            SkyLogger.sendPlayer(player, "A TPA request has been sent to " + McColors.GOLD + target.getName() + McColors.AQUA + "!", SkyLogger.LogType.INFO);
            TextComponent textComponent = new TextComponent(SkyStrings.SF + McColors.AQUA + "TPA request from " + McColors.GOLD + player.getName() + McColors.AQUA + "!" + McColors.GREEN + "   [ACCEPT]");
                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpa " + player.getName() + " accept"));
            target.spigot().sendMessage(textComponent);

            SF.tpaCooldown.put(player, new Timestamp(System.currentTimeMillis()));
        }
        return true;
    }
}
