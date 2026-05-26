package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.grave.GraveManager;
import org.zkaleejoo.listeners.GraveListener;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.UpdateChecker;

public final class MaxGraves extends JavaPlugin {

    private MainConfigManager mainConfigManager;
    private GraveManager graveManager;
    private String latestVersion;

    // PLUGIN ENCIENDE
    @Override
    public void onEnable() {
        saveDefaultConfig();

        mainConfigManager = new MainConfigManager(this);
        graveManager = new GraveManager(this);

        MainCommand mainCommand = new MainCommand(this);
        registerCommand("maxgraves", mainCommand, mainCommand);

        getServer().getPluginManager().registerEvents(new GraveListener(this), this);

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

        checkUpdates();
    }

    @Override
    public void onDisable() {
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
        new UpdateChecker(this).getVersion(version -> {
            if (this.getPluginMeta().getVersion().equalsIgnoreCase(version)) {
                getLogger().info("You are using the latest version!");
            } else {
                this.latestVersion = version;

                Bukkit.getConsoleSender()
                        .sendMessage(MessageUtils
                                .getColoredMessage("&5&lMaxGraves &8» &a&lUPDATE AVAILABLE! " + version));
                Bukkit.getConsoleSender().sendMessage(
                        MessageUtils
                                .getColoredMessage(
                                        "&5&lMaxGraves &8» &eDownload it now at the following link: &7https://modrinth.com/plugin/maxgraves"));
            }
        });
    }

    public String getLatestVersion() {
        return latestVersion;
    }

}
