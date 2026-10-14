package de.laurinhummel.survivalfriend.compat;

import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Bridges Bukkit renames and class/interface changes between 1.19.4 and 26.2. */
public final class ServerApi {
    public static final Attribute MAX_HEALTH = constant(Attribute.class, "MAX_HEALTH", "GENERIC_MAX_HEALTH");
    public static final PotionEffectType RESISTANCE = constant(PotionEffectType.class, "RESISTANCE", "DAMAGE_RESISTANCE");
    public static final Enchantment POWER = constant(Enchantment.class, "POWER", "ARROW_DAMAGE");
    private static final Method TOP_INVENTORY = topInventoryMethod();

    private ServerApi() { }

    private static <T> T constant(Class<T> type, String... names) {
        for (String name : names) {
            try { return type.cast(type.getField(name).get(null)); }
            catch (NoSuchFieldException ignored) { }
            catch (IllegalAccessException e) { throw new IllegalStateException("Cannot access Bukkit constant " + name, e); }
        }
        throw new IllegalStateException("Missing Bukkit constant: " + type.getName());
    }

    private static Method topInventoryMethod() {
        try { return InventoryView.class.getMethod("getTopInventory"); }
        catch (NoSuchMethodException e) { throw new IllegalStateException("Missing InventoryView API", e); }
    }

    // InventoryView was an abstract class in 1.19.4 and is now an interface.
    // Reflection avoids emitting an invokevirtual/invokeinterface tied to one API version.
    public static Inventory topInventory(InventoryView view) {
        try { return (Inventory) TOP_INVENTORY.invoke(view); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw new IllegalStateException("Cannot read top inventory", e.getCause());
        } catch (IllegalAccessException e) { throw new IllegalStateException("Cannot read top inventory", e); }
    }

    public static Class<? extends Event> mountEventType() {
        for (String name : new String[]{"org.bukkit.event.entity.EntityMountEvent", "org.spigotmc.event.entity.EntityMountEvent"}) {
            try { return Class.forName(name).asSubclass(Event.class); }
            catch (ClassNotFoundException ignored) { }
        }
        throw new IllegalStateException("EntityMountEvent API is unavailable");
    }
}
