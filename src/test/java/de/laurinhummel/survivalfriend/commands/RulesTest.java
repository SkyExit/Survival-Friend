package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.spigotmc.event.entity.EntityMountEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RulesTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private SF plugin;
    private Player player;
    private Rules rules;
    private MockedStatic<Bukkit> bukkit;
    private YamlConfiguration config;
    private InventoryView view;
    private AtomicReference<Inventory> top;
    private final List<Runnable> tasks = new ArrayList<>();

    @Before
    public void setup() throws Exception {
        plugin = mock(SF.class);
        when(plugin.isEnabled()).thenReturn(true);
        config = new YamlConfiguration();
        config.set("enabled", true);
        config.set("regeln", IntStream.rangeClosed(1, 46).mapToObj(i -> "Regel " + i).toList());
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(folder.getRoot());
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        view = mock(InventoryView.class);
        top = new AtomicReference<>(mock(Inventory.class));
        when(view.getTopInventory()).thenAnswer(invocation -> top.get());
        when(player.getOpenInventory()).thenReturn(view);
        doAnswer(invocation -> { top.set(invocation.getArgument(0)); return view; }).when(player).openInventory(any(Inventory.class));
        doAnswer(invocation -> { top.set(mock(Inventory.class)); return null; }).when(player).closeInventory();

        bukkit = mockStatic(Bukkit.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        doAnswer(invocation -> { tasks.add(invocation.getArgument(1)); return null; })
                .when(scheduler).runTask(eq(plugin), any(Runnable.class));
        ItemFactory factory = mock(ItemFactory.class);
        bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
        when(factory.getItemMeta(any(Material.class))).thenAnswer(invocation -> {
            ItemMeta meta = mock(ItemMeta.class);
            when(meta.clone()).thenReturn(meta);
            return meta;
        });
        when(factory.isApplicable(any(ItemMeta.class), any(Material.class))).thenReturn(true);
        when(factory.asMetaFor(any(ItemMeta.class), any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(factory.updateMaterial(any(ItemMeta.class), any(Material.class))).thenAnswer(invocation -> invocation.getArgument(1));
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54), anyString())).thenAnswer(invocation -> {
            Inventory inventory = mock(Inventory.class);
            Map<Integer, ItemStack> contents = new HashMap<>();
            when(inventory.getHolder()).thenReturn(invocation.getArgument(0));
            when(inventory.getSize()).thenReturn(54);
            when(inventory.getItem(anyInt())).thenAnswer(call -> contents.get(call.getArgument(0)));
            doAnswer(call -> { contents.put(call.getArgument(0), call.getArgument(1)); return null; })
                    .when(inventory).setItem(anyInt(), any(ItemStack.class));
            return inventory;
        });
        rules = new Rules(plugin);
    }

    @After
    public void cleanup() { if (bukkit != null) bukkit.close(); }

    private void tick() {
        List<Runnable> pending = new ArrayList<>(tasks);
        tasks.clear();
        pending.forEach(Runnable::run);
    }

    private InventoryClickEvent click(int slot, ClickType type) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(slot);
        when(event.getClick()).thenReturn(type);
        rules.onClick(event);
        verify(event).setCancelled(true);
        tick();
        return event;
    }

    @Test
    public void allPagesAndFinalConsentAreRequiredAndSavedAcrossRestart() {
        rules.onCommand(player, null, "regeln", new String[0]);
        assertNotNull(top.get().getItem(44));
        assertEquals(Material.RED_CONCRETE, top.get().getItem(49).getType());
        for (int i = 0; i < 45; i++) click(i, ClickType.LEFT);
        click(49, ClickType.LEFT);
        assertTrue(rules.requiresAcceptance(player));
        click(53, ClickType.LEFT);
        assertNotNull(top.get().getItem(0));
        assertNull(top.get().getItem(1));
        click(0, ClickType.LEFT);
        assertEquals(Material.LIME_CONCRETE, top.get().getItem(49).getType());
        assertTrue(rules.requiresAcceptance(player));
        click(49, ClickType.LEFT);
        assertFalse(rules.requiresAcceptance(player));
        assertTrue(new File(folder.getRoot(), "accepted-rules.yml").isFile());
        assertFalse(new Rules(plugin).requiresAcceptance(player));
        config.set("regeln", List.of("Neue Regel"));
        assertTrue(new Rules(plugin).requiresAcceptance(player));
    }

    @Test
    public void lowerInventoryAndHotbarClicksCannotConfirmRules() {
        config.set("regeln", List.of("Eine Regel"));
        rules = new Rules(plugin);
        rules.onCommand(player, null, "regeln", new String[0]);
        click(54, ClickType.LEFT);
        click(0, ClickType.NUMBER_KEY);
        click(0, ClickType.SHIFT_LEFT);
        click(49, ClickType.LEFT);
        assertTrue(rules.requiresAcceptance(player));
        assertEquals(Material.RED_CONCRETE, top.get().getItem(49).getType());
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);
        rules.onDrag(drag);
        verify(drag).setCancelled(true);
    }

    @Test
    public void movementTeleportAndMountAreBlockedButLookingIsAllowed() {
        World world = mock(World.class);
        Location from = new Location(world, 1.5, 64, 2.5, 0, 0);
        PlayerMoveEvent move = new PlayerMoveEvent(player, from, new Location(world, 2, 64, 2.5, 90, 10));
        rules.onMove(move);
        assertEquals(from.getX(), move.getTo().getX(), 0);
        assertEquals(90, move.getTo().getYaw(), 0);
        PlayerMoveEvent look = new PlayerMoveEvent(player, from, new Location(world, 1.5, 64, 2.5, 180, 20));
        rules.onMove(look);
        assertFalse(look.isCancelled());
        assertEquals(180, look.getTo().getYaw(), 0);
        PlayerTeleportEvent teleport = new PlayerTeleportEvent(player, from, new Location(world, 100, 64, 100));
        rules.onTeleport(teleport);
        assertTrue(teleport.isCancelled());
        PlayerPortalEvent portal = new PlayerPortalEvent(player, from, new Location(mock(World.class), 0, 64, 0));
        rules.onPortal(portal);
        assertTrue(portal.isCancelled());
        EntityMountEvent mount = new EntityMountEvent(player, mock(org.bukkit.entity.Entity.class));
        rules.onMount(mount);
        assertTrue(mount.isCancelled());
    }

    @Test
    public void closingReopensPendingMenuWithoutResettingPageNavigation() {
        rules.onCommand(player, null, "regeln", new String[0]);
        Inventory firstPage = top.get();
        InventoryCloseEvent close = mock(InventoryCloseEvent.class);
        when(close.getInventory()).thenReturn(firstPage);
        when(close.getPlayer()).thenReturn(player);
        rules.onClose(close);
        click(53, ClickType.LEFT);
        tick();
        assertNotNull(top.get().getItem(45));
        assertNull(top.get().getItem(53));
        Inventory secondPage = top.get();
        when(close.getInventory()).thenReturn(secondPage);
        rules.onClose(close);
        top.set(mock(Inventory.class));
        tick();
        assertNotNull(top.get().getItem(45));
        assertTrue(rules.requiresAcceptance(player));
    }

    @Test
    public void emptyRulesOrDisabledPluginDoNotLockPlayers() {
        config.set("enabled", false);
        assertFalse(rules.requiresAcceptance(player));
        config.set("enabled", true);
        config.set("regeln", Collections.emptyList());
        assertFalse(new Rules(plugin).requiresAcceptance(player));
    }

    @Test
    public void failedPersistenceKeepsPlayerLocked() throws Exception {
        config.set("regeln", List.of("Eine Regel"));
        rules = new Rules(plugin);
        assertTrue(new File(folder.getRoot(), "accepted-rules.yml").mkdir());
        rules.onCommand(player, null, "regeln", new String[0]);
        click(0, ClickType.LEFT);
        click(49, ClickType.LEFT);
        assertTrue(rules.requiresAcceptance(player));
    }
}
