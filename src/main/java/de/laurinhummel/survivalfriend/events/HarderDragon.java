package de.laurinhummel.survivalfriend.events;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HarderDragon implements Listener {
    private static final double EXTRA_HEALTH = 1000.0D;

    private static final double HP_THRESHOLD = 300.0D;

    private double lastMilestone = 0.0D;

    private int resistanceLevel = 0;

    @EventHandler
    private void onEntitySpawn(org.bukkit.event.entity.EntitySpawnEvent e) {
        EnderDragon dragon;
        Entity entity = e.getEntity();
        if (entity instanceof EnderDragon) {
            dragon = (EnderDragon)entity;
        } else {
            return;
        }
        dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1000.0D);
        dragon.getAttribute(Attribute.GENERIC_FLYING_SPEED).setBaseValue(25);
        dragon.setHealth(dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        this.lastMilestone = dragon.getHealth();
    }

    @EventHandler
    public void onDragonDamage(EntityDamageEvent e) {
        EnderDragon dragon;
        Entity entity = e.getEntity();
        if (entity instanceof EnderDragon) {
            dragon = (EnderDragon)entity;
        } else {
            return;
        }
        double currentHealth = dragon.getHealth() - e.getFinalDamage();
        currentHealth = Math.min(currentHealth, dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        if (this.lastMilestone - currentHealth >= 300.0D) {
            this.lastMilestone -= 300.0D;
            this.resistanceLevel++;
            this.resistanceLevel = Math.min(this.resistanceLevel, 3);
            dragon.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            dragon.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2147483647, this.resistanceLevel, false, false));
        }
        dragon.setHealth(currentHealth);
    }
}
