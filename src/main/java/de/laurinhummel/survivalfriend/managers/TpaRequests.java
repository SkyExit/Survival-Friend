package de.laurinhummel.survivalfriend.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** UUIDs avoid retaining disconnected Player instances. */
public final class TpaRequests {
    private record Request(UUID recipient, long expiresAt) { }
    private final Map<UUID, Request> requests = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final LongSupplier clock;
    private final long timeoutMillis;
    private final long cooldownMillis;

    public TpaRequests(LongSupplier clock, long timeoutMillis, long cooldownMillis) {
        this.clock = clock;
        this.timeoutMillis = Math.max(1, timeoutMillis);
        this.cooldownMillis = Math.max(0, cooldownMillis);
    }

    public long remainingCooldownMillis(UUID sender) {
        return Math.max(0, cooldowns.getOrDefault(sender, 0L) - clock.getAsLong());
    }

    public boolean send(UUID sender, UUID recipient) {
        long now = clock.getAsLong();
        requests.values().removeIf(request -> request.expiresAt <= now);
        cooldowns.values().removeIf(expiresAt -> expiresAt <= now);
        if (sender.equals(recipient) || remainingCooldownMillis(sender) > 0) return false;
        requests.put(sender, new Request(recipient, now + timeoutMillis));
        cooldowns.put(sender, now + cooldownMillis);
        return true;
    }

    public boolean isPending(UUID sender, UUID recipient) {
        Request request = requests.get(sender);
        if (request == null) return false;
        if (request.expiresAt <= clock.getAsLong()) {
            requests.remove(sender);
            return false;
        }
        return request.recipient.equals(recipient);
    }

    public void complete(UUID sender) { requests.remove(sender); }

    public void disconnect(UUID player) {
        requests.remove(player);
        requests.values().removeIf(request -> request.recipient.equals(player));
    }
}
