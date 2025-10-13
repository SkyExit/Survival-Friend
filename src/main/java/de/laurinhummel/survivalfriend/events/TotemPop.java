package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.McColors;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;

public class TotemPop implements Listener {
    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if(PermissionManager.checkEnabled(MenuSF.MenuItems.TOTEM_MSG)) {
            EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
            Player player = (Player) event.getEntity();

            StringBuilder sb = new StringBuilder();
                sb.append(McColors.GOLD + player.getName() + McColors.AQUA + " was popped by ");

                if(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent entityEvent) {
                    sb.append(switch (entityEvent.getCause()) {
                        case ENTITY_EXPLOSION -> McColors.RED + "an exploding " + entityEvent.getDamager().getName();
                        default -> McColors.RED + entityEvent.getDamager().getName();
                    });
                } else {
                    sb.append(switch (entityDamageEvent.getCause()) {
                        case FALL -> McColors.RED + "hitting the ground too hard";
                        case BLOCK_EXPLOSION -> McColors.RED + "TNT";
                        default -> McColors.RED + player.getDisplayName();
                    });
                }

                if(entityDamageEvent.getEntity() instanceof Player && entityDamageEvent.getEntity() != player) {
                    sb.append(McColors.AQUA + " using " + McColors.RED + ((Player)entityDamageEvent.getEntity()).getInventory().getItemInMainHand().getItemMeta().getDisplayName());
                }

            SkyLogger.sendServer(sb.toString(), SkyLogger.LogType.INFO);
        }
    }
}
