package org.zkaleejoo.grave;

public final class GraveCreationPolicy {

    private GraveCreationPolicy() {
    }

    public static boolean shouldCreateGrave(boolean singleActiveLimitEnabled, boolean playerHasActiveGrave) {
        return !singleActiveLimitEnabled || !playerHasActiveGrave;
    }
}
