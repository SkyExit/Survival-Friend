package de.laurinhummel.survivalfriend.misc;

import de.laurinhummel.survivalfriend.SF;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkyLogger {
    public enum LogType {
        INFO(McColors.AQUA),
        WARNING(McColors.GOLD),
        ERROR(McColors.RED);

        private final String color;
        LogType(String color) {
            this.color = color;
        }
    }

    public static void sendMessage(CommandSender receiver, String message) {
        receiver.sendMessage(message);
    }

    public static void sendPrefixMessage(CommandSender receiver, String message) {
        sendMessage(receiver, SkyStrings.SFCONSOLE + message);
    }

    public static void sendConsole(String message) {
        sendPrefixMessage(Bukkit.getConsoleSender(), message);
    }

    public static void sendServer(String message, LogType type) {
        String formatted = SkyStrings.SF + type.color + message;
        if (!SF.getPlugin().getTasks().hasEntityScheduler()) {
            Bukkit.getServer().broadcastMessage(formatted);
            return;
        }
        Bukkit.getOnlinePlayers().forEach(player -> SF.getPlugin().getTasks().execute(player, () -> {
            if (player.isOnline() && player.hasPermission("bukkit.broadcast.user")) player.sendMessage(formatted);
        }));
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    public static void sendPlayer(Player player, String message, LogType type) {
        player.sendMessage(SkyStrings.SF + type.color + message);
    }
}
