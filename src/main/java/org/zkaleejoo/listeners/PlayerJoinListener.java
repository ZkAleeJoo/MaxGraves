package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;

public class PlayerJoinListener implements Listener {

    private static final String UPDATE_NOTIFY_PERMISSION = "maxgrave.admin";
    private static final String DOWNLOAD_URL = "https://modrinth.com/plugin/maxgraves";

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

        String latest = plugin.getLatestVersion();
        if (latest == null || plugin.getPluginMeta().getVersion().equalsIgnoreCase(latest)) {
            return;
        }

        player.sendMessage(" ");
        player.sendMessage(MessageUtils.getColoredMessage(
                config.getPrefix() + config.getMsgUpdateAvailable().replace("{version}", latest)));
        player.sendMessage(MessageUtils.getColoredMessage(
                config.getMsgUpdateCurrent().replace("{version}", plugin.getPluginMeta().getVersion())));
        player.sendMessage(MessageUtils.getColoredMessage(config.getMsgUpdateDownload()));
        player.sendMessage(MessageUtils.getColoredMessage("&7" + DOWNLOAD_URL));
        player.sendMessage(" ");
    }
}
