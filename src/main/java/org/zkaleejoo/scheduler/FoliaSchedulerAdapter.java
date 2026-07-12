package org.zkaleejoo.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
    }

    @Override
    public ScheduledTask runLater(Runnable runnable, long delayTicks) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), Math.max(1, delayTicks));
        return task::cancel;
    }

    @Override
    public ScheduledTask runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> runnable.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
        return task::cancel;
    }

    @Override
    public void runAtLocation(Location location, Runnable runnable) {
        Bukkit.getRegionScheduler().run(plugin, location, t -> runnable.run());
    }

    @Override
    public ScheduledTask runAtLocationLater(Location location, Runnable runnable, long delayTicks) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> runnable.run(), Math.max(1, delayTicks));
        return task::cancel;
    }

    @Override
    public ScheduledTask runAtLocationTimer(Location location, Runnable runnable, long delayTicks, long periodTicks) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, t -> runnable.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
        return task::cancel;
    }

    @Override
    public void runForPlayer(Player player, Runnable runnable) {
        player.getScheduler().run(plugin, t -> runnable.run(), null);
    }

    @Override
    public ScheduledTask runForPlayerLater(Player player, Runnable runnable, long delayTicks) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = player.getScheduler().runDelayed(plugin, t -> runnable.run(), null, Math.max(1, delayTicks));
        return task::cancel;
    }

    @Override
    public void run(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> runnable.run());
    }
}
