package org.zkaleejoo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.grave.GraveManager;
import org.zkaleejoo.listeners.GraveListener;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.UpdateChecker;
import net.md_5.bungee.api.ChatColor;

public final class MaxGraves extends JavaPlugin {

    private MainConfigManager mainConfigManager;
    private GraveManager graveManager;
    private String version = getDescription().getVersion();
    private String latestVersion;

    //PLUGIN ENCIENDE
    @Override
    public void onEnable() {
        saveDefaultConfig();

        mainConfigManager = new MainConfigManager(this);
        graveManager = new GraveManager(this);

        MainCommand mainCommand = new MainCommand(this);
        registerCommand("maxgraves", mainCommand, mainCommand);

        getServer().getPluginManager().registerEvents(new GraveListener(this), this);

        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE+"   _____                  ________                                 ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE+"  /     \\ _____  ___  ___/  _____/___________ ___  __ ____   ______");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE+" /  \\ /  \\\\__  \\ \\  \\/  /   \\  __\\_  __ \\__  \\\\  \\/ // __ \\ /  ___/");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE+"/    Y    \\/ __ \\_>    <\\    \\_\\  \\  | \\// __ \\\\   /\\  ___/ \\___ \\ ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE+"\\____|__  (____  /__/\\_ \\\\______  /__|  (____  /\\_/  \\___  >____  >");
        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE+"        \\/     \\/      \\/       \\/           \\/          \\/     \\/ ");

        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&#8A2BE2&lMaxGraves &8» &fThe plugin has been enabled! Version: " + version));
        
        checkUpdates();
    }

    @Override
    public void onDisable() {
        if (graveManager != null) {
            graveManager.clearAll();
        }

        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage("&#8A2BE2&lMaxGraves &8» &fThe plugin has been disabled! Version: " + version));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter tabCompleter) {
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
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
                getLogger().info("You are using the latest version!");
            } else {
                this.latestVersion = version;

                Bukkit.getConsoleSender()
                        .sendMessage(MessageUtils.getColoredMessage("&#8A2BE2&lMaxGraves &8» &a&lUPDATE AVAILABLE!"));
                Bukkit.getConsoleSender().sendMessage(MessageUtils
                        .getColoredMessage("&#8A2BE2&lMaxGraves &8» &7A new version of the plugin has been detected."));
                Bukkit.getConsoleSender().sendMessage(
                        MessageUtils.getColoredMessage("&#8A2BE2&lMaxGraves &8» &7Available version: &a" + version));
                Bukkit.getConsoleSender().sendMessage(
                        MessageUtils.getColoredMessage("&#8A2BE2&lMaxGraves &8» &eDownload it now at the following link:"));
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                        "&#8A2BE2&lMaxGraves &8» &a&nhttps://modrinth.com/plugin/maxgraves"));
            }
        });
    }

    public String getLatestVersion() {
        return latestVersion;
    }


}
