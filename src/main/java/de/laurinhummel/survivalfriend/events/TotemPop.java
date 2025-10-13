package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

public class TotemPop implements Listener {
    @EventHandler
    public void onEntityPortalEntry(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if(PermissionManager.checkEnabled(MenuSF.MenuItems.TOTEM_MSG)) {
            Bukkit.broadcast("test", "");
        }
    }
}
