package org.zkaleejoo.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.grave.GraveMarkerType;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class MainConfigManager {

    private CustomConfig configFile;
    private CustomConfig langFile;
    private final MaxGraves plugin;

    // VARIABLES CONFIG
    private String selectedLanguage;
    private String prefix;
    private boolean updateCheckEnabled;
    private boolean bStatsEnabled;
    private int graveDespawnTime;
    private boolean createOnDeath;
    private boolean hologramEnabled;
    private long hologramUpdateIntervalTicks;
    private double hologramBaseHeight;
    private double hologramLineSpacing;
    private List<String> hologramLines;
    private String infoMenuItemMaterial;
    private boolean effectsEnabled;
    private long effectsUpdateIntervalTicks;
    private String effectsPrimaryParticle;
    private String effectsSecondaryParticle;
    private int effectsPrimaryCount;
    private int effectsSecondaryCount;
    private double effectsSpiralRadius;
    private double effectsSpiralHeight;
    private double effectsVerticalSpeed;
    private boolean effectsAmbientSoundEnabled;
    private String effectsAmbientSound;
    private float effectsAmbientSoundVolume;
    private float effectsAmbientSoundPitch;
    private boolean claimAnimationEnabled;
    private long claimAnimationDelayTicks;
    private boolean claimAnimationLightningEnabled;
    private String claimAnimationSound;
    private float claimAnimationSoundVolume;
    private float claimAnimationSoundPitch;
    private int graveSearchMaxRadius;
    private Set<String> graveBlacklistedWorlds;
    private boolean debugDeathEvents;
    private GraveMarkerType graveMarkerType;
    private boolean publicPlayerKillAccess;

    // VARIABLES MENSAJES
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
    private String msgWorldBlacklisted;
    private String msgUpdateAvailable;
    private String msgUpdateCurrent;
    private String msgUpdateDownload;

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

        // CONFIG
        prefix = config.getString("general.prefix", "&#8A2BE2&lMaxGraves &8» ");
        updateCheckEnabled = config.getBoolean("general.update-check", true);
        bStatsEnabled = config.getBoolean("general.bstats", true);
        graveDespawnTime = config.getInt("grave.despawn-time", 3600);
        graveSearchMaxRadius = Math.max(config.getInt("grave.search-max-radius", 6), 1);
        debugDeathEvents = config.getBoolean("grave.debug-death-events", false);
        createOnDeath = config.getBoolean("grave.create-on-death", true);
        graveMarkerType = GraveMarkerType.fromConfig(config.getString("grave.marker.type", "HEAD"));
        String configuredMarkerType = config.getString("grave.marker.type", "HEAD");
        if (!graveMarkerType.name()
                .equalsIgnoreCase(configuredMarkerType == null ? "HEAD" : configuredMarkerType.trim())) {
            plugin.getLogger().warning("Invalid marker type for grave.marker.type: "
                    + configuredMarkerType + ". Falling back to HEAD.");
        }
        publicPlayerKillAccess = config.getBoolean("grave.access.public-player-kill", false);
        graveBlacklistedWorlds = config.getStringList("grave.blacklisted-worlds").stream()
                .filter(worldName -> worldName != null && !worldName.isBlank())
                .map(worldName -> worldName.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        hologramEnabled = config.getBoolean("grave.hologram.enabled", true);
        hologramUpdateIntervalTicks = Math.max(config.getLong("grave.hologram.update-interval-ticks", 20L), 1L);
        hologramBaseHeight = config.getDouble("grave.hologram.base-height", 1.8D);
        hologramLineSpacing = config.getDouble("grave.hologram.line-spacing", 0.25D);
        List<String> configuredHologramLines = config.getStringList("grave.hologram.lines");
        hologramLines = configuredHologramLines.isEmpty()
                ? List.of("&7{player}", "&e{time_left}")
                : List.copyOf(configuredHologramLines);

        String configuredInfoMenuMaterial = config.getString("grave.info-menu.item.material", "PAPER");
        Material infoMenuMaterial = Material
                .matchMaterial(configuredInfoMenuMaterial == null ? "PAPER" : configuredInfoMenuMaterial);
        if (infoMenuMaterial == null || !infoMenuMaterial.isItem()) {
            plugin.getLogger().warning("Invalid material for grave.info-menu.item.material: "
                    + configuredInfoMenuMaterial + ". Falling back to PAPER.");
            infoMenuItemMaterial = Material.PAPER.name();
        } else {
            infoMenuItemMaterial = infoMenuMaterial.name();
        }
        effectsEnabled = config.getBoolean("grave.effects.enabled", true);
        effectsUpdateIntervalTicks = Math.max(config.getLong("grave.effects.update-interval-ticks", 5L), 1L);
        effectsPrimaryParticle = config.getString("grave.effects.primary-particle", "SOUL");
        effectsSecondaryParticle = config.getString("grave.effects.secondary-particle", "SMOKE");
        effectsPrimaryCount = Math.max(config.getInt("grave.effects.primary-count", 4), 0);
        effectsSecondaryCount = Math.max(config.getInt("grave.effects.secondary-count", 2), 0);
        effectsSpiralRadius = Math.max(config.getDouble("grave.effects.spiral-radius", 0.7D), 0D);
        effectsSpiralHeight = Math.max(config.getDouble("grave.effects.spiral-height", 1.3D), 0D);
        effectsVerticalSpeed = Math.max(config.getDouble("grave.effects.vertical-speed", 0.05D), 0.001D);

        effectsAmbientSoundEnabled = config.getBoolean("grave.effects.ambient-sound.enabled", true);
        effectsAmbientSound = config.getString("grave.effects.ambient-sound.type", "BLOCK_SOUL_SAND_HIT");
        effectsAmbientSoundVolume = (float) Math.max(config.getDouble("grave.effects.ambient-sound.volume", 0.45D), 0D);
        effectsAmbientSoundPitch = (float) Math.max(config.getDouble("grave.effects.ambient-sound.pitch", 0.7D), 0.1D);
        claimAnimationEnabled = config.getBoolean("grave.claim-animation.enabled", true);
        claimAnimationDelayTicks = Math.max(config.getLong("grave.claim-animation.delay-ticks", 20L), 0L);
        claimAnimationLightningEnabled = config.getBoolean("grave.claim-animation.lightning.enabled", true);
        claimAnimationSound = config.getString("grave.claim-animation.sound.type", "ITEM_TOTEM_USE");
        claimAnimationSoundVolume = (float) Math.max(config.getDouble("grave.claim-animation.sound.volume", 1.0D), 0D);
        claimAnimationSoundPitch = (float) Math.max(config.getDouble("grave.claim-animation.sound.pitch", 0.75D), 0.1D);

        // MENSAJES
        msgNoPermission = lang.getString("messages.no-permission", "&cYou do not have permission.");
        msgPluginReload = lang.getString("messages.plugin-reload", "&aConfiguration successfully reloaded.");
        msgGraveCreated = lang.getString("messages.grave-created",
                "&eYour tomb has been created. You have been given a map.");
        msgGraveClaimed = lang.getString("messages.grave-claimed", "&aYou have recovered your items and XP.");
        msgMapReceived = lang.getString("messages.map-received", "&eTomb map received.");
        msgUsageCommand = lang.getString("messages.usage-command", "&cUse: /maxgraves <reload>");
        msgOnlyOwnerCanClaim = lang.getString("messages.only-owner-can-claim",
                "&cOnly the owner can claim this grave.");
        msgOnlyOwnerCanUseMap = lang.getString("messages.only-owner-can-use-map", "&cThis map does not belong to you.");
        msgLocatorUsed = lang.getString("messages.locator-used", "&aTeleported to your grave.");
        msgGraveNotFound = lang.getString("messages.grave-not-found", "&cYour grave could not be found.");
        msgGraveExpired = lang.getString("messages.grave-expired",
                "&cYour grave has expired and its contents were lost.");
        msgGraveCreateFail = lang.getString("messages.grave-create-fail",
                "&cCould not create a grave at your death location.");
        msgHologramUnknownKiller = lang.getString("messages.hologram-unknown-killer", "Unknown");
        msgLocatorItemName = lang.getString("messages.locator-item-name", "&#8A2BE2&lMaxGrave Locator");
        msgLocatorItemWorld = lang.getString("messages.locator-item-world", "&7World: &f{world}");
        msgLocatorItemCoordinates = lang.getString("messages.locator-item-coordinates",
                "&7X: &f{x} &7Y: &f{y} &7Z: &f{z}");
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
                ? List.of("&7World: &f{world}", "&7Coordinates: &fX:{x} Y:{y} Z:{z}", "&7Time left: &f{time_left}",
                        "&7ID: &f{id}")
                : List.copyOf(configuredInfoMenuLore);
        msgWorldBlacklisted = lang.getString("messages.world-blacklisted",
                "&cGraves are disabled in this world.");
        msgUpdateAvailable = lang.getString("messages.update-available",
                "&f&lNEW VERSION: &7{version}");
        msgUpdateCurrent = lang.getString("messages.update-current", "&7Your current version: &c{version}");
        msgUpdateDownload = lang.getString("messages.update-download",
                "&eDownload it to get improvements and fixes.");
    }

    public void reloadConfig() {
        configFile.reloadConfig();
        if (langFile != null)
            langFile.reloadConfig();
        loadConfig();
    }

    // GETTERS
    public String getPrefix() {
        return prefix;
    }

    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }

    public boolean isBStatsEnabled() {
        return bStatsEnabled;
    }

    public int getGraveDespawnTime() {
        return graveDespawnTime;
    }

    public boolean isCreateOnDeath() {
        return createOnDeath;
    }

    public boolean isHologramEnabled() {
        return hologramEnabled;
    }

    public long getHologramUpdateIntervalTicks() {
        return hologramUpdateIntervalTicks;
    }

    public double getHologramBaseHeight() {
        return hologramBaseHeight;
    }

    public double getHologramLineSpacing() {
        return hologramLineSpacing;
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }

    public String getInfoMenuItemMaterial() {
        return infoMenuItemMaterial;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public String getMsgPluginReload() {
        return msgPluginReload;
    }

    public String getMsgGraveCreated() {
        return msgGraveCreated;
    }

    public String getMsgGraveClaimed() {
        return msgGraveClaimed;
    }

    public String getMsgMapReceived() {
        return msgMapReceived;
    }

    public String getMsgUsageCommand() {
        return msgUsageCommand;
    }

    public String getMsgOnlyOwnerCanClaim() {
        return msgOnlyOwnerCanClaim;
    }

    public String getMsgOnlyOwnerCanUseMap() {
        return msgOnlyOwnerCanUseMap;
    }

    public String getMsgLocatorUsed() {
        return msgLocatorUsed;
    }

    public String getMsgGraveNotFound() {
        return msgGraveNotFound;
    }

    public String getMsgGraveExpired() {
        return msgGraveExpired;
    }

    public String getMsgGraveCreateFail() {
        return msgGraveCreateFail;
    }

    public String getMsgHologramUnknownKiller() {
        return msgHologramUnknownKiller;
    }

    public String getMsgLocatorItemName() {
        return msgLocatorItemName;
    }

    public String getMsgLocatorItemWorld() {
        return msgLocatorItemWorld;
    }

    public String getMsgLocatorItemCoordinates() {
        return msgLocatorItemCoordinates;
    }

    public String getMsgLocatorItemAction() {
        return msgLocatorItemAction;
    }

    public String getMsgOnlyPlayersCommand() {
        return msgOnlyPlayersCommand;
    }

    public String getMsgInfoNoGrave() {
        return msgInfoNoGrave;
    }

    public String getMsgInfoHeader() {
        return msgInfoHeader;
    }

    public String getMsgInfoWorld() {
        return msgInfoWorld;
    }

    public String getMsgInfoCoordinates() {
        return msgInfoCoordinates;
    }

    public String getMsgInfoTimeLeft() {
        return msgInfoTimeLeft;
    }

    public String getMsgInfoMenuTitle() {
        return msgInfoMenuTitle;
    }

    public String getMsgInfoMenuItemName() {
        return msgInfoMenuItemName;
    }

    public List<String> getMsgInfoMenuLore() {
        return msgInfoMenuLore;
    }

    public boolean isEffectsEnabled() {
        return effectsEnabled;
    }

    public long getEffectsUpdateIntervalTicks() {
        return effectsUpdateIntervalTicks;
    }

    public String getEffectsPrimaryParticle() {
        return effectsPrimaryParticle;
    }

    public String getEffectsSecondaryParticle() {
        return effectsSecondaryParticle;
    }

    public int getEffectsPrimaryCount() {
        return effectsPrimaryCount;
    }

    public int getEffectsSecondaryCount() {
        return effectsSecondaryCount;
    }

    public double getEffectsSpiralRadius() {
        return effectsSpiralRadius;
    }

    public double getEffectsSpiralHeight() {
        return effectsSpiralHeight;
    }

    public double getEffectsVerticalSpeed() {
        return effectsVerticalSpeed;
    }

    public boolean isEffectsAmbientSoundEnabled() {
        return effectsAmbientSoundEnabled;
    }

    public String getEffectsAmbientSound() {
        return effectsAmbientSound;
    }

    public float getEffectsAmbientSoundVolume() {
        return effectsAmbientSoundVolume;
    }

    public float getEffectsAmbientSoundPitch() {
        return effectsAmbientSoundPitch;
    }

    public boolean isClaimAnimationEnabled() {
        return claimAnimationEnabled;
    }

    public long getClaimAnimationDelayTicks() {
        return claimAnimationDelayTicks;
    }

    public boolean isClaimAnimationLightningEnabled() {
        return claimAnimationLightningEnabled;
    }

    public String getClaimAnimationSound() {
        return claimAnimationSound;
    }

    public float getClaimAnimationSoundVolume() {
        return claimAnimationSoundVolume;
    }

    public float getClaimAnimationSoundPitch() {
        return claimAnimationSoundPitch;
    }

    public int getGraveSearchMaxRadius() {
        return graveSearchMaxRadius;
    }

    public Set<String> getGraveBlacklistedWorlds() {
        return graveBlacklistedWorlds;
    }

    public String getMsgWorldBlacklisted() {
        return msgWorldBlacklisted;
    }

    public boolean isDebugDeathEvents() {
        return debugDeathEvents;
    }

    public GraveMarkerType getGraveMarkerType() {
        return graveMarkerType;
    }

    public boolean isPublicPlayerKillAccess() {
        return publicPlayerKillAccess;
    }

    public String getMsgUpdateAvailable() {
        return msgUpdateAvailable;
    }

    public String getMsgUpdateCurrent() {
        return msgUpdateCurrent;
    }

    public String getMsgUpdateDownload() {
        return msgUpdateDownload;
    }
}
