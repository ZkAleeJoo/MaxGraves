package org.zkaleejoo.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class InfoMenuDecorationPolicy {

    private InfoMenuDecorationPolicy() {
    }

    public static List<Integer> fillerSlots(int inventorySize, List<Integer> graveSlots, boolean fillGraveSlots) {
        Set<Integer> reservedSlots = fillGraveSlots ? Set.of() : new HashSet<>(graveSlots);
        List<Integer> slots = new ArrayList<>();

        for (int slot = 0; slot < inventorySize; slot++) {
            if (!reservedSlots.contains(slot)) {
                slots.add(slot);
            }
        }

        return slots;
    }
}
