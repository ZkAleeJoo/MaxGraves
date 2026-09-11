package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

import org.zkaleejoo.utils.UpdateNotificationFormatter;

public class PlayerJoinListener implements Listener {

    private static final String UPDATE_NOTIFY_PERMISSION = "maxgrave.admin";

    private final MaxGraves plugin;

    public PlayerJoinListener(MaxGraves plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MainConfigManager config = plugin.getConfigManager();

        if (!player.hasPermission(UPDATE_NOTIFY_PERMISSION)) {
            return;
        }

        for (String line : UpdateNotificationFormatter.format(
                config.getPrefix(),
                config.getMsgUpdateAvailable(),
                plugin.getPluginMeta().getVersion(),
                plugin.getLatestVersion(),
                MaxGraves.UPDATE_DOWNLOAD_URL)) {
            player.sendMessage(MessageUtils.getColoredMessage(line));
        }
    }
}
