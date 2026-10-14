package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.McColors;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TotemPop implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) return;
        if(PermissionManager.checkEnabled(MenuSF.MenuItems.TOTEM_MSG)) {
            EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
            Player player = (Player) event.getEntity();

            StringBuilder sb = new StringBuilder();
                sb.append(McColors.GOLD + player.getName() + McColors.AQUA + " was popped by ");

                // IS THE DAMAGER AN ENTITY?
                if(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent entityEvent) {
                    Entity damager = entityEvent.getDamager();
                    if (SF.getPlugin().getTasks().owns(damager) && damager instanceof Projectile projectile
                            && projectile.getShooter() instanceof Entity shooter) {
                        damager = shooter;
                    }
                    // A projectile's shooter can be in a different Folia region.
                    boolean ownsDamager = SF.getPlugin().getTasks().owns(damager);
                    String damagerName = ownsDamager || damager instanceof Player ? damager.getName()
                            : damager.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
                    sb.append(switch (entityEvent.getCause()) {
                        case ENTITY_EXPLOSION -> McColors.RED + "an exploding " + damagerName;
                        case PROJECTILE -> McColors.RED + damagerName + McColors.AQUA + "'s projectile";
                        default -> McColors.RED + damagerName;
                    });

                    // GETS ITEM NAME IF DAMAGER IS A PLAYER
                    if(ownsDamager && entityEvent.getCause() != EntityDamageEvent.DamageCause.PROJECTILE && damager instanceof Player && damager != player) {
                        ItemStack item = ((Player)damager).getInventory().getItemInMainHand();
                        ItemMeta meta = item.getItemMeta();
                        if(meta != null && meta.hasDisplayName()) {
                            sb.append(McColors.AQUA + " using " + McColors.RED + meta.getDisplayName());
                        }
                    }

                // IS THE DAMAGER NOT AN ENTITY?
                } else if (entityDamageEvent != null) {
                    sb.append(switch (entityDamageEvent.getCause()) {
                        case FALL -> McColors.RED + "hitting the ground too hard";
                        case FIRE, FIRE_TICK -> McColors.RED + "fire";
                        case LAVA -> McColors.RED + "lava";
                        case SUFFOCATION -> McColors.RED + "suffocation";
                        case VOID -> McColors.RED + "the void";
                        case BLOCK_EXPLOSION -> McColors.RED + "TNT";
                        default -> McColors.RED + entityDamageEvent.getCause().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
                    });
                } else {
                    sb.append(McColors.RED + "an unknown cause");
                }

            SkyLogger.sendServer(sb.toString(), SkyLogger.LogType.INFO);
        }
    }
}
