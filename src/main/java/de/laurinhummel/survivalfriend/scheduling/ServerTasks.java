package de.laurinhummel.survivalfriend.scheduling;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/** Optional Paper APIs are resolved through their public interfaces to retain Spigot support. */
public final class ServerTasks implements AutoCloseable {
    private final Plugin plugin;
    private final Method getScheduler;
    private final Method execute;
    private final Method teleportAsync;
    private final Method ownsEntity;
    private volatile boolean closed;

    public ServerTasks(Plugin plugin) {
        this.plugin = plugin;
        Method scheduler = optionalMethod(Entity.class, "getScheduler");
        getScheduler = scheduler;
        if (scheduler == null) {
            execute = null;
            teleportAsync = null;
            ownsEntity = null;
            // Never silently use Bukkit's scheduler on a broken/unsupported Folia API.
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                throw new IllegalStateException("Folia entity scheduler API is unavailable");
            } catch (ClassNotFoundException ignored) { }
        } else {
            try {
                execute = scheduler.getReturnType().getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
                teleportAsync = Entity.class.getMethod("teleportAsync", Location.class);
                ownsEntity = Bukkit.class.getMethod("isOwnedByCurrentRegion", Entity.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Incomplete Paper/Folia scheduling API", e);
            }
        }
    }

    // Allows contract tests to exercise the optional API while compiling against Spigot.
    ServerTasks(Plugin plugin, Method getScheduler, Method execute, Method teleportAsync, Method ownsEntity) {
        this.plugin = plugin;
        this.getScheduler = getScheduler;
        this.execute = execute;
        this.teleportAsync = teleportAsync;
        this.ownsEntity = ownsEntity;
    }

    public boolean hasEntityScheduler() { return getScheduler != null; }

    public boolean owns(Entity entity) {
        return ownsEntity == null || Boolean.TRUE.equals(invoke(ownsEntity, null, entity));
    }

    /** Use only for immediate work in a command/event's owning region, or to hand off to another entity. */
    public void execute(Entity entity, Runnable task) { execute(entity, task, () -> { }); }

    public void execute(Entity entity, Runnable task, Runnable retired) {
        if (closed || !plugin.isEnabled()) { retired.run(); return; }
        if (!hasEntityScheduler()) task.run();
        else schedule(entity, task, retired);
    }

    /** Always defer inventory changes and post-damage work until the entity's next tick. */
    public void later(Entity entity, Runnable task) {
        if (closed || !plugin.isEnabled()) return;
        if (!hasEntityScheduler()) Bukkit.getScheduler().runTask(plugin, guarded(task));
        else schedule(entity, task, () -> { });
    }

    private void schedule(Entity entity, Runnable task, Runnable retired) {
        try {
            boolean accepted = Boolean.TRUE.equals(invoke(execute, invoke(getScheduler, entity), plugin,
                    (Runnable) () -> {
                        if (closed || !plugin.isEnabled()) retired.run();
                        else task.run();
                    }, retired, 1L));
            if (!accepted) retired.run();
        } catch (RuntimeException e) {
            retired.run();
            if (!closed && plugin.isEnabled()) throw e;
        }
    }

    private Runnable guarded(Runnable task) {
        return () -> { if (!closed && plugin.isEnabled()) task.run(); };
    }

    /** Must be called on the teleported entity's thread; never block on the returned future. */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> teleport(Entity entity, Location destination) {
        if (closed || !plugin.isEnabled()) return CompletableFuture.completedFuture(false);
        try {
            if (teleportAsync != null) return (CompletableFuture<Boolean>) invoke(teleportAsync, entity, destination);
            return CompletableFuture.completedFuture(entity.teleport(destination));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void close() { closed = true; }

    private static Method optionalMethod(Class<?> type, String name) {
        try { return type.getMethod(name); }
        catch (NoSuchMethodException ignored) { return null; }
    }

    private static Object invoke(Method method, Object receiver, Object... args) {
        try { return method.invoke(receiver, args); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            if (e.getCause() instanceof Error cause) throw cause;
            throw new IllegalStateException("Server API failed: " + method.getName(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot invoke server API: " + method.getName(), e);
        }
    }
}
