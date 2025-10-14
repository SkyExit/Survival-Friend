package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.McColors;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;

public class TotemPop implements Listener {
    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if(PermissionManager.checkEnabled(MenuSF.MenuItems.TOTEM_MSG)) {
            EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
            Player player = (Player) event.getEntity();

            StringBuilder sb = new StringBuilder();
                sb.append(McColors.GOLD + player.getName() + McColors.AQUA + " was popped by ");

                // IS THE DAMAGER AN ENTITY?
                if(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent entityEvent) {
                    Entity damager = entityEvent.getDamager();
                    sb.append(switch (entityEvent.getCause()) {
                        case ENTITY_EXPLOSION -> McColors.RED + "an exploding " + damager.getName();
                        case PROJECTILE -> McColors.RED + damager.getType().name().toLowerCase() + McColors.AQUA + "'s projectile";
                        default -> McColors.RED + damager.getName();
                    });

                    // GETS ITEM NAME IF DAMAGER IS A PLAYER
                    if(damager instanceof Player && damager != player) {
                        ItemStack item = ((Player)damager).getInventory().getItemInMainHand();
                        if(item.getItemMeta().getDisplayName() != item.getType().name()) {
                            sb.append(McColors.AQUA + " using " + McColors.RED + item.getItemMeta().getDisplayName());
                        }
                    }

                // IS THE DAMAGER NOT AN ENTITY?
                } else {
                    sb.append(switch (entityDamageEvent.getCause()) {
                        case FALL -> McColors.RED + "hitting the ground too hard";
                        case FIRE -> McColors.RED + "fire";
                        case LAVA -> McColors.RED + "lava";
                        case SUFFOCATION -> McColors.RED + "suffocation";
                        case VOID -> McColors.RED + "the void";
                        case BLOCK_EXPLOSION -> McColors.RED + "TNT";
                        default -> McColors.RED + player.getDisplayName();
                    });
                }

            SkyLogger.sendServer(sb.toString(), SkyLogger.LogType.INFO);
        }
    }
}
