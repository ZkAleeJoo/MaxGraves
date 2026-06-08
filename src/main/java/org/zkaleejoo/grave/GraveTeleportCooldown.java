package org.zkaleejoo.grave;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class GraveTeleportCooldown {

    private final long cooldownMillis;
    private final Map<UUID, Long> cooldownStartMillisByTarget = new HashMap<>();

    GraveTeleportCooldown(long cooldownMillis) {
        this.cooldownMillis = Math.max(0L, cooldownMillis);
    }

    void startCooldown(UUID targetId, long nowMillis) {
        if (cooldownMillis <= 0L || targetId == null) {
            return;
        }

        cooldownStartMillisByTarget.put(targetId, nowMillis);
    }

    boolean canTeleport(UUID targetId, long nowMillis) {
        return remainingMillis(targetId, nowMillis) <= 0L;
    }

    long remainingSeconds(UUID targetId, long nowMillis) {
        long remainingMillis = remainingMillis(targetId, nowMillis);
        if (remainingMillis <= 0L) {
            return 0L;
        }

        return Math.max(TimeUnit.MILLISECONDS.toSeconds(remainingMillis), 1L);
    }

    void clear() {
        cooldownStartMillisByTarget.clear();
    }

    private long remainingMillis(UUID targetId, long nowMillis) {
        if (cooldownMillis <= 0L || targetId == null) {
            return 0L;
        }

        Long cooldownStartMillis = cooldownStartMillisByTarget.get(targetId);
        if (cooldownStartMillis == null) {
            return 0L;
        }

        long remainingMillis = cooldownMillis - Math.max(nowMillis - cooldownStartMillis, 0L);
        if (remainingMillis <= 0L) {
            cooldownStartMillisByTarget.remove(targetId);
            return 0L;
        }

        return remainingMillis;
    }
}
