package de.laurinhummel.survivalfriend.managers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Tracks consent to the complete, ordered list of rules, independently of Bukkit. */
public final class RulesAgreement {
    private final List<String> rules;
    private final String revision;
    private final Map<UUID, String> accepted = new HashMap<>();
    private final Map<UUID, Set<Integer>> confirmed = new HashMap<>();

    public RulesAgreement(List<String> rules) {
        this.rules = rules.stream().filter(Objects::nonNull).map(String::trim)
                .filter(rule -> !rule.isEmpty()).toList();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String rule : this.rules) {
                byte[] bytes = rule.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            revision = HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public List<String> getRules() { return rules; }
    public String getRevision() { return revision; }
    public boolean hasAccepted(UUID player) { return revision.equals(accepted.get(player)); }
    public void restore(UUID player, String revision) { accepted.put(player, revision); }
    public void forgetPending(UUID player) { confirmed.remove(player); }

    public boolean isConfirmed(UUID player, int index) {
        return hasAccepted(player) || confirmed.getOrDefault(player, Collections.emptySet()).contains(index);
    }

    public void confirm(UUID player, int index) {
        if (index >= 0 && index < rules.size()) {
            confirmed.computeIfAbsent(player, ignored -> new HashSet<>()).add(index);
        }
    }

    public boolean allConfirmed(UUID player) {
        return hasAccepted(player) || confirmed.getOrDefault(player, Collections.emptySet()).size() == rules.size();
    }
}
