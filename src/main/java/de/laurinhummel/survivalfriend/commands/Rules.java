package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.compat.ServerApi;
import de.laurinhummel.survivalfriend.managers.RulesAgreement;
import de.laurinhummel.survivalfriend.managers.ConfigAccess;
import de.laurinhummel.survivalfriend.misc.SkyLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;

public final class Rules implements CommandExecutor, Listener {
    private static final int PAGE_SIZE = 45;
    private final SF plugin;
    private final RulesAgreement agreement;
    private final File file;
    private final YamlConfiguration data = new YamlConfiguration();

    public Rules(SF plugin) {
        this.plugin = plugin;
        agreement = new RulesAgreement(plugin.getConfig().getStringList("rules.entries"));
        file = new File(plugin.getDataFolder(), "accepted-rules.yml");
        if (file.exists()) {
            try {
                data.load(file);
            } catch (Exception e) {
                throw new IllegalStateException("Could not load accepted-rules.yml", e);
            }
        }
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players != null) {
            for (String id : players.getKeys(false)) {
                try {
                    agreement.restore(UUID.fromString(id), players.getString(id));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ignoring invalid player UUID in accepted-rules.yml: " + id);
                }
            }
        }
    }

    public boolean requiresAcceptance(Player player) {
        return isEnabled() && !agreement.getRules().isEmpty()
                && !agreement.hasAccepted(player.getUniqueId());
    }

    private boolean isEnabled() {
        return ConfigAccess.read(plugin, config -> config.getBoolean("enabled") && config.getBoolean("rules.enable"));
    }

    public void refresh(Player player) {
        if (!player.isOnline()) return;
        if (isEnabled()) {
            prompt(player);
        } else {
            agreement.forgetPending(player.getUniqueId());
            if (ServerApi.topInventory(player.getOpenInventory()).getHolder() instanceof RulesMenu) {
                player.closeInventory();
            }
        }
    }

    public void prompt(Player player) {
        if (!plugin.isEnabled() || !requiresAcceptance(player)) return;
        player.leaveVehicle();
        SkyLogger.sendPlayer(player, "Please accept all server rules with /rules first.", SkyLogger.LogType.WARNING);
        plugin.getTasks().later(player, () -> {
            if (player.isOnline() && !player.isDead() && requiresAcceptance(player)) open(player, 0);
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
        } else if (!isEnabled()) {
            SkyLogger.sendPlayer(player, "The /rules feature is disabled.", SkyLogger.LogType.INFO);
        } else if (agreement.getRules().isEmpty()) {
            SkyLogger.sendPlayer(player, "No server rules have been configured.", SkyLogger.LogType.INFO);
        } else {
            open(player, 0);
        }
        return true;
    }

    private void open(Player player, int page) {
        if (!isEnabled()) return;
        int pages = (agreement.getRules().size() + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.max(0, Math.min(page, pages - 1));
        RulesMenu menu = new RulesMenu(player.getUniqueId(), page);
        menu.inventory = Bukkit.createInventory(menu, 54, "Server rules " + (page + 1) + "/" + pages);
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = page * PAGE_SIZE + slot;
            if (index >= agreement.getRules().size()) break;
            boolean confirmed = agreement.isConfirmed(player.getUniqueId(), index);
            List<String> lore = wrap(agreement.getRules().get(index));
            lore.add("");
            lore.add(confirmed ? ChatColor.GREEN + "Read and confirmed" : ChatColor.YELLOW + "Left-click to confirm this rule");
            menu.inventory.setItem(slot, item(confirmed ? Material.LIME_DYE : Material.PAPER,
                    ChatColor.GOLD + "Rule " + (index + 1), lore));
        }
        if (page > 0) menu.inventory.setItem(45, item(Material.ARROW, "Previous page", Collections.emptyList()));
        if (page + 1 < pages) menu.inventory.setItem(53, item(Material.ARROW, "Next page", Collections.emptyList()));
        boolean accepted = agreement.hasAccepted(player.getUniqueId());
        boolean ready = agreement.allConfirmed(player.getUniqueId());
        menu.inventory.setItem(49, item(ready ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                accepted ? "Already accepted" : "Accept all rules",
                List.of(ready ? ChatColor.GREEN + "Left-click to finish accepting the rules"
                        : ChatColor.YELLOW + "Confirm every rule on every page first.")));
        player.openInventory(menu.inventory);
    }

    private static ItemStack item(Material material, String title, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        meta.setDisplayName(ChatColor.GOLD + title);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : ChatColor.translateAlternateColorCodes('&', text).split("\\R", -1)) {
            String line = ChatColor.GRAY.toString();
            for (String word : paragraph.split("\\s+")) {
                // Split long words as well, so a rule remains readable in an item tooltip.
                while (word.length() > 42) {
                    if (!ChatColor.stripColor(line).isEmpty()) lines.add(line);
                    lines.add(ChatColor.GRAY + word.substring(0, 42));
                    word = word.substring(42);
                    line = ChatColor.GRAY.toString();
                }
                if (ChatColor.stripColor(line + word).length() > 42) {
                    lines.add(line.trim());
                    line = ChatColor.GRAY + ChatColor.getLastColors(line);
                }
                line += word + " ";
            }
            lines.add(line.trim());
        }
        return lines;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(ServerApi.topInventory(event.getView()).getHolder() instanceof RulesMenu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !menu.owner.equals(player.getUniqueId())) return;
        if (!isEnabled()) {
            plugin.getTasks().later(player, () -> refresh(player));
            return;
        }
        if (event.getClick() != ClickType.LEFT || event.getRawSlot() < 0 || event.getRawSlot() >= 54) return;
        int slot = event.getRawSlot();
        if (slot < PAGE_SIZE) {
            agreement.confirm(player.getUniqueId(), menu.page * PAGE_SIZE + slot);
        } else if (slot == 49) {
            if (!agreement.allConfirmed(player.getUniqueId())) {
                SkyLogger.sendPlayer(player, "Please confirm every rule on every page first.", SkyLogger.LogType.WARNING);
                return;
            }
            if (!agreement.hasAccepted(player.getUniqueId()) && !saveAcceptance(player)) return;
            plugin.getTasks().later(player, () -> {
                if (player.isOnline() && ServerApi.topInventory(player.getOpenInventory()) == menu.inventory) player.closeInventory();
            });
            return;
        } else if (slot != 45 && slot != 53) {
            return;
        }
        int page = menu.page + (slot == 45 ? -1 : slot == 53 ? 1 : 0);
        plugin.getTasks().later(player, () -> {
            if (player.isOnline() && ServerApi.topInventory(player.getOpenInventory()) == menu.inventory) open(player, page);
        });
    }

    private synchronized boolean saveAcceptance(Player player) {
        String path = "players." + player.getUniqueId();
        Object previous = data.get(path);
        data.set(path, agreement.getRevision());
        Path temporary = null;
        try {
            temporary = Files.createTempFile(file.toPath().getParent(), "accepted-rules-", ".tmp");
            Files.writeString(temporary, data.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            data.set(path, previous);
            plugin.getLogger().log(Level.SEVERE, "Could not save rules acceptance", e);
            SkyLogger.sendPlayer(player, "Your acceptance could not be saved. Please try again.", SkyLogger.LogType.ERROR);
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not remove temporary rules file", e);
                }
            }
        }
        agreement.restore(player.getUniqueId(), agreement.getRevision());
        agreement.forgetPending(player.getUniqueId());
        SkyLogger.sendPlayer(player, "You have accepted all rules and can now move freely.", SkyLogger.LogType.INFO);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (ServerApi.topInventory(event.getView()).getHolder() instanceof RulesMenu) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!plugin.isEnabled() || !(event.getInventory().getHolder() instanceof RulesMenu menu)
                || !(event.getPlayer() instanceof Player player)) return;
        plugin.getTasks().later(player, () -> {
            if (player.isOnline() && !player.isDead() && requiresAcceptance(player)
                    && !(ServerApi.topInventory(player.getOpenInventory()).getHolder() instanceof RulesMenu)) {
                open(player, menu.page);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { prompt(event.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) { prompt(event.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { agreement.forgetPending(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!requiresAcceptance(event.getPlayer()) || !changesPosition(event)) return;
        Location from = event.getFrom().clone();
        from.setYaw(event.getTo().getYaw());
        from.setPitch(event.getTo().getPitch());
        event.setTo(from);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (requiresAcceptance(event.getPlayer()) && changesPosition(event)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) { onTeleport(event); }

    private static boolean changesPosition(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        return to != null && (from.getWorld() != to.getWorld() || from.getX() != to.getX()
                || from.getY() != to.getY() || from.getZ() != to.getZ());
    }

    public void registerMountListener(PluginManager manager) {
        manager.registerEvent(ServerApi.mountEventType(), this, EventPriority.HIGHEST,
                (listener, event) -> onMount((EntityEvent) event), plugin, true);
    }

    public void onMount(EntityEvent event) {
        if (event instanceof Cancellable cancellable && event.getEntity() instanceof Player player
                && requiresAcceptance(player)) cancellable.setCancelled(true);
    }

    private static final class RulesMenu implements InventoryHolder {
        private final UUID owner;
        private final int page;
        private Inventory inventory;

        private RulesMenu(UUID owner, int page) { this.owner = owner; this.page = page; }
        @Override
        public Inventory getInventory() { return inventory; }
    }
}
