package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HarderDragon implements Listener {
    private final NamespacedKey resistance = new NamespacedKey(SF.getPlugin(), "dragon-resistance");

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!PermissionManager.checkEnabled(MenuSF.MenuItems.HARDER_DRAGON)
                || !(event.getEntity() instanceof EnderDragon dragon)) return;
        AttributeInstance health = dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health == null) return;
        double configuredHealth = SF.getPlugin().getConfig().getDouble("hd.health", 1000);
        if (!Double.isFinite(configuredHealth) || configuredHealth <= 0) {
            SF.getPlugin().getLogger().warning("Invalid hd.health; using 1000 HP.");
            configuredHealth = 1000;
        }
        health.setBaseValue(configuredHealth);
        dragon.setHealth(health.getValue());
        dragon.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 20, false, false));
        dragon.getPersistentDataContainer().set(resistance, PersistentDataType.INTEGER, 0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonDamage(EntityDamageEvent event) {
        if (!PermissionManager.checkEnabled(MenuSF.MenuItems.HARDER_DRAGON)
                || !(event.getEntity() instanceof EnderDragon dragon)
                || !dragon.getPersistentDataContainer().has(resistance, PersistentDataType.INTEGER)) return;
        // Inspect actual health after Bukkit applies damage; never apply it a second time.
        Bukkit.getScheduler().runTask(SF.getPlugin(), () -> {
            if (!dragon.isValid() || dragon.isDead() || !PermissionManager.checkEnabled(MenuSF.MenuItems.HARDER_DRAGON)) return;
            AttributeInstance health = dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health == null || health.getValue() <= 0) return;
            int level = Math.min(3, (int) ((health.getValue() - dragon.getHealth()) / (health.getValue() / 3)));
            int previous = dragon.getPersistentDataContainer().getOrDefault(resistance, PersistentDataType.INTEGER, 0);
            if (level <= previous) return;
            dragon.getPersistentDataContainer().set(resistance, PersistentDataType.INTEGER, level);
            dragon.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, level - 1, false, false));
        });
    }
}
