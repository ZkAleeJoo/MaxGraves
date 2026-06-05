package org.zkaleejoo.commands;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class InfoMenuLayout {

    private static final List<Integer> DEFAULT_GRAVE_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43);

    private final int size;
    private final List<Integer> graveSlots;

    public InfoMenuLayout(int configuredSize, List<Integer> configuredGraveSlots) {
        this.size = normalizeSize(configuredSize);
        this.graveSlots = sanitizeSlots(configuredGraveSlots, size);
    }

    public static List<Integer> defaultGraveSlots() {
        return DEFAULT_GRAVE_SLOTS;
    }

    public int size() {
        return size;
    }

    public List<Integer> graveSlots() {
        return graveSlots;
    }

    public int pageCount(int itemCount) {
        if (itemCount <= 0) {
            return 1;
        }

        return Math.max(1, (int) Math.ceil(itemCount / (double) graveSlots.size()));
    }

    public int clampPage(int requestedPage, int itemCount) {
        int maxPage = pageCount(itemCount) - 1;
        return Math.max(0, Math.min(requestedPage, maxPage));
    }

    public List<Integer> itemIndexesForPage(int page, int itemCount) {
        int safePage = clampPage(page, itemCount);
        int startIndex = safePage * graveSlots.size();
        int endIndex = Math.min(startIndex + graveSlots.size(), itemCount);
        List<Integer> indexes = new ArrayList<>();

        for (int index = startIndex; index < endIndex; index++) {
            indexes.add(index);
        }

        return indexes;
    }

    private static int normalizeSize(int configuredSize) {
        if (configuredSize < 9) {
            return 54;
        }

        int normalized = Math.min(54, configuredSize);
        return ((normalized + 8) / 9) * 9;
    }

    private static List<Integer> sanitizeSlots(List<Integer> configuredSlots, int size) {
        Set<Integer> uniqueSlots = new LinkedHashSet<>();
        if (configuredSlots != null) {
            for (Integer slot : configuredSlots) {
                if (slot != null && slot >= 0 && slot < size) {
                    uniqueSlots.add(slot);
                }
            }
        }

        if (uniqueSlots.isEmpty()) {
            return DEFAULT_GRAVE_SLOTS.stream()
                    .filter(slot -> slot < size)
                    .toList();
        }

        return List.copyOf(uniqueSlots);
    }
}
