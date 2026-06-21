package org.zkaleejoo.grave;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;

final class GravePlacementPolicy {

    private static final Set<Material> MARKER_REPLACEABLE_MATERIALS = createMarkerReplaceableMaterials();

    private GravePlacementPolicy() {
    }

    static boolean isMarkerReplaceable(Material material) {
        return material != null && MARKER_REPLACEABLE_MATERIALS.contains(material);
    }

    private static Set<Material> createMarkerReplaceableMaterials() {
        EnumSet<Material> materials = EnumSet.noneOf(Material.class);
        Collections.addAll(materials,
                Material.AIR,
                Material.CAVE_AIR,
                Material.VOID_AIR,
                Material.WATER,
                Material.LAVA,
                Material.TALL_GRASS,
                Material.SHORT_GRASS,
                Material.FERN,
                Material.LARGE_FERN,
                Material.DEAD_BUSH,
                Material.SNOW);

        addIfPresent(materials,
                "DANDELION",
                "POPPY",
                "BLUE_ORCHID",
                "ALLIUM",
                "AZURE_BLUET",
                "RED_TULIP",
                "ORANGE_TULIP",
                "WHITE_TULIP",
                "PINK_TULIP",
                "OXEYE_DAISY",
                "CORNFLOWER",
                "LILY_OF_THE_VALLEY",
                "WITHER_ROSE",
                "SUNFLOWER",
                "LILAC",
                "ROSE_BUSH",
                "PEONY",
                "TORCHFLOWER",
                "PITCHER_PLANT",
                "PINK_PETALS",
                "WILDFLOWERS",
                "CACTUS_FLOWER",
                "OPEN_EYEBLOSSOM",
                "CLOSED_EYEBLOSSOM");

        return Collections.unmodifiableSet(materials);
    }

    private static void addIfPresent(EnumSet<Material> materials, String... materialNames) {
        for (String materialName : materialNames) {
            try {
                materials.add(Material.valueOf(materialName));
            } catch (IllegalArgumentException ignored) {
                // Material is not available on this API version.
            }
        }
    }
}
