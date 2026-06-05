package org.zkaleejoo.commands;

import java.util.Set;

public final class InfoMenuClickPolicy {

    private InfoMenuClickPolicy() {
    }

    public static boolean isTopInventoryClick(int rawSlot, int topInventorySize) {
        return rawSlot >= 0 && rawSlot < topInventorySize;
    }

    public static boolean touchesTopInventory(Set<Integer> rawSlots, int topInventorySize) {
        return rawSlots.stream().anyMatch(rawSlot -> isTopInventoryClick(rawSlot, topInventorySize));
    }
}
