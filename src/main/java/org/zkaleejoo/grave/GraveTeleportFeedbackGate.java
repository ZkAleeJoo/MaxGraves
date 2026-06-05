package org.zkaleejoo.grave;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class GraveTeleportFeedbackGate {

    private final long cooldownMillis;
    private final Map<UUID, RecentTeleport> recentTeleports = new HashMap<>();

    GraveTeleportFeedbackGate(long cooldownMillis) {
        this.cooldownMillis = Math.max(0L, cooldownMillis);
    }

    boolean shouldAnnounce(UUID playerId, UUID graveId, long nowMillis) {
        RecentTeleport recentTeleport = recentTeleports.get(playerId);

        if (recentTeleport == null || !recentTeleport.graveId().equals(graveId)) {
            recentTeleports.put(playerId, new RecentTeleport(graveId, nowMillis));
            return true;
        }

        if (nowMillis - recentTeleport.timestampMillis() < cooldownMillis) {
            return false;
        }

        recentTeleports.put(playerId, new RecentTeleport(graveId, nowMillis));
        return true;
    }

    void clear() {
        recentTeleports.clear();
    }

    private record RecentTeleport(UUID graveId, long timestampMillis) {
    }
}
