package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.Menu;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class CancelCreeper implements Listener {
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.CREEPER) {
            if(!SF.getPlugin().getConfig().getBoolean(Menu.MenuItems.CREEPER_EXPLOSIONS.getPath())) {
                entity.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 0);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if(!SF.getPlugin().getConfig().getBoolean(Menu.MenuItems.CREEPER_DAMAGE.getPath())) {
            if (e.getEntity() instanceof org.bukkit.entity.Player) {
                if (e.getDamager().getType() == EntityType.CREEPER)
                    e.setCancelled(true);
            }
        }
    }
}
