package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.managers.PermissionManager;
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
import java.util.Objects;

public class Compass implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        PermissionManager pm = new PermissionManager(sender, MenuSF.MenuItems.PATHFINDER);
            if(!(pm.check() instanceof Boolean)) { sender.sendMessage((String) pm.check()); return true; }

        Player player = (Player) sender;
        ItemStack compass = player.getInventory().getItemInMainHand();
        if(!Objects.equals(compass, new ItemStack(Material.COMPASS))) { player.sendMessage(ChatColor.RED + "Sorry, but you need to hold a compass in your main hand!"); return true; }

        switch (args.length) {
            default -> { player.sendMessage(ChatColor.RED + "Sorry, but you need to specify a player or coordinates!"); return false; }
            case 1 -> {
                Player target = Bukkit.getServer().getPlayer(args[0]);
                if(target != null && target.isOnline()) {
                    player.setCompassTarget(target.getLocation());

                    player.sendMessage(ChatColor.AQUA + "Compass coordinates were set to " + ChatColor.GOLD + target.getName());
                } else {
                    player.sendMessage(ChatColor.RED + "Sorry, but this player is not online...");
                }
            }
            case 2 -> {
                try {
                    int x = Integer.parseInt(args[0]);
                    int z = Integer.parseInt(args[1]);

                    player.setCompassTarget(new Location(player.getWorld(), x, 64, z));

                    player.sendMessage(ChatColor.AQUA + "Compass coordinates were set to " + ChatColor.GOLD + x + "/" + z);
                } catch (Exception ex) {
                    player.sendMessage(ChatColor.RED + "Sorry, but i couldn't read these coordinates...");
                }
            }
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
                params.add(p.getName());
            }

            return params;
        }
        if(args.length == 2) {
            List<String> params = new ArrayList<>();
            params.add("~");

            return params;
        }

        return null;
    }
}
