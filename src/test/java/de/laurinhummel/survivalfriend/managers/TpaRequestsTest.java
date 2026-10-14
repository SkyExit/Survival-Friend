package de.laurinhummel.survivalfriend.managers;

import org.junit.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

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
}
