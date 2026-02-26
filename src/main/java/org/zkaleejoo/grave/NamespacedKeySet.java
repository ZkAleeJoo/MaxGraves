package org.zkaleejoo.grave;

import org.bukkit.NamespacedKey;
import org.zkaleejoo.MaxGraves;

public record NamespacedKeySet(NamespacedKey graveIdKey) {

    public NamespacedKeySet(MaxGraves plugin) {
        this(new NamespacedKey(plugin, "grave_id"));
    }
}
