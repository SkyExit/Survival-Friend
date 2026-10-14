package de.laurinhummel.survivalfriend.commands;

import de.laurinhummel.survivalfriend.SF;
import de.laurinhummel.survivalfriend.TestServerFixtures;
import de.laurinhummel.survivalfriend.scheduling.ServerTasks;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Simulate distinct entity contexts; foreign player locations/inventories must never be read. */
public class FoliaCommandsTest {
    private record Task(Entity owner, Runnable work) { }
    private final Queue<Task> pending = new ArrayDeque<>();
    private Entity owner;
    private SF plugin;
    private ServerTasks tasks;
    private Player requester;
    private Player recipient;
    private Location destination;
    private CompletableFuture<Boolean> teleport;
    private TPA tpa;
    private MockedStatic<SF> sf;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    public void setup() throws Exception {
        plugin = mock(SF.class);
        tasks = mock(ServerTasks.class);
        when(plugin.getTasks()).thenReturn(tasks);
        when(plugin.getRules()).thenReturn(mock(Rules.class));
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        YamlConfiguration config = new YamlConfiguration();
        config.set("enabled", true);
        config.set("tpa.enable", true);
        config.set("tpa.request-timeout-seconds", 120);
        config.set("utils.compass", 1);
        when(plugin.getConfig()).thenReturn(config);
        requester = player("Requester");
        recipient = player("Recipient");
        World world = mock(World.class);
        destination = new Location(world, 12000, 70, -12000);
        when(recipient.getLocation()).thenAnswer(call -> { assertSame(recipient, owner); return destination; });
        when(requester.getLocation()).thenAnswer(call -> { assertSame(requester, owner); return destination; });
        when(requester.getWorld()).thenAnswer(call -> { assertSame(requester, owner); return world; });
        teleport = new CompletableFuture<>();
        when(tasks.teleport(eq(requester), any(Location.class))).thenAnswer(call -> {
            assertSame(requester, owner);
            return teleport;
        });
        doAnswer(call -> { pending.add(new Task(call.getArgument(0), call.getArgument(1))); return null; })
                .when(tasks).execute(any(Entity.class), any(Runnable.class));
        doAnswer(call -> { pending.add(new Task(call.getArgument(0), call.getArgument(1))); return null; })
                .when(tasks).execute(any(Entity.class), any(Runnable.class), any(Runnable.class));
        sf = mockStatic(SF.class);
        sf.when(SF::getPlugin).thenReturn(plugin);
        bukkit = mockStatic(Bukkit.class);
        TestServerFixtures.registries(bukkit);
        bukkit.when(() -> Bukkit.getPlayerExact("Requester")).thenReturn(requester);
        bukkit.when(() -> Bukkit.getPlayerExact("Recipient")).thenReturn(recipient);
        tpa = new TPA(plugin);
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        when(player.isOnline()).thenReturn(true);
        when(player.spigot()).thenAnswer(call -> {
            assertSame(player, owner);
            return mock(Player.Spigot.class);
        });
        doAnswer(call -> { assertSame(player, owner); return null; }).when(player).sendMessage(anyString());
        return player;
    }

    private void as(Entity entity, Runnable work) {
        Entity previous = owner;
        owner = entity;
        try { work.run(); } finally { owner = previous; }
    }

    private void tick() {
        while (!pending.isEmpty()) {
            Task task = pending.remove();
            as(task.owner, task.work);
        }
    }

    private void send() {
        as(requester, () -> tpa.onCommand(requester, null, "tpa", new String[]{"Recipient"}));
        tick();
    }

    private void accept() {
        as(recipient, () -> tpa.onCommand(recipient, null, "tpa", new String[]{"Requester", "accept"}));
    }

    @AfterEach
    public void cleanup() {
        if (bukkit != null) bukkit.close();
        if (sf != null) sf.close();
    }

    @Test
    public void asyncAcceptanceRunsOnRequesterAndCannotBeDuplicated() {
        send();
        accept();
        accept();
        verify(tasks, never()).teleport(any(Entity.class), any(Location.class));
        tick();
        verify(tasks).teleport(requester, destination);
        accept();
        tick();
        verify(tasks, times(1)).teleport(any(Entity.class), any(Location.class));
        teleport.complete(true); // Completion has no owning entity context.
        tick();
        verify(requester).sendMessage(contains("You have been teleported"));
        verify(recipient).sendMessage(contains("has been teleported to you"));
        accept();
        tick();
        verify(tasks, times(1)).teleport(any(Entity.class), any(Location.class));
        verify(requester, never()).teleport(any(Location.class));
    }

    @Test
    public void cancelledOrExceptionalAsyncTeleportAllowsRetry() {
        send();
        accept();
        tick();
        teleport.complete(false);
        tick();
        teleport = new CompletableFuture<>();
        accept();
        tick();
        teleport.completeExceptionally(new IllegalStateException("Cancelled by server"));
        tick();
        teleport = CompletableFuture.completedFuture(true);
        accept();
        tick();
        verify(tasks, times(3)).teleport(requester, destination);
        verify(requester).sendMessage(contains("You have been teleported"));
    }

    @Test
    public void disconnectBeforeEntityTaskRunsPreventsTeleport() {
        send();
        accept();
        tpa.onQuit(new PlayerQuitEvent(requester, ""));
        tick();
        verify(tasks, never()).teleport(any(Entity.class), any(Location.class));
    }

    @Test
    public void compassReadsTargetInItsRegionAndAppliesResultInOwnersRegion() {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(requester.getInventory()).thenAnswer(call -> { assertSame(requester, owner); return inventory; });
        when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.COMPASS));
        Server server = mock(Server.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        when(server.getPlayerExact("Recipient")).thenReturn(recipient);
        doAnswer(call -> { assertSame(requester, owner); return null; }).when(requester).setCompassTarget(any(Location.class));
        as(requester, () -> new Compass().onCommand(requester, null, "compass", new String[]{"Recipient"}));
        verify(recipient, never()).getLocation();
        tick();
        verify(requester).setCompassTarget(destination);
    }
}
