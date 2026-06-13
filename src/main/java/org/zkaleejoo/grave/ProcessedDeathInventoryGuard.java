package org.zkaleejoo.grave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ProcessedDeathInventoryGuard {

    private final Map<UUID, List<ItemStack>> pendingSnapshots = new HashMap<>();

    public void markPending(UUID playerId, List<ItemStack> snapshotItems) {
        pendingSnapshots.put(playerId, copyItems(snapshotItems));
    }

    public Optional<List<ItemStack>> consume(UUID playerId) {
        List<ItemStack> snapshotItems = pendingSnapshots.remove(playerId);
        return snapshotItems == null ? Optional.empty() : Optional.of(copyItems(snapshotItems));
    }

    private List<ItemStack> copyItems(List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                copies.add(item.clone());
            }
        }
        return copies;
    }
}
