package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

import java.util.EnumSet;
import java.util.Set;

public class SoloPortalFarm implements Listener {
    private static final Set<EntityType> FARM_MOBS = EnumSet.of(EntityType.WITHER_SKELETON, EntityType.CREEPER,
            EntityType.IRON_GOLEM, EntityType.ZOMBIFIED_PIGLIN, EntityType.GUARDIAN);
    @EventHandler
    public void onEntityPortalEntry(EntityPortalEnterEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            if(PermissionManager.checkEnabled(MenuSF.MenuItems.SOLO_PORTAL_FARM)) {
                if (FARM_MOBS.contains(event.getEntityType())) living.setRemoveWhenFarAway(false);
            }
        }
    }
}
