package de.laurinhummel.survivalfriend.managers;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class TpaRequestsTest {
    private final AtomicLong clock = new AtomicLong(1000);
    private final TpaRequests requests = new TpaRequests(clock::get, 120_000, 3_600_000);
    private final UUID sender = UUID.randomUUID();
    private final UUID recipient = UUID.randomUUID();

    @Test
    public void onlyIntendedRecipientCanAcceptAndSuccessConsumesRequest() {
        assertTrue(requests.send(sender, recipient));
        assertFalse(requests.isPending(recipient, sender));
        assertFalse(requests.isPending(sender, UUID.randomUUID()));
        assertTrue(requests.isPending(sender, recipient));
        requests.complete(sender);
        assertFalse(requests.isPending(sender, recipient));
    }

    @Test
    public void selfRequestsAreRejectedWithoutCooldown() {
        assertFalse(requests.send(sender, sender));
        assertEquals(0, requests.remainingCooldownMillis(sender));
    }

    @Test
    public void requestExpiresAtExactBoundary() {
        requests.send(sender, recipient);
        clock.addAndGet(119_999);
        assertTrue(requests.isPending(sender, recipient));
        clock.incrementAndGet();
        assertFalse(requests.isPending(sender, recipient));
    }

    @Test
    public void cooldownPreventsReplacementAndExpiresPrecisely() {
        requests.send(sender, recipient);
        assertFalse(requests.send(sender, UUID.randomUUID()));
        assertTrue(requests.isPending(sender, recipient));
        clock.addAndGet(3_599_999);
        assertEquals(1, requests.remainingCooldownMillis(sender));
        assertFalse(requests.send(sender, recipient));
        clock.incrementAndGet();
        assertTrue(requests.send(sender, recipient));
    }

    @Test
    public void disconnectClearsBothDirectionsWithoutBypassingCooldown() {
        UUID other = UUID.randomUUID();
        requests.send(sender, recipient);
        requests.send(recipient, other);
        requests.disconnect(recipient);
        assertFalse(requests.isPending(sender, recipient));
        assertFalse(requests.isPending(recipient, other));
        assertTrue(requests.remainingCooldownMillis(recipient) > 0);
    }

    @Test
    public void asynchronousAcceptanceCanOnlyBeReservedOnceAndFailureAllowsRetry() {
        requests.send(sender, recipient);
        TpaRequests.Request attempt = requests.begin(sender, recipient);
        assertNotNull(attempt);
        assertNull(requests.begin(sender, recipient));
        assertTrue(requests.isActive(attempt));
        requests.finish(attempt, false);
        TpaRequests.Request retry = requests.begin(sender, recipient);
        assertNotNull(retry);
        requests.finish(retry, true);
        assertFalse(requests.isPending(sender, recipient));
    }

    @Test
    public void oldTeleportCompletionCannotConsumeNewRequestAfterDisconnect() {
        TpaRequests noCooldown = new TpaRequests(clock::get, 120_000, 0);
        noCooldown.send(sender, recipient);
        TpaRequests.Request old = noCooldown.begin(sender, recipient);
        assertFalse(noCooldown.send(sender, UUID.randomUUID()));
        noCooldown.disconnect(recipient);
        assertFalse(noCooldown.isActive(old));
        assertTrue(noCooldown.send(sender, recipient));
        noCooldown.finish(old, true);
        assertTrue(noCooldown.isPending(sender, recipient));
    }

    @Test
    public void reservationExpiresBeforeScheduledTeleportStarts() {
        requests.send(sender, recipient);
        TpaRequests.Request attempt = requests.begin(sender, recipient);
        clock.addAndGet(120_000);
        assertFalse(requests.isActive(attempt));
        requests.finish(attempt, false);
        assertNull(requests.begin(sender, recipient));
    }

    @Test @org.junit.jupiter.api.Timeout(5)
    public void simultaneousRegionRequestsRespectCooldownAndReserveOnlyOnce() throws Exception {
        ExecutorService regions = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch ready = new CountDownLatch(8);
            CountDownLatch start = new CountDownLatch(1);
            java.util.List<Future<Boolean>> sends = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) sends.add(regions.submit(() -> {
                ready.countDown();
                start.await();
                return requests.send(sender, recipient);
            }));
            assertTrue(ready.await(1, TimeUnit.SECONDS));
            start.countDown();
            int sent = 0;
            for (Future<Boolean> result : sends) if (result.get(1, TimeUnit.SECONDS)) sent++;
            assertEquals(1, sent);
            java.util.List<Future<TpaRequests.Request>> accepts = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) accepts.add(regions.submit(() -> requests.begin(sender, recipient)));
            int reserved = 0;
            for (Future<TpaRequests.Request> result : accepts) if (result.get(1, TimeUnit.SECONDS) != null) reserved++;
            assertEquals(1, reserved);
        } finally {
            regions.shutdownNow();
        }
    }
}
