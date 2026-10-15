package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.misc.McColors;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Locale;

public class TotemPop implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent event) {
        if (event.isCancelled() || event.getHand() == null || !(event.getEntity() instanceof Player player)
                || !PermissionManager.checkEnabled(MenuSF.MenuItems.TOTEM_MSG)) return;

        String cause = describeDamage(player.getLastDamageCause(), player);
        SkyLogger.sendServer(McColors.GOLD + player.getName() + McColors.AQUA + " was popped by "
                + McColors.RED + cause, SkyLogger.LogType.INFO);
    }

    private String describeDamage(EntityDamageEvent damage, Player player) {
        if (damage == null) return "an unknown cause";
        if (damage instanceof EntityDamageByEntityEvent entityDamage) {
            return describeAttacker(entityDamage, player);
        }
        if (damage instanceof EntityDamageByBlockEvent blockDamage && blockDamage.getDamager() != null
                && !blockDamage.getDamager().getType().isAir()) {
            String block = readable(blockDamage.getDamager().getType().name());
            return damage.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                    ? "an exploding " + block : block;
        }
        // Names also cover causes added after the oldest supported Bukkit API.
        return switch (damage.getCause().name()) {
            case "FALL" -> "hitting the ground too hard";
            case "FIRE", "FIRE_TICK" -> "fire";
            case "LAVA" -> "lava";
            case "SUFFOCATION" -> "suffocation";
            case "VOID" -> "the void";
            case "BLOCK_EXPLOSION", "ENTITY_EXPLOSION" -> "an explosion";
            case "DROWNING" -> "drowning";
            case "STARVATION" -> "starvation";
            case "FREEZE" -> "freezing";
            case "HOT_FLOOR" -> "a magma block";
            case "CONTACT" -> "a prickly block";
            case "CRAMMING" -> "entity cramming";
            case "FLY_INTO_WALL" -> "flying into a wall";
            case "POISON" -> "poison";
            case "WITHER" -> "the wither effect";
            case "MAGIC" -> "magic";
            case "DRAGON_BREATH" -> "dragon's breath";
            case "SONIC_BOOM" -> "a sonic boom";
            case "LIGHTNING" -> "a lightning strike";
            case "FALLING_BLOCK" -> "a falling block";
            case "THORNS" -> "thorns";
            case "WORLD_BORDER" -> "the world border";
            case "KILL", "SUICIDE", "CUSTOM" -> "an unknown cause";
            default -> readable(damage.getCause().name());
        };
    }

    private String describeAttacker(EntityDamageByEntityEvent damage, Player victim) {
        Entity damager = damage.getDamager();
        boolean ownsDamager = SF.getPlugin().getTasks().owns(damager);
        if (damager instanceof Projectile projectile) {
            String projectileName = readable(damager.getType().name());
            return ownsDamager ? fromSource(projectileName, projectile.getShooter()) : projectileName;
        }
        if (ownsDamager && damager instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            return source == null ? "TNT" : "TNT ignited by " + entityName(source);
        }
        if (ownsDamager && damager instanceof AreaEffectCloud cloud) {
            return fromSource("lingering cloud", cloud.getSource());
        }
        if (ownsDamager && damager instanceof EvokerFangs fangs) {
            LivingEntity owner = fangs.getOwner();
            return owner == null ? "evoker fangs" : entityName(owner) + "'s evoker fangs";
        }
        if (ownsDamager && damager instanceof FallingBlock block) {
            return "a falling " + readable(block.getBlockData().getMaterial().name());
        }

        String name = entityName(damager);
        String description = switch (damage.getCause()) {
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> "an exploding " + name;
            case PROJECTILE -> name + "'s projectile";
            case THORNS -> "thorns from " + name;
            case SONIC_BOOM -> name + "'s sonic boom";
            case LIGHTNING -> "a lightning strike";
            default -> name;
        };
        if (ownsDamager && damager instanceof Player attacker && attacker != victim
                && (damage.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || damage.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) {
            ItemStack item = attacker.getInventory().getItemInMainHand();
            if (item.getType().isAir()) return description;
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                description += McColors.AQUA + " using " + McColors.RED + meta.getDisplayName();
            }
        }
        return description;
    }

    private String fromSource(String attack, ProjectileSource source) {
        if (source instanceof Entity entity) return entityName(entity) + "'s " + attack;
        if (source instanceof BlockProjectileSource) return attack + " from a dispenser";
        return attack;
    }

    private String entityName(Entity entity) {
        if (entity instanceof Player || SF.getPlugin().getTasks().owns(entity)) return entity.getName();
        return readable(entity.getType().name());
    }

    private String readable(String name) {
        return name.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
