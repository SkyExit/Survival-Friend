package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.managers.TpaRequests;
import de.laurinhummel.survivalfriend.misc.McColors;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import de.laurinhummel.survivalfriend.misc.SkyStrings;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.List;

public class TPA implements CommandExecutor, TabCompleter, Listener {
    private final SF plugin;
    private final TpaRequests requests;

    public TPA(SF plugin) {
        this.plugin = plugin;
        requests = new TpaRequests(System::currentTimeMillis,
                Math.max(1, plugin.getConfig().getInt("tpa.request-timeout-seconds")) * 1000L,
                Math.max(0, plugin.getConfig().getInt("tpa.cooldown-seconds")) * 1000L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PermissionManager.checkPermission(sender, MenuSF.MenuItems.TPA)) return true;
        Player player = (Player) sender;
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !args[1].equalsIgnoreCase("accept"))) {
            SkyLogger.sendPlayer(player, "Usage: /tpa <player> [accept]", SkyLogger.LogType.ERROR);
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            SkyLogger.sendPlayer(player, "Player is not online!", SkyLogger.LogType.ERROR);
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            SkyLogger.sendPlayer(player, "You cannot send a TPA request to yourself.", SkyLogger.LogType.ERROR);
            return true;
        }
        if (plugin.getRules().requiresAcceptance(player) || plugin.getRules().requiresAcceptance(target)) {
            SkyLogger.sendPlayer(player, "Both players must accept /regeln before teleporting.", SkyLogger.LogType.ERROR);
            return true;
        }
        if (args.length == 2) {
            if (!requests.isPending(target.getUniqueId(), player.getUniqueId())) {
                SkyLogger.sendPlayer(player, "No pending request from " + target.getName() + " (it may have expired).", SkyLogger.LogType.ERROR);
                return true;
            }
            if (!target.teleport(player.getLocation())) {
                SkyLogger.sendPlayer(player, "The teleport was cancelled. The request is still pending.", SkyLogger.LogType.ERROR);
                return true;
            }
            requests.complete(target.getUniqueId());
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 20);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            SkyLogger.sendPlayer(player, McColors.GOLD + target.getName() + McColors.AQUA + " has been teleported to you!", SkyLogger.LogType.INFO);
            SkyLogger.sendPlayer(target, "You have been teleported to " + McColors.GOLD + player.getName() + McColors.AQUA + "!", SkyLogger.LogType.INFO);
            return true;
        }
        long remaining = requests.remainingCooldownMillis(player.getUniqueId());
        if (remaining > 0) {
            SkyLogger.sendPlayer(player, "You have to wait " + (long) Math.ceil(remaining / 1000.0)
                    + " more seconds to do that again!", SkyLogger.LogType.ERROR);
            return true;
        }
        if (!requests.send(player.getUniqueId(), target.getUniqueId())) return true;
        SkyLogger.sendPlayer(player, "A TPA request has been sent to " + McColors.GOLD + target.getName() + McColors.AQUA + "!", SkyLogger.LogType.INFO);
        TextComponent text = new TextComponent(SkyStrings.SF + McColors.AQUA + "TPA request from " + McColors.GOLD
                + player.getName() + McColors.AQUA + "!" + McColors.GREEN + "   [ACCEPT]");
        text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpa " + player.getName() + " accept"));
        target.spigot().sendMessage(text);
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { requests.disconnect(event.getPlayer().getUniqueId()); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(target -> !target.equals(sender) && (!(sender instanceof Player player) || player.canSee(target)))
                    .map(Player::getName).filter(name -> name.regionMatches(true, 0, args[0], 0, args[0].length())).toList();
        }
        if (args.length == 2 && "accept".startsWith(args[1].toLowerCase(java.util.Locale.ROOT))) return List.of("accept");
        return Collections.emptyList();
    }
}
