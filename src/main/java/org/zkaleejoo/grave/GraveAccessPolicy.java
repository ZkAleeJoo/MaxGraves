package org.zkaleejoo.grave;

public final class GraveAccessPolicy {

    private GraveAccessPolicy() {
    }

    public static boolean resolvePublicAccess(boolean publicPlayerKillAccessEnabled, boolean killedByPlayer) {
        return publicPlayerKillAccessEnabled && killedByPlayer;
    }
}
