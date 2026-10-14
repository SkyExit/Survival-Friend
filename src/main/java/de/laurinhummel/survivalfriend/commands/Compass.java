package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Compass implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(!PermissionManager.checkPermission(sender, MenuSF.MenuItems.PATHFINDER)) { return true; }

        Player player = (Player) sender;
        ItemStack compass = player.getInventory().getItemInMainHand();
        if(compass.getType() != Material.COMPASS) { SkyLogger.sendPlayer(player, "Sorry, but you need to hold a compass in your main hand!", SkyLogger.LogType.ERROR); return true; }

        switch (args.length) {
            case 1 -> {
                if (args[0].equalsIgnoreCase("reset")) {
                    player.setCompassTarget(player.getWorld().getSpawnLocation());
                    SkyLogger.sendPlayer(player, "Compass coordinates were reset to spawn.", SkyLogger.LogType.INFO);
                    return true;
                }
                Player target = Bukkit.getServer().getPlayerExact(args[0]);
                if(target != null && target.isOnline()) {
                    SF.getPlugin().getTasks().execute(target, () -> {
                        if (!target.isOnline()) return;
                        Location destination = target.getLocation().clone();
                        String name = target.getName();
                        SF.getPlugin().getTasks().execute(player, () -> {
                            if (!player.isOnline() || !PermissionManager.checkPermission(player, MenuSF.MenuItems.PATHFINDER)) return;
                            if (!destination.getWorld().equals(player.getWorld())) {
                                SkyLogger.sendPlayer(player, "This player is in another world.", SkyLogger.LogType.ERROR);
                                return;
                            }
                            player.setCompassTarget(destination);
                            SkyLogger.sendPlayer(player, "Compass coordinates were set to " + ChatColor.GOLD + name, SkyLogger.LogType.INFO);
                        });
                    });
                } else {
                    SkyLogger.sendPlayer(player, "Sorry, but this player is not online...", SkyLogger.LogType.ERROR);
                }
            }
            case 2 -> {
                try {
                    double x = coordinate(args[0], player.getLocation().getX());
                    double z = coordinate(args[1], player.getLocation().getZ());

                    player.setCompassTarget(new Location(player.getWorld(), x, 64, z));

                    SkyLogger.sendPlayer(player, "Compass coordinates were set to " + ChatColor.GOLD + x + "/" + z,  SkyLogger.LogType.INFO);
                } catch (NumberFormatException ex) {
                    SkyLogger.sendPlayer(player, "Sorry, but i couldn't read these coordinates...", SkyLogger.LogType.ERROR);
                }
            }
            default -> { SkyLogger.sendPlayer(player, "Sorry, but you need to specify a player or coordinates!", SkyLogger.LogType.ERROR); return true; }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if(args.length == 1) {
            List<String> params = new ArrayList<>();
            params.add("reset");
            params.add("~");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!(commandSender instanceof Player player) || player.canSee(p)) params.add(p.getName());
            }

            return params.stream().filter(value -> value.regionMatches(true, 0, args[0], 0, args[0].length())).toList();
        }
        if(args.length == 2) {
            List<String> params = new ArrayList<>();
            params.add("~");

            return params;
        }

        return Collections.emptyList();
    }

    private static double coordinate(String input, double current) {
        boolean relative = input.startsWith("~");
        String number = relative ? input.substring(1) : input;
        double value = relative && number.isEmpty() ? current : Double.parseDouble(number) + (relative ? current : 0);
        if (!Double.isFinite(value) || Math.abs(value) > 30_000_000) throw new NumberFormatException("Invalid coordinate");
        return value;
    }
}
