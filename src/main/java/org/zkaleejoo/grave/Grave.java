package org.zkaleejoo.grave;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Grave {

    private final UUID id;
    private final UUID owner;
    private final Location location;
    private final List<ItemStack> items;
    private final int exp;
    private final String ownerName;
    private final int ownerLevel;
    private final String killerName;
    private final long despawnAtMillis;
    private final GraveMarkerType markerType;
    private final boolean publicAccess;

    public Grave(UUID id, UUID owner, String ownerName, int ownerLevel, String killerName, Location location,
            List<ItemStack> items, int exp, long despawnAtMillis, GraveMarkerType markerType, boolean publicAccess) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.ownerLevel = ownerLevel;
        this.killerName = killerName;
        this.location = location;
        this.items = new ArrayList<>(items);
        this.exp = exp;
        this.despawnAtMillis = despawnAtMillis;
        this.markerType = markerType;
        this.publicAccess = publicAccess;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location.clone();
    }

    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void replaceItemsFromInventory(Inventory inventory) {
        items.clear();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
    }

    public boolean isEmpty() {
        return items.stream()
                .allMatch(item -> item == null || item.getType() == Material.AIR);
    }

    public int getExp() {
        return exp;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getOwnerLevel() {
        return ownerLevel;
    }

    public String getKillerName() {
        return killerName;
    }

    public long getDespawnAtMillis() {
        return despawnAtMillis;
    }

    public GraveMarkerType getMarkerType() {
        return markerType;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }
}
