package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.compat.ServerApi;
import de.laurinhummel.survivalfriend.compat.ServerPlatform;
import de.laurinhummel.survivalfriend.compat.PaperExploitSettings;
import de.laurinhummel.survivalfriend.managers.MenuClickManager;
import de.laurinhummel.survivalfriend.managers.ConfigAccess;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.laurinhummel.survivalfriend.commands.MenuSF.MenuItems.*;

public class MenuSF implements CommandExecutor, Listener {

    private static final class AdminMenu implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;
        private AdminMenu(UUID owner) { this.owner = owner; }
        @Override public Inventory getInventory() { return inventory; }
    }

    public enum MenuItems{
        CRAFTING_TABLE(12, "utils.wb"),
        PATHFINDER(13, "utils.compass"),
        ENDER_CHEST(14, "utils.ec"),
        CREEPER_EXPLOSIONS(29, "creeper.explosion"),
        CREEPER_DAMAGE(30, "creeper.damage"),
        TRAMPLING_PLAYER(32, "trampling.player"),
        TRAMPLING_MOB(33, "trampling.mob"),
        SOLO_PORTAL_FARM(19, "spf.enable"),
        HARDER_DRAGON(40, "hd.enable"),
        TOTEM_MSG(22, "totem.enable"),
        TPA(25, "tpa.enable"),
        RULES(45, "rules.enable"),
        BLOCK_TRANSFORMATION(38, "block-transformation.enable"),
        GRAVITY_DUPLICATION(44, "gravity-duplication.enable"),
        PEARL_STASIS(52, "pearl-stasis.enable");

        private final int position;
        private final String path;
        MenuItems(int position, String path) {
            this.position = position;
            this.path = path; }

        public int getPosition() { return position; }
        public String getPath() { return path; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command is for players only!");
            return true;
        }

        AdminMenu holder = new AdminMenu(player.getUniqueId());
        Inventory menu = Bukkit.createInventory(holder, 6 * 9, player.isOp() ? "MenuSF" : "MenuSF (Read Only)");
        holder.inventory = menu;

        for(int i : new int[]{0,1,9,7,8,17,36,46,44,53,52}) {
            menu.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, "PLACEHOLDER", false, ""));
        }

        // BASIC SURVIVOR STUFF
        menu.setItem(CRAFTING_TABLE.position, createItem(Material.CRAFTING_TABLE, "CRAFTING TABLE", false,
                ChatColor.AQUA + "This toggles the /wb command",
                ChatColor.GRAY + "Status: " + getStatus(CRAFTING_TABLE.path)));
        menu.setItem(PATHFINDER.position, createItem(Material.COMPASS, "PATHFINDER", false,
                ChatColor.AQUA + "This toggles the /compass command",
                ChatColor.GRAY + "Status: " + getStatus(PATHFINDER.path)));
        menu.setItem(ENDER_CHEST.position, createItem(Material.ENDER_CHEST, "ENDER CHEST", false,
                ChatColor.AQUA + "This toggles the /ec command",
                ChatColor.GRAY + "Status: " + getStatus(ENDER_CHEST.path)));

        // ANTI CREEPER
        menu.setItem(CREEPER_EXPLOSIONS.position, createItem(Material.CREEPER_HEAD, "CREEPER EXPLOSIONS", false,
                ChatColor.AQUA + "Enable creeper block-explosion prevention",
                ChatColor.GRAY + "Status: " + getStatus(CREEPER_EXPLOSIONS.path)));
        menu.setItem(CREEPER_DAMAGE.position, createItem(Material.TNT, "EXPLOSION DAMAGE", false,
                ChatColor.AQUA + "Enable creeper damage to player prevention",
                ChatColor.GRAY + "Status: " + getStatus(CREEPER_DAMAGE.path)));

        // CROP TRAMPLING
        menu.setItem(TRAMPLING_PLAYER.position, createItem(Material.LEATHER_BOOTS, "PLAYER TRAMPLING", false,
                ChatColor.AQUA + "Enable player crop trampling prevention",
                ChatColor.GRAY + "Status: " + getStatus(TRAMPLING_PLAYER.path)));
        menu.setItem(TRAMPLING_MOB.position, createItem(Material.CHAINMAIL_BOOTS, "MOB TRAMPLING", false,
                ChatColor.AQUA + "Enable mob crop trampling prevention",
                ChatColor.GRAY + "Status: " + getStatus(TRAMPLING_MOB.path)));

        // SOLO PORTAL FARM
        menu.setItem(SOLO_PORTAL_FARM.position, createItem(Material.OBSIDIAN, "SOLO PORTAL FARM", false,
                ChatColor.AQUA + "Enable far-distance-despawn prevention",
                ChatColor.GRAY + "Status: " + getStatus(SOLO_PORTAL_FARM.path)));

        // TPA
        menu.setItem(TPA.position, createItem(Material.ENDER_PEARL, "TPA", false,
                ChatColor.AQUA + "Enable players to send /tpa requests",
                ChatColor.GRAY + "Status: " + getStatus(TPA.path)));

        // HARDER DRAGONS
        menu.setItem(HARDER_DRAGON.position, createItem(Material.DRAGON_EGG, "HARDER DRAGONS", false,
                ChatColor.AQUA + "Raise the dragons health to " + ChatColor.GOLD + ConfigAccess.integer(SF.getPlugin(), "hd.health") + ChatColor.AQUA + " HP?",
                ChatColor.GRAY + "Status: " + getStatus(HARDER_DRAGON.path)));

        // TOTEM MSG
        menu.setItem(TOTEM_MSG.position, createItem(Material.TOTEM_OF_UNDYING, "TOTEM POP MESSAGE", false,
                ChatColor.AQUA + "Displays a message on totem pop",
                ChatColor.GRAY + "Status: " + getStatus(TOTEM_MSG.path)));

        menu.setItem(RULES.position, createItem(Material.BOOK, "SERVER RULES", false,
                ChatColor.AQUA + "Enable /rules and require players to accept them",
                ChatColor.GRAY + "Status: " + getStatus(RULES.path)));

        menu.setItem(BLOCK_TRANSFORMATION.position, createItem(Material.CAULDRON, "BLOCK TRANSFORMATION", false,
                ChatColor.AQUA + "Drop concrete powder into a water cauldron for concrete",
                ChatColor.AQUA + "Smelt concrete powder in a furnace for stained glass",
                ChatColor.GRAY + "Keeps the color and amount; no cauldron water used",
                ChatColor.GRAY + "Normal furnaces only; consumes fuel as usual",
                ChatColor.GRAY + "Status: " + getStatus(BLOCK_TRANSFORMATION.path)));

        if (ServerPlatform.isFolia()) {
            menu.setItem(GRAVITY_DUPLICATION.position, createItem(Material.SAND, "GRAVITY DUPLICATION", false,
                    ChatColor.AQUA + "Enable gravity block duplication at End portals",
                    ChatColor.GRAY + "Folia only; takes effect immediately",
                    ChatColor.GRAY + "Status: " + getStatus(GRAVITY_DUPLICATION.path)));
            menu.setItem(PEARL_STASIS.position, createItem(Material.ENDER_EYE, "PEARL STASIS FIX", false,
                    ChatColor.AQUA + "Restore Ender pearl stasis chambers",
                    ChatColor.GRAY + "Supports bubble columns and honey chambers",
                    ChatColor.GRAY + "Keeps chamber chunks loaded while enabled",
                    ChatColor.GRAY + "Folia only; saved chambers survive restarts",
                    ChatColor.GRAY + "Status: " + getStatus(PEARL_STASIS.path)));
        }
        if (ServerPlatform.isPaper()) {
            menu.setItem(53, createItem(Material.COMPARATOR, "PAPER EXPLOIT SETTINGS", false, paperSettingsLore()));
        }

        boolean enabled = ConfigAccess.enabled(SF.getPlugin(), "enabled");
        menu.setItem(49, createItem(enabled ? Material.GREEN_TERRACOTTA : Material.RED_TERRACOTTA,
                "PLUGIN " + (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"), enabled));

        player.openInventory(menu);

        return true;
    }

    private String getStatus(String type) {
        return ConfigAccess.read(SF.getPlugin(), config -> {
            if(config.getBoolean(type) || config.getInt(type) == 1) return ChatColor.GREEN + "ENABLED";
            if(config.getInt(type) == 2) return ChatColor.YELLOW + "ADMINS ONLY";
            return ChatColor.RED + "DISABLED";
        });
    }

    private String[] paperSettingsLore() {
        Map<PaperExploitSettings.Setting, Boolean> settings = PaperExploitSettings.read();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "paper-global.yml > unsupported-settings");
        lore.add("");
        for (PaperExploitSettings.Setting setting : PaperExploitSettings.Setting.values()) {
            Boolean enabled = settings.get(setting);
            String status = enabled == null ? ChatColor.RED + "UNKNOWN"
                    : enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
            lore.add((Boolean.TRUE.equals(enabled) ? ChatColor.GREEN : ChatColor.RED) + setting.getKey() + ": " + status);
            lore.add(ChatColor.GRAY + setting.getDescription());
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Unsafe End portal teleportation alone");
        lore.add(ChatColor.YELLOW + "does not fix sand duplication on Folia.");
        lore.add(ChatColor.GRAY + "Active values; restart after config changes.");
        lore.add(ChatColor.GRAY + "Click to refresh this status display.");
        return lore.toArray(String[]::new);
    }

    private ItemStack createItem(Material material, String name, boolean enchanted, String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta itemMeta = item.getItemMeta();
            assert itemMeta != null;
            itemMeta.setDisplayName(ChatColor.GOLD + name);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if(enchanted) itemMeta.addEnchant(ServerApi.POWER, 1, true);
            itemMeta.setLore(Arrays.asList(lore));
        item.setItemMeta(itemMeta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof AdminMenu holder)) return;
        e.setCancelled(true);
        if (!e.getWhoClicked().isOp()) return;
        Inventory menu = holder.inventory;
        if (!holder.owner.equals(e.getWhoClicked().getUniqueId())) return;
        if (e.getRawSlot() < 0 || e.getRawSlot() >= menu.getSize() || e.getClick() != ClickType.LEFT) return;

        final ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        MenuClickManager clickManager = new MenuClickManager(e.getRawSlot());
            clickManager.updateConfig();

        Player p = (Player) e.getWhoClicked();
        SF.getPlugin().getTasks().later(p, () -> {
            if (p.isOnline() && ServerApi.topInventory(p.getOpenInventory()).equals(menu)) p.performCommand("menusf");
        });
    }

    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof AdminMenu) {
            e.setCancelled(true);
        }
    }
}
