package de.laurinhummel.survivalfriend;

import de.laurinhummel.survivalfriend.commands.MenuSF;
import de.laurinhummel.survivalfriend.commands.Compass;
import de.laurinhummel.survivalfriend.commands.Rules;
import de.laurinhummel.survivalfriend.commands.TPA;
import de.laurinhummel.survivalfriend.events.HarderDragon;
import de.laurinhummel.survivalfriend.events.TotemPop;
import de.laurinhummel.survivalfriend.managers.PermissionManager;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.*;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RegressionTest {
    private SF plugin;
    private YamlConfiguration config;
    private MockedStatic<SF> sf;
    private MockedStatic<Bukkit> bukkit;
    private Inventory menu;
    private Player player;
    private final List<Runnable> tasks = new ArrayList<>();

    @Before
    public void setup() {
        plugin = mock(SF.class);
        when(plugin.getName()).thenReturn("SurvivalFriend");
        config = new YamlConfiguration();
        config.set("enabled", true);
        config.set("utils.wb", 3);
        when(plugin.getConfig()).thenReturn(config);
        sf = mockStatic(SF.class);
        sf.when(SF::getPlugin).thenReturn(plugin);
        bukkit = mockStatic(Bukkit.class);
        menu = mock(Inventory.class);
        when(menu.getSize()).thenReturn(54);
        bukkit.when(() -> Bukkit.createInventory(isNull(), eq(54), eq("MenuSF"))).thenReturn(menu);
        player = mock(Player.class);
        when(player.isOp()).thenReturn(true);
        when(player.isOnline()).thenReturn(true);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        doAnswer(call -> { tasks.add(call.getArgument(1)); return null; })
                .when(scheduler).runTask(eq(plugin), any(Runnable.class));
    }

    @After
    public void cleanup() {
        if (bukkit != null) bukkit.close();
        if (sf != null) sf.close();
    }

    private InventoryClickEvent menuClick(int slot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(menu);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(slot);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PAPER);
        when(event.getCurrentItem()).thenReturn(item);
        return event;
    }

    @Test
    public void ownInventoryCannotChangeAdminSettings() {
        MenuSF menu = new MenuSF();
        InventoryClickEvent event = menuClick(54 + 12);
        when(event.getSlot()).thenReturn(12);
        menu.onInventoryClick(event);
        verify(event).setCancelled(true);
        verify(plugin, never()).saveConfig();
        assertEquals(3, config.getInt("utils.wb"));
    }

    @Test
    public void adminPermissionIsRecheckedForEveryClick() {
        MenuSF menu = new MenuSF();
        when(player.isOp()).thenReturn(false);
        menu.onInventoryClick(menuClick(12));
        assertEquals(3, config.getInt("utils.wb"));
        verify(plugin, never()).saveConfig();
    }

    @Test
    public void authorizedTopSlotCyclesSettingAndDefersRefresh() {
        new MenuSF().onInventoryClick(menuClick(12));
        assertEquals(1, config.getInt("utils.wb"));
        verify(plugin).saveConfig();
        verify(player, never()).performCommand(anyString());
        assertEquals(1, tasks.size());
    }

    @Test
    public void disabledPluginBlocksUtilityCommandsButAllowsAdminMenu() {
        config.set("utils.wb", 1);
        config.set("enabled", false);
        assertFalse(PermissionManager.checkPermission(player, MenuSF.MenuItems.CRAFTING_TABLE));
        assertTrue(PermissionManager.checkPermission(player, null));
    }

    @Test
    public void invalidPermissionValueDoesNotGrantAccess() {
        config.set("utils.wb", 9);
        assertFalse(PermissionManager.checkPermission(player, MenuSF.MenuItems.CRAFTING_TABLE));
    }

    private EnderDragon dragon(double health) {
        EnderDragon dragon = mock(EnderDragon.class);
        AttributeInstance attribute = mock(AttributeInstance.class);
        when(attribute.getValue()).thenReturn(1000.0);
        when(dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH)).thenReturn(attribute);
        when(dragon.getHealth()).thenReturn(health);
        when(dragon.isValid()).thenReturn(true);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        Map<NamespacedKey, Integer> values = new HashMap<>();
        when(dragon.getPersistentDataContainer()).thenReturn(data);
        doAnswer(call -> { values.put(call.getArgument(0), call.getArgument(2)); return null; })
                .when(data).set(any(NamespacedKey.class), eq(PersistentDataType.INTEGER), anyInt());
        when(data.has(any(NamespacedKey.class), eq(PersistentDataType.INTEGER)))
                .thenAnswer(call -> values.containsKey(call.getArgument(0)));
        when(data.getOrDefault(any(NamespacedKey.class), eq(PersistentDataType.INTEGER), anyInt()))
                .thenAnswer(call -> values.getOrDefault(call.getArgument(0), call.getArgument(2)));
        return dragon;
    }

    @Test
    public void disabledDragonFeatureLeavesVanillaDragonUntouched() {
        EnderDragon dragon = dragon(200);
        new HarderDragon().onEntitySpawn(new EntitySpawnEvent(dragon));
        verify(dragon, never()).setHealth(anyDouble());
        verify(dragon, never()).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    public void dragonDamageIsNotAppliedTwiceAndMilestonesArePerDragon() {
        config.set("hd.enable", true);
        config.set("hd.health", 1000);
        HarderDragon listener = new HarderDragon();
        EnderDragon first = dragon(600);
        EnderDragon second = dragon(950);
        listener.onEntitySpawn(new EntitySpawnEvent(first));
        listener.onEntitySpawn(new EntitySpawnEvent(second));
        clearInvocations(first, second);
        listener.onDragonDamage(new EntityDamageEvent(first, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 5));
        listener.onDragonDamage(new EntityDamageEvent(second, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 5));
        new ArrayList<>(tasks).forEach(Runnable::run);
        verify(first, never()).setHealth(anyDouble());
        verify(second, never()).setHealth(anyDouble());
        ArgumentCaptor<PotionEffect> potion = ArgumentCaptor.forClass(PotionEffect.class);
        verify(first).addPotionEffect(potion.capture());
        assertEquals(PotionEffectType.DAMAGE_RESISTANCE, potion.getValue().getType());
        assertEquals(0, potion.getValue().getAmplifier());
        verify(second, never()).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    public void cancelledResurrectionDoesNotBroadcast() {
        config.set("totem.enable", true);
        EntityResurrectEvent event = mock(EntityResurrectEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.isCancelled()).thenReturn(true);
        new TotemPop().onTotemPop(event);
        bukkit.verify(Bukkit::getServer, never());
    }

    @Test
    public void resurrectionWithoutDamageCauseDoesNotCrash() {
        config.set("totem.enable", true);
        Server server = mock(Server.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        when(player.getName()).thenReturn("Player");
        EntityResurrectEvent event = mock(EntityResurrectEvent.class);
        when(event.getEntity()).thenReturn(player);
        new TotemPop().onTotemPop(event);
        verify(server).broadcastMessage(contains("unknown cause"));
    }

    @Test
    public void compassAcceptsCustomItemAndRelativeCoordinates() {
        config.set("utils.compass", 1);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack compass = mock(ItemStack.class);
        when(compass.getType()).thenReturn(Material.COMPASS);
        when(compass.getAmount()).thenReturn(5);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(compass);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 100.5, 70, 20.5));
        new Compass().onCommand(player, null, "compass", new String[]{"~10", "~-5"});
        ArgumentCaptor<Location> location = ArgumentCaptor.forClass(Location.class);
        verify(player).setCompassTarget(location.capture());
        assertEquals(110.5, location.getValue().getX(), 0);
        assertEquals(15.5, location.getValue().getZ(), 0);
        clearInvocations(player);
        new Compass().onCommand(player, null, "compass", new String[]{"NaN", "1"});
        verify(player, never()).setCompassTarget(any(Location.class));
    }

    @Test
    public void tpaHandlesMissingArgumentsOfflinePlayersAndSelfRequests() {
        config.set("tpa.enable", true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        TPA tpa = new TPA(plugin);
        assertTrue(tpa.onCommand(player, null, "tpa", new String[0]));
        assertTrue(tpa.onCommand(player, null, "tpa", new String[]{"Offline"}));
        bukkit.when(() -> Bukkit.getPlayerExact("Self")).thenReturn(player);
        assertTrue(tpa.onCommand(player, null, "tpa", new String[]{"Self"}));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    public void cancelledTpaTeleportCanBeRetried() {
        config.set("tpa.enable", true);
        config.set("tpa.request-timeout-seconds", 120);
        when(plugin.getRules()).thenReturn(mock(Rules.class));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Requester");
        Player recipient = mock(Player.class);
        when(recipient.getUniqueId()).thenReturn(UUID.randomUUID());
        when(recipient.getName()).thenReturn("Recipient");
        when(recipient.isOnline()).thenReturn(true);
        when(recipient.spigot()).thenReturn(mock(Player.Spigot.class));
        World world = mock(World.class);
        when(recipient.getWorld()).thenReturn(world);
        Location destination = new Location(world, 1, 64, 1);
        when(recipient.getLocation()).thenReturn(destination);
        bukkit.when(() -> Bukkit.getPlayerExact("Recipient")).thenReturn(recipient);
        bukkit.when(() -> Bukkit.getPlayerExact("Requester")).thenReturn(player);
        TPA tpa = new TPA(plugin);
        tpa.onCommand(player, null, "tpa", new String[]{"Recipient"});
        tpa.onCommand(recipient, null, "tpa", new String[]{"Requester", "accept"});
        verify(player).teleport(destination);
        when(player.teleport(destination)).thenReturn(true);
        tpa.onCommand(recipient, null, "tpa", new String[]{"Requester", "accept"});
        verify(player, times(2)).teleport(destination);
        tpa.onCommand(recipient, null, "tpa", new String[]{"Requester", "accept"});
        verify(player, times(2)).teleport(destination);
    }
}
