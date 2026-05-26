package org.zkaleejoo.grave;

import java.util.Locale;

import org.bukkit.Material;

public enum GraveMarkerType {
    HEAD(Material.PLAYER_HEAD),
    CHEST(Material.CHEST);

    private final Material material;

    GraveMarkerType(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean supportsEffects() {
        return this == HEAD;
    }

    public static GraveMarkerType fromConfig(String configuredType) {
        if (configuredType == null || configuredType.isBlank()) {
            return HEAD;
        }

        try {
            return valueOf(configuredType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return HEAD;
        }
    }
}
