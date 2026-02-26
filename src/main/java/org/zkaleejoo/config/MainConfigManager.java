package org.zkaleejoo.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxGraves;

import java.util.List;

public class MainConfigManager {

    private CustomConfig configFile;
    private CustomConfig langFile;
    private final MaxGraves plugin;

    //VARIABLES CONFIG
    private String selectedLanguage;
    private String prefix;
    private int graveDespawnTime;
    private boolean createOnDeath;
    private String graveMarkerBlock;
    private boolean hologramEnabled;
    private long hologramUpdateIntervalTicks;
    private double hologramBaseHeight;
    private double hologramLineSpacing;
    private List<String> hologramLines;
    private String infoMenuItemMaterial;

    //VARIABLES MENSAJES
    private String msgNoPermission;
    private String msgPluginReload;
    private String msgGraveCreated;
    private String msgGraveClaimed;
    private String msgMapReceived;
    private String msgUsageCommand;
    private String msgOnlyOwnerCanClaim;
    private String msgOnlyOwnerCanUseMap;
    private String msgLocatorUsed;
    private String msgGraveNotFound;
    private String msgGraveExpired;
    private String msgGraveCreateFail;
    private String msgHologramUnknownKiller;
    private String msgLocatorItemName;
    private String msgLocatorItemWorld;
    private String msgLocatorItemCoordinates;
    private String msgLocatorItemAction;
    private String msgOnlyPlayersCommand;
    private String msgInfoNoGrave;
    private String msgInfoHeader;
    private String msgInfoWorld;
    private String msgInfoCoordinates;
    private String msgInfoTimeLeft;
    private String msgInfoMenuTitle;
    private String msgInfoMenuItemName;
    private List<String> msgInfoMenuLore;

    public MainConfigManager(MaxGraves plugin) {
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = configFile.getConfig();

        selectedLanguage = config.getString("general.language", "en");

        String langPath = "messages_" + selectedLanguage + ".yml";
        langFile = new CustomConfig(langPath, "lang", plugin, false);
        langFile.registerConfig();
        FileConfiguration lang = langFile.getConfig();

        //CONFIG
        prefix = config.getString("general.prefix", "&#8A2BE2&lMaxGraves &8» ");
        graveDespawnTime = config.getInt("grave.despawn-time", 3600);
        createOnDeath = config.getBoolean("grave.create-on-death", true);
        graveMarkerBlock = config.getString("grave.marker-block", "PLAYER_HEAD");

        hologramEnabled = config.getBoolean("grave.hologram.enabled", true);
        hologramUpdateIntervalTicks = Math.max(config.getLong("grave.hologram.update-interval-ticks", 20L), 1L);
        hologramBaseHeight = config.getDouble("grave.hologram.base-height", 1.8D);
        hologramLineSpacing = config.getDouble("grave.hologram.line-spacing", 0.25D);
        List<String> configuredHologramLines = config.getStringList("grave.hologram.lines");
        hologramLines = configuredHologramLines.isEmpty()
                ? List.of("&7{player}", "&e{time_left}")
                : List.copyOf(configuredHologramLines);

        String configuredInfoMenuMaterial = config.getString("grave.info-menu.item.material", "PAPER");
        Material infoMenuMaterial = Material.matchMaterial(configuredInfoMenuMaterial == null ? "PAPER" : configuredInfoMenuMaterial);
        if (infoMenuMaterial == null || !infoMenuMaterial.isItem()) {
            plugin.getLogger().warning("Invalid material for grave.info-menu.item.material: " + configuredInfoMenuMaterial + ". Falling back to PAPER.");
            infoMenuItemMaterial = Material.PAPER.name();
        } else {
            infoMenuItemMaterial = infoMenuMaterial.name();
        }

        //MENSAJES
        msgNoPermission = lang.getString("messages.no-permission", "&cYou do not have permission.");
        msgPluginReload = lang.getString("messages.plugin-reload", "&aConfiguration successfully reloaded.");
        msgGraveCreated = lang.getString("messages.grave-created", "&eYour tomb has been created. You have been given a map.");
        msgGraveClaimed = lang.getString("messages.grave-claimed", "&aYou have recovered your items and XP.");
        msgMapReceived = lang.getString("messages.map-received", "&eTomb map received.");
        msgUsageCommand = lang.getString("messages.usage-command", "&cUse: /maxgraves <reload>");
        msgOnlyOwnerCanClaim = lang.getString("messages.only-owner-can-claim", "&cOnly the owner can claim this grave.");
        msgOnlyOwnerCanUseMap = lang.getString("messages.only-owner-can-use-map", "&cThis map does not belong to you.");
        msgLocatorUsed = lang.getString("messages.locator-used", "&aTeleported to your grave.");
        msgGraveNotFound = lang.getString("messages.grave-not-found", "&cYour grave could not be found.");
        msgGraveExpired = lang.getString("messages.grave-expired", "&cYour grave has expired and its contents were lost.");
        msgGraveCreateFail = lang.getString("messages.grave-create-fail", "&cCould not create a grave at your death location.");
        msgHologramUnknownKiller = lang.getString("messages.hologram-unknown-killer", "Unknown");
        msgLocatorItemName = lang.getString("messages.locator-item-name", "&#8A2BE2&lMaxGrave Locator");
        msgLocatorItemWorld = lang.getString("messages.locator-item-world", "&7World: &f{world}");
        msgLocatorItemCoordinates = lang.getString("messages.locator-item-coordinates", "&7X: &f{x} &7Y: &f{y} &7Z: &f{z}");
        msgLocatorItemAction = lang.getString("messages.locator-item-action", "&eRight click to teleport");
        msgOnlyPlayersCommand = lang.getString("messages.only-players-command", "&cOnly players can use this command.");
        msgInfoNoGrave = lang.getString("messages.info-no-grave", "&cYou currently do not have an active grave.");
        msgInfoHeader = lang.getString("messages.info-header", "&eYour active grave:");
        msgInfoWorld = lang.getString("messages.info-world", "&7World: &f{world}");
        msgInfoCoordinates = lang.getString("messages.info-coordinates", "&7Coordinates: &fX:{x} Y:{y} Z:{z}");
        msgInfoTimeLeft = lang.getString("messages.info-time-left", "&7Time left: &f{time_left}");
        msgInfoMenuTitle = lang.getString("messages.info-menu-title", "&8Active Graves");
        msgInfoMenuItemName = lang.getString("messages.info-menu-item-name", "&eGrave #{index}");
        List<String> configuredInfoMenuLore = lang.getStringList("messages.info-menu-item-lore");
        msgInfoMenuLore = configuredInfoMenuLore.isEmpty()
                ? List.of("&7World: &f{world}", "&7Coordinates: &fX:{x} Y:{y} Z:{z}", "&7Time left: &f{time_left}", "&7ID: &f{id}")
                : List.copyOf(configuredInfoMenuLore);
    }

    public void reloadConfig() {
        configFile.reloadConfig();
        if (langFile != null) langFile.reloadConfig();
        loadConfig();
    }

    //GETTERS
    public String getPrefix() { return prefix; }
    public int getGraveDespawnTime() { return graveDespawnTime; }
    public boolean isCreateOnDeath() { return createOnDeath; }
    public String getGraveMarkerBlock() { return graveMarkerBlock; }
    public boolean isHologramEnabled() { return hologramEnabled; }
    public long getHologramUpdateIntervalTicks() { return hologramUpdateIntervalTicks; }
    public double getHologramBaseHeight() { return hologramBaseHeight; }
    public double getHologramLineSpacing() { return hologramLineSpacing; }
    public List<String> getHologramLines() { return hologramLines; }
    public String getInfoMenuItemMaterial() { return infoMenuItemMaterial; }

    public String getMsgNoPermission() { return msgNoPermission; }
    public String getMsgPluginReload() { return msgPluginReload; }
    public String getMsgGraveCreated() { return msgGraveCreated; }
    public String getMsgGraveClaimed() { return msgGraveClaimed; }
    public String getMsgMapReceived() { return msgMapReceived; }
    public String getMsgUsageCommand() { return msgUsageCommand; }
    public String getMsgOnlyOwnerCanClaim() { return msgOnlyOwnerCanClaim; }
    public String getMsgOnlyOwnerCanUseMap() { return msgOnlyOwnerCanUseMap; }
    public String getMsgLocatorUsed() { return msgLocatorUsed; }
    public String getMsgGraveNotFound() { return msgGraveNotFound; }
    public String getMsgGraveExpired() { return msgGraveExpired; }
    public String getMsgGraveCreateFail() { return msgGraveCreateFail; }
    public String getMsgHologramUnknownKiller() { return msgHologramUnknownKiller; }
    public String getMsgLocatorItemName() { return msgLocatorItemName; }
    public String getMsgLocatorItemWorld() { return msgLocatorItemWorld; }
    public String getMsgLocatorItemCoordinates() { return msgLocatorItemCoordinates; }
    public String getMsgLocatorItemAction() { return msgLocatorItemAction; }
    public String getMsgOnlyPlayersCommand() { return msgOnlyPlayersCommand; }
    public String getMsgInfoNoGrave() { return msgInfoNoGrave; }
    public String getMsgInfoHeader() { return msgInfoHeader; }
    public String getMsgInfoWorld() { return msgInfoWorld; }
    public String getMsgInfoCoordinates() { return msgInfoCoordinates; }
    public String getMsgInfoTimeLeft() { return msgInfoTimeLeft; }
    public String getMsgInfoMenuTitle() { return msgInfoMenuTitle; }
    public String getMsgInfoMenuItemName() { return msgInfoMenuItemName; }
    public List<String> getMsgInfoMenuLore() { return msgInfoMenuLore; }
}
