package org.zkaleejoo.scheduler;

import org.bukkit.plugin.Plugin;

public class SchedulerAdapterFactory {
    public static SchedulerAdapter createAdapter(Plugin plugin) {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return new FoliaSchedulerAdapter(plugin);
        } catch (ClassNotFoundException e) {
            return new BukkitSchedulerAdapter(plugin);
        }
    }
}
