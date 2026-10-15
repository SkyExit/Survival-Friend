package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.compat.ServerPlatform;
import de.laurinhummel.survivalfriend.managers.ConfigAccess;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import de.laurinhummel.survivalfriend.managers.StasisChunkTickets;
import de.laurinhummel.survivalfriend.managers.StasisStore;
import de.laurinhummel.survivalfriend.managers.StasisStore.Chamber;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.BubbleColumn;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent Folia stasis chambers, adapted from PearlStasisFix's placeholder approach. */
public final class PearlStasis implements Listener, AutoCloseable {
    private static final int[][] HORIZONTAL_OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private record Settings(int radius, double height, boolean bubbles, int bubbleTicks, int touchTicks, Set<Material> mediums) { }

    private final SF plugin;
    private final Settings settings;
    private final StasisStore store;
    private final StasisChunkTickets tickets;
    private final NamespacedKey chamberKey;
    private final Map<UUID, Chamber> chambers = new ConcurrentHashMap<>();
    private final Map<UUID, Flight> flights = new ConcurrentHashMap<>();
    private final Map<UUID, ParkedPearl> active = new ConcurrentHashMap<>();
    private final Set<UUID> teleporting = ConcurrentHashMap.newKeySet();
    private volatile boolean running;
    private volatile boolean closed;
    private volatile long generation;

    public PearlStasis(SF plugin) {
        this.plugin = plugin;
        settings = loadSettings();
        chamberKey = new NamespacedKey(plugin, "pearl-stasis-chamber");
        store = new StasisStore(plugin);
        tickets = new StasisChunkTickets(plugin);
        store.load().forEach(chamber -> chambers.put(chamber.id, chamber));
    }

    private Settings loadSettings() {
        return ConfigAccess.read(plugin, config -> {
            String path = "pearl-stasis.";
            Set<Material> mediums = EnumSet.noneOf(Material.class);
            if (config.getBoolean(path + "detection.honey-blocks", true)) mediums.add(Material.HONEY_BLOCK);
            for (String name : config.getStringList(path + "detection.extra-blocks")) {
                Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
                if (material != null && material.isBlock() && !material.isAir()) mediums.add(material);
                else plugin.getLogger().warning("Ignoring invalid pearl stasis medium: " + name);
            }
            double height = config.getDouble(path + "dummy-height", 0.87);
            return new Settings(Math.max(0, Math.min(8, config.getInt(path + "chunk-radius", 1))),
                    Double.isFinite(height) ? Math.max(0, Math.min(0.99, height)) : 0.87,
                    config.getBoolean(path + "detection.bubble-columns", true),
                    Math.max(1, config.getInt(path + "detection.min-ticks-bubble-column", 2)),
                    Math.max(1, config.getInt(path + "detection.min-ticks-touch-medium", 10)), Set.copyOf(mediums));
        });
    }

    public synchronized void refresh() {
        boolean enabled = !closed && ServerPlatform.isFolia()
                && PermissionManager.checkEnabled(MenuSF.MenuItems.PEARL_STASIS);
        if (running == enabled) return;
        running = enabled;
        generation++;
        if (enabled) chambers.values().forEach(this::resume);
        else {
            flights.clear();
            active.values().forEach(parked -> plugin.getTasks().execute(parked.item, () -> {
                ParkedPearl current = active.get(parked.chamber.id);
                if (current == null || !current.item.getUniqueId().equals(parked.item.getUniqueId())) parked.item.remove();
            }));
            active.clear();
            tickets.clear();
        }
    }

    private boolean enabled() {
        return running && !closed && ServerPlatform.isFolia()
                && PermissionManager.checkEnabled(MenuSF.MenuItems.PEARL_STASIS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onThrow(ProjectileLaunchEvent event) {
        if (!enabled() || !(event.getEntity() instanceof EnderPearl pearl)
                || !(pearl.getShooter() instanceof Player player)) return;
        Flight flight = new Flight(pearl, player.getUniqueId(), generation);
        if (flights.putIfAbsent(pearl.getUniqueId(), flight) == null) flight.schedule();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!enabled()) return;
        Flight flight = flights.get(event.getEntity().getUniqueId());
        if (flight != null && flight.armed && flight.capture(flight.anchor())) event.setCancelled(true);
    }

    private final class Flight implements Runnable {
        private final EnderPearl pearl;
        private final UUID owner;
        private final UUID id;
        private final long session;
        private int dwell, medium, fallTicks;
        private double highestY;
        private Location peak, fluid;
        private boolean armed;

        private Flight(EnderPearl pearl, UUID owner, long session) {
            this.pearl = pearl;
            this.owner = owner;
            this.id = pearl.getUniqueId();
            this.session = session;
        }

        private void schedule() {
            plugin.getTasks().later(pearl, this, () -> flights.remove(id, this), 1L);
        }

        @Override public void run() {
            if (!enabled() || generation != session || flights.get(id) != this || !pearl.isValid()) {
                flights.remove(id, this);
                return;
            }
            Location location = pearl.getLocation();
            Block block = ownedBlock(location);
            if (block == null) { schedule(); return; }
            if (!armed) {
                int detected = medium(block);
                if (detected == 0 || medium != detected) dwell = 0;
                medium = detected;
                int required = detected == 1 ? settings.bubbleTicks : settings.touchTicks;
                if (detected != 0 && ++dwell >= required) {
                    armed = true;
                    highestY = location.getY();
                    peak = location.clone();
                    if (isFluid(block)) fluid = location.clone();
                }
            } else {
                Block ceiling = ownedBlock(location.clone().add(0, 1, 0));
                if (ceiling != null && ceiling.getType().isSolid() && capture(location)) return;
                boolean submerged = isFluid(block);
                if (submerged) fluid = location.clone();
                if (submerged || location.getY() >= highestY - 0.02) {
                    if (location.getY() > highestY) { highestY = location.getY(); peak = location.clone(); }
                    fallTicks = 0;
                } else if (++fallTicks >= 3 && capture(anchor())) return;
            }
            schedule();
        }

        private Location anchor() {
            Location anchor = (fluid == null ? peak : fluid).clone();
            if (fluid != null) {
                for (int i = 0; i < 64; i++) {
                    Block above = ownedBlock(anchor.clone().add(0, 1, 0));
                    if (above == null || !isFluid(above)) break;
                    anchor.add(0, 1, 0);
                }
            }
            return anchor;
        }

        private boolean capture(Location location) {
            if (!enabled() || generation != session || flights.get(id) != this) return false;
            Location anchor = new Location(location.getWorld(), location.getBlockX() + 0.5,
                    location.getBlockY() + settings.height, location.getBlockZ() + 0.5);
            Chamber chamber = new Chamber(id, owner, anchor, settings.radius);
            synchronized (store) {
                if (chambers.putIfAbsent(id, chamber) != null) return false;
                if (!store.save(chambers.values())) { chambers.remove(id, chamber); return false; }
            }
            flights.remove(id, this);
            pearl.remove();
            resume(chamber);
            return true;
        }
    }

    private int medium(Block block) {
        if (settings.bubbles && block.getType() == Material.BUBBLE_COLUMN
                && block.getBlockData() instanceof BubbleColumn column && !column.isDrag()) return 1;
        if (settings.mediums.contains(block.getType())) return 2;
        Location location = block.getLocation();
        for (int[] offset : HORIZONTAL_OFFSETS) {
            Block adjacent = ownedBlock(location.clone().add(offset[0], 0, offset[1]));
            if (adjacent != null && settings.mediums.contains(adjacent.getType())) return 2;
        }
        return 0;
    }

    private boolean isFluid(Block block) {
        return block.getType() == Material.WATER || block.getType() == Material.BUBBLE_COLUMN;
    }

    private Block ownedBlock(Location location) {
        World world = location.getWorld();
        if (location.getY() < world.getMinHeight() || location.getY() >= world.getMaxHeight()
                || !plugin.getTasks().owns(location)) return null;
        return location.getBlock();
    }

    private void resume(Chamber chamber) {
        if (!enabled() || active.containsKey(chamber.id)) return;
        World world = Bukkit.getWorld(chamber.world);
        if (world == null || chamber.y < world.getMinHeight() || chamber.y >= world.getMaxHeight()) return;
        Location anchor = chamber.location(world);
        long session = generation;
        tickets.acquire(chamber.id, anchor, chamber.radius);
        plugin.getTasks().at(anchor, () -> {
            if (!enabled() || generation != session || chambers.get(chamber.id) != chamber || active.containsKey(chamber.id)) return;
            for (Entity entity : anchor.getChunk().getEntities()) {
                if (entity instanceof Item item && chamber.id.equals(chamberId(item))) { attach(item, chamber); return; }
            }
            Item item = world.dropItem(anchor, new ItemStack(Material.ENDER_PEARL));
            item.getPersistentDataContainer().set(chamberKey, PersistentDataType.STRING, chamber.id.toString());
            attach(item, chamber);
        }).exceptionally(error -> {
            plugin.getLogger().warning("Could not restore pearl stasis chamber " + chamber.id + ": " + error.getMessage());
            return null;
        });
    }

    private UUID chamberId(Entity entity) {
        if (!(entity instanceof Item)) return null;
        String value = entity.getPersistentDataContainer().get(chamberKey, PersistentDataType.STRING);
        if (value == null) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private void attach(Item item, Chamber chamber) {
        ParkedPearl parked = new ParkedPearl(item, chamber, generation);
        ParkedPearl existing = active.putIfAbsent(chamber.id, parked);
        if (existing != null) { if (!existing.item.getUniqueId().equals(item.getUniqueId())) item.remove(); return; }
        item.setGravity(false);
        item.setVelocity(new Vector());
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setUnlimitedLifetime(true);
        item.setInvulnerable(true);
        item.setSilent(true);
        item.setPersistent(true);
        tickets.acquire(chamber.id, parked.anchor, chamber.radius);
        parked.schedule();
        if (chamber.pending) trigger(chamber);
    }

    private final class ParkedPearl implements Runnable {
        private final Item item;
        private final Chamber chamber;
        private final Location anchor;
        private final long session;
        private Set<Integer> previousCollisions;
        private volatile boolean pinning;
        private int age;

        private ParkedPearl(Item item, Chamber chamber, long session) {
            this.item = item;
            this.chamber = chamber;
            this.anchor = chamber.location(item.getWorld());
            this.session = session;
        }

        private void schedule() { plugin.getTasks().later(item, this, () -> active.remove(chamber.id, this), 1L); }

        @Override public void run() {
            if (!enabled() || session != generation || active.get(chamber.id) != this || !item.isValid()) return;
            item.setVelocity(new Vector());
            if (!pinning && (item.getWorld() != anchor.getWorld() || item.getLocation().distanceSquared(anchor) > 0.0001)) {
                pinning = true;
                plugin.getTasks().teleport(item, anchor).whenComplete((success, error) -> pinning = false);
            }
            if (++age >= 10 && age % 2 == 0 && plugin.getTasks().owns(anchor)) {
                Set<Integer> collisions = collisions(anchor);
                if (previousCollisions != null && !previousCollisions.containsAll(collisions)) trigger(chamber);
                previousCollisions = collisions;
            }
            schedule();
        }
    }

    private Set<Integer> collisions(Location anchor) {
        BoundingBox pearl = new BoundingBox(anchor.getX() - 0.2, anchor.getY() - 0.2, anchor.getZ() - 0.2,
                anchor.getX() + 0.2, anchor.getY() + 0.8, anchor.getZ() + 0.2);
        Set<Integer> collisions = new HashSet<>();
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            Block block = ownedBlock(anchor.clone().add(x, y, z));
            if (block != null && block.getType().isSolid() && block.getBoundingBox().overlaps(pearl)) {
                collisions.add((x + 1) * 9 + (y + 1) * 3 + z + 1);
            }
        }
        return collisions;
    }

    private void trigger(Chamber chamber) {
        if (!enabled() || chambers.get(chamber.id) != chamber || !teleporting.add(chamber.id)) return;
        chamber.pending = true;
        store.save(chambers.values());
        Player owner = Bukkit.getPlayer(chamber.owner);
        if (owner == null) { teleporting.remove(chamber.id); return; }
        plugin.getTasks().execute(owner, () -> {
            if (!enabled() || !owner.isOnline() || chambers.get(chamber.id) != chamber) {
                teleporting.remove(chamber.id);
                return;
            }
            World world = Bukkit.getWorld(chamber.world);
            if (world == null) { teleporting.remove(chamber.id); return; }
            Location destination = chamber.location(world);
            destination.setYaw(owner.getLocation().getYaw());
            destination.setPitch(owner.getLocation().getPitch());
            plugin.getTasks().teleport(owner, destination).whenComplete((success, error) -> {
                if (Boolean.TRUE.equals(success)) consume(chamber);
                else {
                    chamber.pending = false;
                    store.save(chambers.values());
                    plugin.getTasks().execute(owner, () -> owner.sendMessage(ChatColor.RED
                            + "The stasis teleport failed. Your pearl is still available."));
                }
                teleporting.remove(chamber.id);
            });
        }, () -> teleporting.remove(chamber.id));
    }

    private void consume(Chamber chamber) {
        synchronized (store) {
            if (!chambers.remove(chamber.id, chamber)) return;
            store.save(chambers.values());
        }
        ParkedPearl parked = active.remove(chamber.id);
        if (parked != null) plugin.getTasks().execute(parked.item, parked.item::remove);
        tickets.release(chamber.id);
    }

    private void trigger(Entity entity) {
        UUID id = chamberId(entity);
        if (id != null) {
            Chamber chamber = chambers.get(id);
            if (chamber != null) trigger(chamber);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (chamberId(event.getRightClicked()) == null) return;
        event.setCancelled(true);
        trigger(event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (chamberId(event.getEntity()) == null) return;
        event.setCancelled(true);
        if (event instanceof EntityDamageByEntityEvent) trigger(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (chamberId(event.getItem()) == null) return;
        event.setCancelled(true);
        if (event.getEntity() instanceof Player) trigger(event.getItem());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (chamberId(event.getItem()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        if (chamberId(event.getEntity()) != null || chamberId(event.getTarget()) != null) event.setCancelled(true);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (!enabled()) return;
        UUID owner = event.getPlayer().getUniqueId();
        chambers.values().stream().filter(chamber -> chamber.pending && chamber.owner.equals(owner)).forEach(this::trigger);
    }

    @EventHandler public void onWorldLoad(WorldLoadEvent event) {
        if (enabled()) chambers.values().stream().filter(chamber -> chamber.world.equals(event.getWorld().getName())).forEach(this::resume);
    }

    @EventHandler public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            UUID id = chamberId(entity);
            if (id == null || !(entity instanceof Item item)) continue;
            plugin.getTasks().execute(item, () -> {
                Chamber chamber = chambers.get(id);
                if (!enabled() || chamber == null) item.remove();
                else attach(item, chamber);
            });
        }
    }

    @Override public synchronized void close() {
        closed = true;
        running = false;
        generation++;
        flights.clear();
        active.clear();
        tickets.clear();
        store.save(chambers.values());
    }
}
