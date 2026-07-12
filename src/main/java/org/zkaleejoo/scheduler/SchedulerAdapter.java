package org.zkaleejoo.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface SchedulerAdapter {
    void runAsync(Runnable runnable);
    ScheduledTask runLater(Runnable runnable, long delayTicks);
    ScheduledTask runTimer(Runnable runnable, long delayTicks, long periodTicks);
    void runAtLocation(Location location, Runnable runnable);
    ScheduledTask runAtLocationLater(Location location, Runnable runnable, long delayTicks);
    ScheduledTask runAtLocationTimer(Location location, Runnable runnable, long delayTicks, long periodTicks);
    void runForPlayer(Player player, Runnable runnable);
    ScheduledTask runForPlayerLater(Player player, Runnable runnable, long delayTicks);
    void run(Runnable runnable);
}
