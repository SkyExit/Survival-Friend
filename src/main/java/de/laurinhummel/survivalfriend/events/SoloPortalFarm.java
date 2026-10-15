package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.ConfigAccess;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public class SoloPortalFarm implements Listener {
    private final Set<EntityType> farmMobs;

    public SoloPortalFarm(SF plugin) {
        Set<EntityType> mobs = EnumSet.noneOf(EntityType.class);
        for (String name : ConfigAccess.read(plugin, config -> config.getStringList("spf.mobs"))) {
            try {
                EntityType type = EntityType.valueOf(name.trim().toUpperCase(Locale.ROOT));
                Class<?> entityClass = type.getEntityClass();
                if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass)
                        || Player.class.isAssignableFrom(entityClass) || type == EntityType.ARMOR_STAND) {
                    plugin.getLogger().warning("Ignoring non-mob spf.mobs entry: " + name);
                    continue;
                }
                mobs.add(type);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown mob in spf.mobs: " + name);
            }
        }
        farmMobs = Set.copyOf(mobs);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPortalEntry(EntityPortalEnterEvent event) {
        if (!farmMobs.contains(event.getEntityType())
                || !(event.getEntity() instanceof LivingEntity living)
                || !PermissionManager.checkEnabled(MenuSF.MenuItems.SOLO_PORTAL_FARM)) return;

        // The event owns this entity on Folia. Set the flag before the portal copies it to its destination.
        living.setRemoveWhenFarAway(false);
    }
}
