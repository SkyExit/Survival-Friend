package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

public class CancelCreeper implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.CREEPER) {
            if(PermissionManager.checkEnabled(MenuSF.MenuItems.CREEPER_EXPLOSIONS)) {
                event.blockList().clear();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if(PermissionManager.checkEnabled(MenuSF.MenuItems.CREEPER_DAMAGE)) {
            if (e.getEntity() instanceof org.bukkit.entity.Player) {
                if (e.getDamager().getType() == EntityType.CREEPER)
                    e.setCancelled(true);
            }
        }

        // Prevent ItemFrame destruction when blockdamage is disabled
        if(PermissionManager.checkEnabled(MenuSF.MenuItems.CREEPER_EXPLOSIONS)) {
            if (e.getEntity() instanceof Hanging) {
                if (e.getDamager().getType() == EntityType.CREEPER)
                    e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (PermissionManager.checkEnabled(MenuSF.MenuItems.CREEPER_EXPLOSIONS)
                && event.getRemover() != null && event.getRemover().getType() == EntityType.CREEPER) {
            event.setCancelled(true);
        }
    }
}
