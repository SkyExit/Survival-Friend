package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

import java.util.ArrayList;
import java.util.List;

public class SoloPortalFarm implements Listener {
    @EventHandler
    public void onEntityPortalEntry(EntityPortalEnterEvent event){
        if (event.getEntity() instanceof LivingEntity living) {
            if(PermissionManager.checkEnabled(MenuSF.MenuItems.SOLO_PORTAL_FARM)) {
                List<EntityType> mobs = new  ArrayList<>();
                    mobs.add(EntityType.WITHER_SKELETON);
                    mobs.add(EntityType.CREEPER);
                    mobs.add(EntityType.IRON_GOLEM);
                    mobs.add(EntityType.ZOMBIFIED_PIGLIN);
                for (EntityType mob : mobs) {
                    if (event.getEntityType() == mob) {
                        living.setRemoveWhenFarAway(false);
                        return;
                    }
                }
            }
        }
    }
}
