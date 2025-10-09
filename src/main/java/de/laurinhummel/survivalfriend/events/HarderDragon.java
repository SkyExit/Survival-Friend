package de.laurinhummel.survivalfriend.events;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HarderDragon implements Listener {
    private static final double EXTRA_HEALTH = 1000;

    private static final double HP_THRESHOLD = 300;

    private double lastMilestone = 0.0D;

    private int resistanceLevel = 0;

    @EventHandler
    private void onEntitySpawn(EntitySpawnEvent e) {
        EnderDragon dragon;
        Entity entity = e.getEntity();
        if (entity instanceof EnderDragon) {
            dragon = (EnderDragon)entity;
        } else {
            return;
        }
        dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(EXTRA_HEALTH);
        dragon.setHealth(dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        dragon.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2147483647, 20, false, false));
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
        if (this.lastMilestone - currentHealth >= HP_THRESHOLD) {
            this.lastMilestone -= HP_THRESHOLD;
            this.resistanceLevel++;
            this.resistanceLevel = Math.min(this.resistanceLevel, 3);
            dragon.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            dragon.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2147483647, this.resistanceLevel, false, false));
        }
        dragon.setHealth(currentHealth);
    }
}
