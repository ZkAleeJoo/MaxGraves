package org.zkaleejoo.grave;

import java.util.Objects;

class GraveTeleportWarmupPolicy {

    private GraveTeleportWarmupPolicy() {
    }

    static boolean shouldCancelForMove(boolean cancelOnMove,
            String fromWorld, int fromX, int fromY, int fromZ,
            String toWorld, int toX, int toY, int toZ) {
        if (!cancelOnMove) {
            return false;
        }

        return !Objects.equals(fromWorld, toWorld)
                || fromX != toX
                || fromY != toY
                || fromZ != toZ;
    }
}
