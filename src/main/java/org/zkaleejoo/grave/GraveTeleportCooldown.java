package org.zkaleejoo.grave;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class GraveTeleportCooldown {

    private final long cooldownMillis;
    private final Map<UUID, Long> lastCombatMillisByPlayer = new HashMap<>();

    GraveTeleportCooldown(long cooldownMillis) {
        this.cooldownMillis = Math.max(0L, cooldownMillis);
    }

    void recordCombat(UUID playerId, long nowMillis) {
        if (cooldownMillis <= 0L || playerId == null) {
            return;
        }

        lastCombatMillisByPlayer.put(playerId, nowMillis);
    }

    boolean canTeleport(UUID playerId, long nowMillis) {
        return remainingMillis(playerId, nowMillis) <= 0L;
    }

    long remainingSeconds(UUID playerId, long nowMillis) {
        long remainingMillis = remainingMillis(playerId, nowMillis);
        if (remainingMillis <= 0L) {
            return 0L;
        }

        return Math.max(TimeUnit.MILLISECONDS.toSeconds(remainingMillis), 1L);
    }

    void clear() {
        lastCombatMillisByPlayer.clear();
    }

    private long remainingMillis(UUID playerId, long nowMillis) {
        if (cooldownMillis <= 0L || playerId == null) {
            return 0L;
        }

        Long lastCombatMillis = lastCombatMillisByPlayer.get(playerId);
        if (lastCombatMillis == null) {
            return 0L;
        }

        long remainingMillis = cooldownMillis - Math.max(nowMillis - lastCombatMillis, 0L);
        if (remainingMillis <= 0L) {
            lastCombatMillisByPlayer.remove(playerId);
            return 0L;
        }

        return remainingMillis;
    }
}
