package org.zkaleejoo.commands;

import org.bukkit.Material;

import java.util.List;

public record InfoMenuButton(boolean enabled, int slot, Material material, String name, List<String> lore) {

    public static InfoMenuButton disabled() {
        return new InfoMenuButton(false, -1, Material.AIR, "", List.of());
    }
}
