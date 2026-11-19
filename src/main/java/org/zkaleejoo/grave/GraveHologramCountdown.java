package org.zkaleejoo.grave;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.ArmorStand;
import org.zkaleejoo.utils.MessageUtils;

final class GraveHologramCountdown implements Runnable {

    private static final String TIME_LEFT = "{time_left}";

    private final long despawnAtMillis;
    private final List<TimedLine> timedLines = new ArrayList<>();
    private long displayedSeconds = -1;

    GraveHologramCountdown(long despawnAtMillis) {
        this.despawnAtMillis = despawnAtMillis;
    }

    void addLine(ArmorStand stand, String template) {
        if (template.contains(TIME_LEFT)) {
            timedLines.add(new TimedLine(stand, template));
        } else {
            stand.customName(MessageUtils.getColoredComponent(template));
        }
    }

    boolean hasTimedLines() {
        return !timedLines.isEmpty();
    }

    @Override
    public void run() {
        long seconds = Math.max((despawnAtMillis - System.currentTimeMillis()) / 1000L, 0L);
        if (seconds == displayedSeconds) {
            return;
        }
        displayedSeconds = seconds;
        String time = seconds / 3600 + "h " + seconds % 3600 / 60 + "m " + seconds % 60 + "s";
        for (TimedLine line : timedLines) {
            if (!line.stand().isDead()) {
                line.stand().customName(MessageUtils.getColoredComponent(line.template().replace(TIME_LEFT, time)));
            }
        }
    }

    private record TimedLine(ArmorStand stand, String template) {
    }
}
