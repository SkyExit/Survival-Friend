package de.laurinhummel.survivalfriend.events;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.compat.ServerPlatform;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.util.Vector;

/** Restores End portal gravity duplication */
public final class GravityDuplication implements Listener {
    private static final String PROCESSED_TAG = "survivalfriend_gravity_processed";
    private static final double HORIZONTAL_MULTIPLIER = 1.499;
    private static final double VERTICAL_MULTIPLIER = -1.7;
    private final SF plugin;

    public GravityDuplication(SF plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock falling) || !canDuplicate(falling)) return;
        Vector velocity = falling.getVelocity();
        Location location = falling.getLocation();
        double x = Math.abs(velocity.getX());
        double y = Math.abs(velocity.getY());
        double z = Math.abs(velocity.getZ());
        if (x > y && x > z) location.add(Math.signum(velocity.getX()), 0, 0);
        else if (y > z) location.add(0, Math.signum(velocity.getY()), 0);
        else location.add(0, 0, Math.signum(velocity.getZ()));

        Block portal = ownedBlock(location);
        if (portal != null && portal.getType() == Material.END_PORTAL) spawnDuplicate(falling, portal, false);
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

        FallingBlock duplicate = portal.getWorld().spawnFallingBlock(
                portal.getLocation().add(0.5, 0.08, 0.5), original.getBlockData());
        // Both event paths can fire for one source. Neither the source nor its copy may duplicate again.
        original.addScoreboardTag(PROCESSED_TAG);
        duplicate.addScoreboardTag(PROCESSED_TAG);
        duplicate.setVelocity(velocity);
        plugin.getTasks().later(duplicate, () -> correctPosition(duplicate, velocity), 2L);
    }

    private void correctPosition(Entity duplicate, Vector velocity) {
        if (!duplicate.isValid() || !PermissionManager.checkEnabled(MenuSF.MenuItems.GRAVITY_DUPLICATION)) return;
        plugin.getTasks().teleport(duplicate, duplicate.getLocation().add(0, 0.5, 0))
                .thenAccept(success -> {
                    if (success) plugin.getTasks().execute(duplicate, () -> {
                        if (duplicate.isValid()
                                && PermissionManager.checkEnabled(MenuSF.MenuItems.GRAVITY_DUPLICATION)) {
                            duplicate.setVelocity(velocity);
                        }
                    });
                }).exceptionally(error -> {
                    plugin.getLogger().warning("Could not adjust duplicated gravity block: " + error.getMessage());
                    return null;
                });
    }
}
