package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CancelCropTrampling implements Listener {

    @EventHandler
    public void onMobTrample(EntityInteractEvent event) {
        if(PermissionManager.checkEnabled(MenuSF.MenuItems.TRAMPLING_MOB)) {
            if (event.getEntity() instanceof org.bukkit.entity.Player) return;
            if (event.getBlock().getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerTrample(PlayerInteractEvent event) {
        if(PermissionManager.checkEnabled(MenuSF.MenuItems.TRAMPLING_PLAYER)) {
            if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null
                    && event.getClickedBlock().getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
        }
    }
}
