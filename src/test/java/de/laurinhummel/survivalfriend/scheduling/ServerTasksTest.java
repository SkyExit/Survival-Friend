package de.laurinhummel.survivalfriend.scheduling;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ServerTasksTest {
    public interface RegionEntity {
        EntityScheduler getScheduler();
        CompletableFuture<Boolean> teleportAsync(Location location);
    }
    public interface EntityScheduler {
        boolean execute(Plugin plugin, Runnable task, Runnable retired, long delay);
    }
    public static boolean ownsEntity(Entity entity) { return false; }

    private Plugin plugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        return plugin;
    }

    private ServerTasks regionTasks(Plugin plugin) throws Exception {
        return new ServerTasks(plugin, RegionEntity.class.getMethod("getScheduler"),
                EntityScheduler.class.getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class),
                RegionEntity.class.getMethod("teleportAsync", Location.class),
                ServerTasksTest.class.getMethod("ownsEntity", Entity.class));
    }

    @Test
    public void spigotExecutesOnMainThreadAndDefersInventoryWork() {
        Plugin plugin = plugin();
        Entity entity = mock(Entity.class);
        ServerTasks tasks = new ServerTasks(plugin);
        assertFalse(tasks.hasEntityScheduler());
        AtomicInteger runs = new AtomicInteger();
        tasks.execute(entity, runs::incrementAndGet);
        assertEquals(1, runs.get());
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            List<Runnable> pending = new ArrayList<>();
            doAnswer(call -> { pending.add(call.getArgument(1)); return null; })
                    .when(scheduler).runTask(eq(plugin), any(Runnable.class));
            tasks.later(entity, runs::incrementAndGet);
            assertEquals(1, runs.get());
            pending.get(0).run();
            assertEquals(2, runs.get());
        }
        Location destination = mock(Location.class);
        when(entity.teleport(destination)).thenReturn(true);
        assertTrue(tasks.teleport(entity, destination).join());
    }

    @Test
    public void regionTasksUseEntitySchedulerAndAsyncTeleportWithoutBukkitFallback() throws Exception {
        Plugin plugin = plugin();
        ServerTasks tasks = regionTasks(plugin);
        Entity entity = mock(Entity.class, withSettings().extraInterfaces(RegionEntity.class));
        RegionEntity api = (RegionEntity) entity;
        EntityScheduler scheduler = mock(EntityScheduler.class);
        when(api.getScheduler()).thenReturn(scheduler);
        List<Runnable> pending = new ArrayList<>();
        when(scheduler.execute(eq(plugin), any(Runnable.class), any(Runnable.class), eq(1L)))
                .thenAnswer(call -> { pending.add(call.getArgument(1)); return true; });
        AtomicInteger runs = new AtomicInteger();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            tasks.execute(entity, runs::incrementAndGet);
            tasks.later(entity, runs::incrementAndGet);
            assertEquals(0, runs.get());
            pending.forEach(Runnable::run);
            assertEquals(2, runs.get());
            assertFalse(tasks.owns(entity));
            Location destination = mock(Location.class);
            CompletableFuture<Boolean> teleport = new CompletableFuture<>();
            when(api.teleportAsync(destination)).thenReturn(teleport);
            assertSame(teleport, tasks.teleport(entity, destination));
            assertFalse(teleport.isDone());
            verify(entity, never()).teleport(any(Location.class));
            bukkit.verify(Bukkit::getScheduler, never());
        }
        tasks.close();
        pending.forEach(Runnable::run);
        assertEquals(2, runs.get());
    }

    @Test
    public void retiredEntitiesAndDisabledPluginReleasePendingWork() throws Exception {
        Plugin plugin = plugin();
        ServerTasks tasks = regionTasks(plugin);
        Entity entity = mock(Entity.class, withSettings().extraInterfaces(RegionEntity.class));
        EntityScheduler scheduler = mock(EntityScheduler.class);
        when(((RegionEntity) entity).getScheduler()).thenReturn(scheduler);
        AtomicInteger retired = new AtomicInteger();
        Runnable work = mock(Runnable.class);
        tasks.execute(entity, work, retired::incrementAndGet);
        assertEquals(1, retired.get());
        verifyNoInteractions(work);
        when(plugin.isEnabled()).thenReturn(false);
        tasks.execute(entity, work, retired::incrementAndGet);
        assertEquals(2, retired.get());
        verifyNoInteractions(work);
    }

    @Test
    public void reflectionFailuresNeverFallBackToSynchronousTeleport() throws Exception {
        ServerTasks tasks = regionTasks(plugin());
        Entity entity = mock(Entity.class, withSettings().extraInterfaces(RegionEntity.class));
        Location destination = mock(Location.class);
        when(((RegionEntity) entity).teleportAsync(destination)).thenThrow(new IllegalStateException("Wrong region"));
        assertTrue(tasks.teleport(entity, destination).isCompletedExceptionally());
        verify(entity, never()).teleport(any(Location.class));
    }
}
