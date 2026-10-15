package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.compat.ServerPlatform;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Restores End portal gravity duplication */
public final class GravityDuplication implements Listener {
    private static final String PROCESSED_TAG = "survivalfriend_gravity_processed";
    private static final String DUPLICATE_TAG = "survivalfriend_gravity_duplicate";
    private static final double HORIZONTAL_MULTIPLIER = 1.499;
    private static final double VERTICAL_MULTIPLIER = -1.7;
    private static final int PORTAL_TIMEOUT_TICKS = 100;
    private final SF plugin;
    private final AtomicBoolean transferWarning = new AtomicBoolean();

    private record Transfer(UUID sourceWorld, Vector velocity, boolean gravity, boolean dropItem, boolean persistent) { }

    public GravityDuplication(SF plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock falling) || !canDuplicate(falling)) return;
        Block portal = portalInPath(falling);
        if (portal != null) spawnDuplicate(falling, portal, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof FallingBlock falling) || !canDuplicate(falling)) return;
        // A duper may break the source into an item instead of placing a block.
        // In that case EntityChangeBlockEvent never reports a successful placement.
        Block portal = portalInPath(falling);
        if (portal != null) spawnDuplicate(falling, portal, false);
    }

    private Block portalInPath(FallingBlock falling) {
        Vector velocity = falling.getVelocity();
        Location location = falling.getLocation();
        Block current = ownedBlock(location);
        if (current != null && current.getType() == Material.END_PORTAL) return current;
        double x = Math.abs(velocity.getX());
        double y = Math.abs(velocity.getY());
        double z = Math.abs(velocity.getZ());
        if (x > y && x > z) location.add(Math.signum(velocity.getX()), 0, 0);
        else if (y > z) location.add(0, Math.signum(velocity.getY()), 0);
        else location.add(0, 0, Math.signum(velocity.getZ()));

        Block portal = ownedBlock(location);
        return portal != null && portal.getType() == Material.END_PORTAL ? portal : null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof FallingBlock falling) || !canDuplicate(falling)) return;
        Block portal = ownedBlock(event.getLocation());
        if (portal == null || portal.getType() != Material.END_PORTAL) return;
        Block support = ownedBlock(portal.getLocation().subtract(0, 1, 0));
        if (support != null && Tag.FENCES.isTagged(support.getType())) spawnDuplicate(falling, portal, true);
    }

    private boolean canDuplicate(FallingBlock falling) {
        return ServerPlatform.isFolia()
                && PermissionManager.checkEnabled(MenuSF.MenuItems.GRAVITY_DUPLICATION)
                && !falling.getScoreboardTags().contains(PROCESSED_TAG)
                && !falling.getScoreboardTags().contains("foliaflow_dupe");
    }

    private Block ownedBlock(Location location) {
        if (location.getY() < location.getWorld().getMinHeight()
                || location.getY() >= location.getWorld().getMaxHeight()) return null;
        if (!plugin.getTasks().owns(location)) return null;
        return location.getBlock();
    }

    private void spawnDuplicate(FallingBlock original, Block portal, boolean preserveVerticalVelocity) {
        Vector velocity = original.getVelocity().clone();
        velocity.setX(velocity.getX() * HORIZONTAL_MULTIPLIER);
        velocity.setZ(velocity.getZ() * HORIZONTAL_MULTIPLIER);
        if (!preserveVerticalVelocity) velocity.setY(velocity.getY() * VERTICAL_MULTIPLIER);

        Transfer transfer = new Transfer(original.getWorld().getUID(), velocity, original.hasGravity(),
                original.getDropItem(), original.isPersistent());
        FallingBlock duplicate = portal.getWorld().spawnFallingBlock(
                portal.getLocation().add(0.5, 0.7, 0.5), original.getBlockData());
        // The copy clears a fence's top while still intersecting the portal's trigger shape.
        // Multiple event paths may report one source; neither entity may duplicate again.
        original.addScoreboardTag(PROCESSED_TAG);
        duplicate.addScoreboardTag(PROCESSED_TAG);
        duplicate.addScoreboardTag(DUPLICATE_TAG);
        duplicate.addScoreboardTag("foliaflow_dupe");
        // A moving copy can leave the portal or land before Folia processes the transfer.
        // Keep it inside the portal until the native portal logic changes its world.
        duplicate.setGravity(false);
        duplicate.setVelocity(new Vector());
        duplicate.setDropItem(false);
        duplicate.setPersistent(false);
        plugin.getTasks().later(duplicate, () -> awaitArrival(duplicate, transfer, PORTAL_TIMEOUT_TICKS));
    }

    private void awaitArrival(FallingBlock duplicate, Transfer transfer, int remainingTicks) {
        if (!duplicate.isValid()) return;
        if (!PermissionManager.checkEnabled(MenuSF.MenuItems.GRAVITY_DUPLICATION)) {
            duplicate.remove();
            return;
        }
        if (!duplicate.getWorld().getUID().equals(transfer.sourceWorld)) {
            finishArrival(duplicate, transfer);
        } else if (remainingTicks <= 0) {
            duplicate.remove();
            if (transferWarning.compareAndSet(false, true)) {
                plugin.getLogger().warning("A gravity duplicate could not pass through its End portal. "
                        + "The temporary copy was removed; check portal protection or cancelled portal events.");
            }
        } else {
            plugin.getTasks().later(duplicate, () -> awaitArrival(duplicate, transfer, remainingTicks - 1));
        }
    }

    private void finishArrival(FallingBlock duplicate, Transfer transfer) {
        // This offset belongs in the destination world, after the real portal transfer.
        plugin.getTasks().teleport(duplicate, duplicate.getLocation().add(0, 0.5, 0))
                .whenComplete((success, error) -> {
                    // A rejected height adjustment must not leave an arrived block frozen.
                    plugin.getTasks().execute(duplicate, () -> {
                        if (!duplicate.isValid()) return;
                        if (!PermissionManager.checkEnabled(MenuSF.MenuItems.GRAVITY_DUPLICATION)) {
                            duplicate.remove();
                            return;
                        }
                        duplicate.setGravity(transfer.gravity);
                        duplicate.setDropItem(transfer.dropItem);
                        duplicate.setPersistent(transfer.persistent);
                        duplicate.setVelocity(transfer.velocity.clone());
                        transferWarning.set(false);
                    });
                });
    }
}
