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
    private final List<ItemStack> items;
    private final int exp;
    private final String ownerName;
    private final int ownerLevel;
    private final String killerName;
    private final long despawnAtMillis;

    public Grave(UUID id, UUID owner, String ownerName, int ownerLevel, String killerName, Location location,
            List<ItemStack> items, int exp, long despawnAtMillis) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.ownerLevel = ownerLevel;
        this.killerName = killerName;
        this.location = location;
        this.items = new ArrayList<>(items);
        this.exp = exp;
        this.despawnAtMillis = despawnAtMillis;
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
}