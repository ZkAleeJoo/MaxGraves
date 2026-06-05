package org.zkaleejoo.commands;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InfoMenuHolder implements InventoryHolder {

    public enum ViewType {
        MAIN,
        DETAILS
    }

    private final UUID viewerId;
    private final ViewType viewType;
    private final int page;
    private final UUID graveId;
    private final Map<Integer, InfoMenuClickAction> clickActions = new HashMap<>();

    public InfoMenuHolder(UUID viewerId, ViewType viewType, int page, UUID graveId) {
        this.viewerId = viewerId;
        this.viewType = viewType;
        this.page = page;
        this.graveId = graveId;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public ViewType getViewType() {
        return viewType;
    }

    public int getPage() {
        return page;
    }

    public UUID getGraveId() {
        return graveId;
    }

    public void registerAction(int slot, InfoMenuAction action) {
        registerAction(slot, action, null);
    }

    public void registerAction(int slot, InfoMenuAction action, UUID actionGraveId) {
        clickActions.put(slot, new InfoMenuClickAction(action, actionGraveId));
    }

    public Optional<InfoMenuClickAction> getAction(int slot) {
        return Optional.ofNullable(clickActions.get(slot));
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
