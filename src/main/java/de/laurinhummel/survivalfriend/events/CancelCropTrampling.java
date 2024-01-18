package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.Menu;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;

public class CancelCropTrampling implements Listener {
    Material[] materials = new Material[]{ Material.FARMLAND, Material.SOUL_SOIL };

    @EventHandler
    public void onMobTrample(EntityInteractEvent event) {
        if(!SF.getPlugin().getConfig().getBoolean(Menu.MenuItems.TRAMPLING_MOB.getPath())) {
            if (event.getEntity() instanceof org.bukkit.entity.Player) return;
            if (Arrays.stream(materials).anyMatch(material -> material == event.getBlock().getType())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerTrample(PlayerInteractEvent event) {
        if(!SF.getPlugin().getConfig().getBoolean(Menu.MenuItems.TRAMPLING_PLAYER.getPath())) {
            if (event.getAction() == Action.PHYSICAL && (Arrays.stream(materials).anyMatch(material -> material == event.getClickedBlock().getType()))) {
                event.setCancelled(true);
            }
        }
    }
}
