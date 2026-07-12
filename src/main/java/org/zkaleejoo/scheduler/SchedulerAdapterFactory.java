package org.zkaleejoo.scheduler;

import org.bukkit.plugin.Plugin;

public class SchedulerAdapterFactory {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    public static SchedulerAdapter createAdapter(Plugin plugin) {
        return FOLIA ? new FoliaSchedulerAdapter(plugin) : new BukkitSchedulerAdapter(plugin);
    }

    public static boolean isFolia() {
        return FOLIA;
    }
}
