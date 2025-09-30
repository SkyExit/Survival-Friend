package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.managers.MenuClickManager;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

import static de.laurinhummel.survivalfriend.commands.MenuSF.MenuItems.*;

public class MenuSF implements CommandExecutor, Listener {

    private static final Inventory menu = Bukkit.createInventory(null, 6 * 9, "MenuSF");

    public enum MenuItems{
        CRAFTING_TABLE(12, "utils.wb"),
        PATHFINDER(13, "utils.compass"),
        ENDER_CHEST(14, "utils.ec"),
        CREEPER_EXPLOSIONS(29, "creeper.explosion"),
        CREEPER_DAMAGE(30, "creeper.damage"),
        TRAMPLING_PLAYER(32, "trampling.player"),
        TRAMPLING_MOB(33, "trampling.mob"),
        SOLO_PORTAL_FARM(31, "spf.enable");

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
        PermissionManager pm = new PermissionManager(sender, null);
        if(!(pm.check() instanceof Boolean)) { sender.sendMessage((String) pm.check()); return true; }

        for(int i : new int[]{0,1,9,7,8,17,36,45,46,44,53,52}) {
            menu.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, "PLACEHOLDER", false, ""));
        }

        // BASIC SURVIVOR STUFF
        menu.setItem(CRAFTING_TABLE.position, createItem(Material.CRAFTING_TABLE, "CRAFTING TABLE", false,
                ChatColor.AQUA + "This enables the /wb command",
                ChatColor.GRAY + "Status: " + getStatus(CRAFTING_TABLE.path)));
        menu.setItem(PATHFINDER.position, createItem(Material.COMPASS, "PATHFINDER", false,
                ChatColor.AQUA + "This enables the /compass command",
                ChatColor.GRAY + "Status: " + getStatus(PATHFINDER.path)));
        menu.setItem(ENDER_CHEST.position, createItem(Material.ENDER_CHEST, "ENDER CHEST", false,
                ChatColor.AQUA + "This enables the /ec command",
                ChatColor.GRAY + "Status: " + getStatus(ENDER_CHEST.path)));

        // ANTI CREEPER
        menu.setItem(CREEPER_EXPLOSIONS.position, createItem(Material.CREEPER_HEAD, "CREEPER EXPLOSIONS", false,
                ChatColor.AQUA + "Should creepers be able to destroy blocks?",
                ChatColor.GRAY + "Status: " + getStatus(CREEPER_EXPLOSIONS.path)));
        menu.setItem(CREEPER_DAMAGE.position, createItem(Material.TNT, "EXPLOSION DAMAGE", false,
                ChatColor.AQUA + "Should creepers be able to damage players?",
                ChatColor.GRAY + "Status: " + getStatus(CREEPER_DAMAGE.path)));

        // CROP TRAMPLING
        menu.setItem(TRAMPLING_PLAYER.position, createItem(Material.LEATHER_BOOTS, "PLAYER TRAMPLING", false,
                ChatColor.AQUA + "Should players be able to destroy farmland?",
                ChatColor.GRAY + "Status: " + getStatus(TRAMPLING_PLAYER.path)));
        menu.setItem(TRAMPLING_MOB.position, createItem(Material.CHAINMAIL_BOOTS, "MOB TRAMPLING", false,
                ChatColor.AQUA + "Should mobs be able to destroy farmland?",
                ChatColor.GRAY + "Status: " + getStatus(TRAMPLING_MOB.path)));

        // SOLO PORTAL FARM
        menu.setItem(SOLO_PORTAL_FARM.position, createItem(Material.OBSIDIAN, "SOLO PORTAL FARM", false,
                ChatColor.AQUA + "Should mobs despawn when far away?",
                ChatColor.GRAY + "Status: " + getStatus(SOLO_PORTAL_FARM.path)));

        menu.setItem(49, createItem(Material.GREEN_TERRACOTTA, ChatColor.GRAY + "PLUGIN " + ChatColor.GREEN + "ENABLED", true));

        Player player = (Player) sender;
            player.openInventory(menu);

        return true;
    }

    private String getStatus(String type) {
        FileConfiguration config = SF.getPlugin().getConfig();
        if(config.getBoolean(type) || config.getInt(type) == 1) { return ChatColor.GREEN + "ENABLED"; }
        if(config.getInt(type) == 2) { return ChatColor.YELLOW + "ADMINS ONLY"; }
        if(!(config.getBoolean(type)) || config.getInt(type) == 3) { return ChatColor.RED + "DISABLED"; }
        return null;
    }

    private ItemStack createItem(Material material, String name, boolean enchanted, String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta itemMeta = item.getItemMeta();
            assert itemMeta != null;
            itemMeta.setDisplayName(ChatColor.GOLD + name);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if(enchanted) itemMeta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
            itemMeta.setLore(Arrays.asList(lore));
        item.setItemMeta(itemMeta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getInventory().equals(menu)) return;
        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        MenuClickManager clickManager = new MenuClickManager(e.getSlot());
            clickManager.updateConfig();

        Player p = (Player) e.getWhoClicked();
            p.performCommand("menusf");
    }

    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(menu)) {
            e.setCancelled(true);
        }
    }
}
