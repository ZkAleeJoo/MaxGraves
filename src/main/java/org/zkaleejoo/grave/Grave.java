package org.zkaleejoo.grave;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Grave {

    private final UUID id;
    private final UUID owner;
    private final Location location;
    private final Location secondaryLocation;
    private final List<ItemStack> items;
    private final int exp;
    private final String ownerName;
    private final int ownerLevel;
    private final String killerName;
    private final long despawnAtMillis;
    private final Map<Location, BlockData> originalBlocks;

    public Grave(UUID id, UUID owner, String ownerName, int ownerLevel, String killerName, Location location, Location secondaryLocation, List<ItemStack> items, int exp, long despawnAtMillis, Map<Location, BlockData> originalBlocks) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.ownerLevel = ownerLevel;
        this.killerName = killerName;
        this.location = location;
        this.secondaryLocation = secondaryLocation != null ? secondaryLocation.clone() : null;
        this.items = new ArrayList<>(items);
        this.exp = exp;
        this.despawnAtMillis = despawnAtMillis;
        this.originalBlocks = new HashMap<>();
        if (originalBlocks != null) {
            originalBlocks.forEach((loc, blockData) -> {
                if (loc != null && blockData != null) {
                    this.originalBlocks.put(loc.clone(), blockData.clone());
                }
            });
        }
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

    public Location getSecondaryLocation() {
        return secondaryLocation != null ? secondaryLocation.clone() : null;
    }

    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
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

    public Map<Location, BlockData> getOriginalBlocks() {
        Map<Location, BlockData> blocks = new HashMap<>();
        originalBlocks.forEach((loc, data) -> blocks.put(loc.clone(), data.clone()));
        return Collections.unmodifiableMap(blocks);
    }
}