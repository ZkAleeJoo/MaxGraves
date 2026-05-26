package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bstats.bukkit.Metrics;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.CustomConfig;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.grave.GraveManager;
import org.zkaleejoo.listeners.GraveListener;
import org.zkaleejoo.listeners.PlayerJoinListener;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.UpdateChecker;

public final class MaxGraves extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 31607;
    private static final long UPDATE_CHECK_INTERVAL_TICKS = 20L * 60L * 60L * 5L;

    private MainConfigManager mainConfigManager;
    private GraveManager graveManager;
    private String latestVersion;
    private Metrics metrics;
    private BukkitTask updateCheckTask;

    // PLUGIN ENCIENDE
    @Override
    public void onEnable() {
        CustomConfig initialConfig = new CustomConfig("config.yml", null, this, false);
        initialConfig.registerConfig();

        mainConfigManager = new MainConfigManager(this);
        syncMetricsState();
        graveManager = new GraveManager(this);

        MainCommand mainCommand = new MainCommand(this);
        registerCommand("maxgraves", mainCommand, mainCommand);

        getServer().getPluginManager().registerEvents(new GraveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&5&lMaxGraves &8» &5   _____      _____  ____  _____________________    _________   _______________ _________"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&5&lMaxGraves &8» &5  /     \\    /  _  \\ \\   \\/  /  _____/\\______   \\  /  _  \\   \\ /   /\\_   _____//   _____/"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&5&lMaxGraves &8» &5 /  \\ /  \\  /  /_\\  \\ \\     /   \\  ___ |       _/ /  /_\\  \\   Y   /  |    __)_ \\_____  \\ "));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&5&lMaxGraves &8» &5/    Y    \\/    |    \\/     \\    \\_\\  \\|    |   \\/    |    \\     /   |        \\/        \\"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&5&lMaxGraves &8» &5\\____|__  /\\____|__  /___/\\  \\______  /|____|_  /\\____|__  /\\___/   /_______  /_______  /"));
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                "&5&lMaxGraves &8» &5        \\/         \\/      \\_/      \\/        \\/         \\/                 \\/        \\/ "));

        Bukkit.getConsoleSender().sendMessage(MessageUtils
                .getColoredMessage("&5&lMaxGraves &8» &5The plugin has been enabled! Version: "));

        startUpdateChecks();
    }

    @Override
    public void onDisable() {
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }

        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }

        if (graveManager != null) {
            graveManager.clearAll();
        }

        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage("&5&lMaxGraves &8» &fThe plugin has been disabled! Version: "));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor,
            org.bukkit.command.TabCompleter tabCompleter) {
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command \"" + name + "\" is missing in plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public MainConfigManager getConfigManager() {
        return mainConfigManager;
    }

    public GraveManager getGraveManager() {
        return graveManager;
    }

    private void checkUpdates() {
        if (!getConfigManager().isUpdateCheckEnabled()) {
            return;
        }

        new UpdateChecker(this).getVersion(version -> {
            if (this.getPluginMeta().getVersion().equalsIgnoreCase(version)) {
                this.latestVersion = null;
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                        "&5&lMaxGraves &8» &aA check for updates was performed and nothing was found."));
            } else {
                this.latestVersion = version;

                Bukkit.getConsoleSender()
                        .sendMessage(MessageUtils
                                .getColoredMessage("&5&lMaxGraves &8» &f&lNEW VERSION: &7" + version));
                Bukkit.getConsoleSender().sendMessage(
                        MessageUtils
                                .getColoredMessage(
                                        "&5&lMaxGraves &8» &fDownload it now at the following link: &7https://modrinth.com/plugin/maxgraves"));
            }
        });
    }

    private void startUpdateChecks() {
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }

        if (!getConfigManager().isUpdateCheckEnabled()) {
            latestVersion = null;
            return;
        }

        checkUpdates();
        updateCheckTask = Bukkit.getScheduler().runTaskTimer(this, this::checkUpdates,
                UPDATE_CHECK_INTERVAL_TICKS, UPDATE_CHECK_INTERVAL_TICKS);
    }

    private void syncMetricsState() {
        if (getConfigManager().isBStatsEnabled()) {
            if (metrics == null) {
                metrics = new Metrics(this, BSTATS_PLUGIN_ID);
            }
            return;
        }

        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }

    public void reloadPluginState() {
        getConfigManager().reloadConfig();
        getGraveManager().reloadSettings();
        syncMetricsState();
        startUpdateChecks();
    }

    public String getLatestVersion() {
        return latestVersion;
    }

}
