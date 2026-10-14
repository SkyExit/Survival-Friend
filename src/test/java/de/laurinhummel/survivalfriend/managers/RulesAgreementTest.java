package de.laurinhummel.survivalfriend.managers;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class RulesAgreementTest {
    private final UUID player = UUID.randomUUID();

    @Test
    public void requiresEveryRuleEvenAcrossPages() {
        RulesAgreement agreement = new RulesAgreement(IntStream.range(0, 60).mapToObj(i -> "Rule " + i).toList());
        for (int i = 0; i < 59; i++) agreement.confirm(player, i);
        assertFalse(agreement.allConfirmed(player));
        agreement.confirm(player, 59);
        assertTrue(agreement.allConfirmed(player));
        assertFalse(agreement.hasAccepted(player));
    }

    @Test
    public void duplicateAndInvalidClicksCannotReplaceMissingRules() {
        RulesAgreement agreement = new RulesAgreement(List.of("One", "Two"));
        agreement.confirm(player, 0);
        agreement.confirm(player, 0);
        agreement.confirm(player, -1);
        agreement.confirm(player, 2);
        assertFalse(agreement.allConfirmed(player));
        assertFalse(agreement.allConfirmed(UUID.randomUUID()));
    }

    @Test
    public void acceptanceSurvivesRestartButNotRuleChangesOrReordering() {
        RulesAgreement original = new RulesAgreement(List.of("One", "Two"));
        RulesAgreement restart = new RulesAgreement(List.of("One", "Two"));
        restart.restore(player, original.getRevision());
        assertTrue(restart.hasAccepted(player));
        for (List<String> rules : List.of(List.of("One", "Changed"), List.of("Two", "One"), List.of("One"))) {
            RulesAgreement changed = new RulesAgreement(rules);
            changed.restore(player, original.getRevision());
            assertFalse(changed.hasAccepted(player));
        }
    }

    @Test
    public void ruleBoundariesArePartOfRevision() {
        assertNotEquals(new RulesAgreement(List.of("ab", "c")).getRevision(),
                new RulesAgreement(List.of("a", "bc")).getRevision());
    }

    @Test
    public void quittingClearsPartialConfirmationsButKeepsFinalAcceptance() {
        RulesAgreement agreement = new RulesAgreement(List.of("One"));
        agreement.confirm(player, 0);
        agreement.forgetPending(player);
        assertFalse(agreement.allConfirmed(player));
        agreement.restore(player, agreement.getRevision());
        agreement.forgetPending(player);
        assertTrue(agreement.hasAccepted(player));
    }

    @Test
    public void ignoresBlankRulesAndKeepsAnImmutableCopy() {
        List<String> rules = new ArrayList<>(Arrays.asList(" One ", "", "  ", null));
        RulesAgreement agreement = new RulesAgreement(rules);
        rules.clear();
        assertEquals(List.of("One"), agreement.getRules());
        assertTrue(new RulesAgreement(Collections.emptyList()).allConfirmed(player));
    }

    @Test @org.junit.jupiter.api.Timeout(5)
    public void parallelConfirmationsKeepEveryPlayersConsentSeparate() throws Exception {
        RulesAgreement agreement = new RulesAgreement(IntStream.range(0, 60).mapToObj(i -> "Rule " + i).toList());
        List<UUID> players = IntStream.range(0, 32).mapToObj(i -> UUID.randomUUID()).toList();
        ExecutorService regions = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> confirmations = new ArrayList<>();
            for (UUID id : players) for (int i = 0; i < 60; i++) {
                int index = i;
                confirmations.add(regions.submit(() -> agreement.confirm(id, index)));
            }
            for (Future<?> confirmation : confirmations) confirmation.get(2, TimeUnit.SECONDS);
            for (UUID id : players) {
                assertTrue(agreement.allConfirmed(id));
                assertFalse(agreement.hasAccepted(id));
            }
            agreement.restore(players.get(0), agreement.getRevision());
            assertFalse(agreement.hasAccepted(players.get(1)));
        } finally {
            regions.shutdownNow();
        }
    }
}
