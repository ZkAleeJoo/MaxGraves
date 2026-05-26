package org.zkaleejoo.grave;

import java.util.UUID;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GraveChestHolder implements InventoryHolder {

    private final UUID graveId;
    private Inventory inventory;

    public GraveChestHolder(UUID graveId) {
        this.graveId = graveId;
    }

    public UUID getGraveId() {
        return graveId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
