package org.zkaleejoo.grave;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class DeathInventoryReconciler {

    private DeathInventoryReconciler() {
    }

    @SuppressWarnings("null")
    public static ItemStack[] removeSnapshotItems(ItemStack[] currentItems, List<ItemStack> snapshotItems) {
        ItemStack[] reconciled = cloneItems(currentItems);
        List<StackAmount<ItemStack>> current = IntStream.range(0, reconciled.length)
                .mapToObj(slot -> new StackAmount<>(
                        reconciled[slot],
                        isEmpty(reconciled[slot]) ? 0 : reconciled[slot].getAmount()))
                .toList();
        List<StackAmount<ItemStack>> snapshot = snapshotItems.stream()
                .map(item -> new StackAmount<>(item, isEmpty(item) ? 0 : item.getAmount()))
                .toList();
        int[] remainingAmounts = calculateRemainingAmounts(current, snapshot, ItemStack::isSimilar);

        for (int slot = 0; slot < reconciled.length; slot++) {
            if (remainingAmounts[slot] <= 0) {
                reconciled[slot] = null;
            } else if (reconciled[slot] != null) {
                reconciled[slot].setAmount(remainingAmounts[slot]);
            }
        }

        return reconciled;
    }

    static <T> int[] calculateRemainingAmounts(
            List<StackAmount<T>> currentItems,
            List<StackAmount<T>> snapshotItems,
            BiPredicate<T, T> similarity) {
        int[] remainingAmounts = currentItems.stream()
                .mapToInt(item -> Math.max(item.amount(), 0))
                .toArray();

        for (StackAmount<T> snapshotItem : snapshotItems) {
            if (snapshotItem.item() == null || snapshotItem.amount() <= 0) {
                continue;
            }

            int remainingSnapshotAmount = snapshotItem.amount();
            for (int slot = 0; slot < currentItems.size() && remainingSnapshotAmount > 0; slot++) {
                StackAmount<T> currentItem = currentItems.get(slot);
                if (currentItem.item() == null
                        || remainingAmounts[slot] <= 0
                        || !similarity.test(currentItem.item(), snapshotItem.item())) {
                    continue;
                }

                int removed = Math.min(remainingAmounts[slot], remainingSnapshotAmount);
                remainingAmounts[slot] -= removed;
                remainingSnapshotAmount -= removed;
            }
        }

        return remainingAmounts;
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] clones = new ItemStack[items.length];
        for (int slot = 0; slot < items.length; slot++) {
            clones[slot] = items[slot] == null ? null : items[slot].clone();
        }
        return clones;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    record StackAmount<T>(T item, int amount) {
    }
}
