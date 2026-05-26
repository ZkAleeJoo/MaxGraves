package org.zkaleejoo.grave;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GraveAccessPolicyTest {

    @Test
    void keepsNonPlayerDeathsPrivateWhenPublicKillAccessIsEnabled() {
        assertFalse(GraveAccessPolicy.resolvePublicAccess(true, false));
    }

    @Test
    void keepsPlayerDeathsPrivateWhenPublicKillAccessIsDisabled() {
        assertFalse(GraveAccessPolicy.resolvePublicAccess(false, true));
    }

    @Test
    void makesPlayerDeathsPublicWhenPublicKillAccessIsEnabled() {
        assertTrue(GraveAccessPolicy.resolvePublicAccess(true, true));
    }
}
