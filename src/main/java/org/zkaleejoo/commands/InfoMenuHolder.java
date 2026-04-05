package org.zkaleejoo.commands;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class InfoMenuHolder implements InventoryHolder {

    private final UUID viewerId;

    public InfoMenuHolder(UUID viewerId) {
        this.viewerId = viewerId;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}