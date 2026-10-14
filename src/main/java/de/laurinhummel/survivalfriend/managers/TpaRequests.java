package de.laurinhummel.survivalfriend.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** UUIDs avoid retaining disconnected Player instances. */
public final class TpaRequests {
    public static final class Request {
        private final UUID sender;
        private final UUID recipient;
        private final long expiresAt;
        private boolean accepting;
        private Request(UUID sender, UUID recipient, long expiresAt) {
            this.sender = sender;
            this.recipient = recipient;
            this.expiresAt = expiresAt;
        }
    }
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

    public synchronized long remainingCooldownMillis(UUID sender) {
        return Math.max(0, cooldowns.getOrDefault(sender, 0L) - clock.getAsLong());
    }

    public synchronized boolean send(UUID sender, UUID recipient) {
        long now = clock.getAsLong();
        requests.values().removeIf(request -> !request.accepting && request.expiresAt <= now);
        cooldowns.values().removeIf(expiresAt -> expiresAt <= now);
        if (sender.equals(recipient) || remainingCooldownMillis(sender) > 0
                || (requests.containsKey(sender) && requests.get(sender).accepting)) return false;
        requests.put(sender, new Request(sender, recipient, now + timeoutMillis));
        cooldowns.put(sender, now + cooldownMillis);
        return true;
    }

    public synchronized boolean isPending(UUID sender, UUID recipient) {
        Request request = requests.get(sender);
        if (request == null) return false;
        if (request.expiresAt <= clock.getAsLong()) {
            if (!request.accepting) requests.remove(sender);
            return false;
        }
        return request.recipient.equals(recipient);
    }

    public synchronized void complete(UUID sender) { requests.remove(sender); }

    /** Reserve a specific request before starting an asynchronous teleport. */
    public synchronized Request begin(UUID sender, UUID recipient) {
        if (!isPending(sender, recipient)) return null;
        Request request = requests.get(sender);
        if (request.accepting) return null;
        request.accepting = true;
        return request;
    }

    public synchronized boolean isActive(Request request) {
        return requests.get(request.sender) == request && request.accepting && request.expiresAt > clock.getAsLong();
    }

    /** An old callback must never consume a newer request after a disconnect/rejoin. */
    public synchronized void finish(Request request, boolean success) {
        if (requests.get(request.sender) != request) return;
        if (success) requests.remove(request.sender);
        else request.accepting = false;
    }

    public synchronized void disconnect(UUID player) {
        requests.remove(player);
        requests.values().removeIf(request -> request.recipient.equals(player));
    }
}
