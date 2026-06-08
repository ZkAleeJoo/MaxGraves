package org.zkaleejoo.grave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.utils.MessageUtils;
import org.bukkit.Tag;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;

import java.util.*;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class GraveManager {

    private final MaxGraves plugin;
    private final NamespacedKeySet keys;
    private final Map<UUID, Grave> gravesById = new HashMap<>();
    private final Map<UUID, Set<UUID>> gravesByPlayer = new HashMap<>();
    private final Map<GraveBlockKey, UUID> gravesByBlock = new HashMap<>();
    private final Map<UUID, BukkitTask> removalTasks = new HashMap<>();
    private final Map<UUID, List<UUID>> hologramEntitiesByGrave = new HashMap<>();
    private final Map<UUID, BukkitTask> hologramTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<>();
    private final Map<UUID, Inventory> graveChestInventories = new HashMap<>();
    private final GraveTeleportFeedbackGate teleportFeedbackGate = new GraveTeleportFeedbackGate(750L);
    private GraveTeleportCooldown teleportCooldown;
    private GraveMarkerType graveMarkerType;
    private Material graveMarkerMaterial;
    private int graveSearchMaxRadius;
    private boolean hologramEnabled;
    private double hologramBaseHeight;
    private double hologramLineSpacing;
    private long hologramUpdateIntervalTicks;
    private List<String> hologramLines;
    private boolean effectsEnabled;
    private long effectsUpdateIntervalTicks;
    private Particle effectsPrimaryParticle;
    private Particle effectsSecondaryParticle;
    private int effectsPrimaryCount;
    private int effectsSecondaryCount;
    private double effectsSpiralRadius;
    private double effectsSpiralHeight;
    private double effectsVerticalSpeed;
    private boolean effectsAmbientSoundEnabled;
    private Sound effectsAmbientSound;
    private float effectsAmbientSoundVolume;
    private float effectsAmbientSoundPitch;
    private boolean claimAnimationEnabled;
    private long claimAnimationDelayTicks;
    private boolean claimAnimationLightningEnabled;
    private Sound claimAnimationSound;
    private float claimAnimationSoundVolume;
    private float claimAnimationSoundPitch;
    private Set<String> blacklistedWorlds;

    public GraveManager(MaxGraves plugin) {
        this.plugin = plugin;
        this.keys = new NamespacedKeySet(plugin);
        reloadSettings();
    }

    public void reloadSettings() {
        this.graveMarkerType = plugin.getConfigManager().getGraveMarkerType();
        this.teleportCooldown = new GraveTeleportCooldown(
                plugin.getConfigManager().getGraveTeleportCooldownSeconds() * 1000L);
        this.graveMarkerMaterial = graveMarkerType.getMaterial();
        this.graveSearchMaxRadius = Math.max(plugin.getConfigManager().getGraveSearchMaxRadius(), 1);
        this.blacklistedWorlds = plugin.getConfigManager().getGraveBlacklistedWorlds();
        this.hologramEnabled = plugin.getConfigManager().isHologramEnabled();
        this.hologramBaseHeight = plugin.getConfigManager().getHologramBaseHeight();
        this.hologramLineSpacing = plugin.getConfigManager().getHologramLineSpacing();
        this.hologramUpdateIntervalTicks = plugin.getConfigManager().getHologramUpdateIntervalTicks();
        this.hologramLines = plugin.getConfigManager().getHologramLines();
        this.effectsEnabled = plugin.getConfigManager().isEffectsEnabled();
        this.effectsUpdateIntervalTicks = plugin.getConfigManager().getEffectsUpdateIntervalTicks();
        this.effectsPrimaryParticle = resolveParticle(plugin.getConfigManager().getEffectsPrimaryParticle(),
                Particle.SOUL);
        this.effectsSecondaryParticle = resolveParticle(plugin.getConfigManager().getEffectsSecondaryParticle(),
                Particle.SMOKE);
        this.effectsPrimaryCount = plugin.getConfigManager().getEffectsPrimaryCount();
        this.effectsSecondaryCount = plugin.getConfigManager().getEffectsSecondaryCount();
        this.effectsSpiralRadius = plugin.getConfigManager().getEffectsSpiralRadius();
        this.effectsSpiralHeight = plugin.getConfigManager().getEffectsSpiralHeight();
        this.effectsVerticalSpeed = plugin.getConfigManager().getEffectsVerticalSpeed();
        this.effectsAmbientSoundEnabled = plugin.getConfigManager().isEffectsAmbientSoundEnabled();
        this.effectsAmbientSound = resolveSound(
                plugin.getConfigManager().getEffectsAmbientSound(),
                Sound.BLOCK_SOUL_SAND_HIT,
                "grave.effects.ambient-sound.type");
        this.effectsAmbientSoundVolume = plugin.getConfigManager().getEffectsAmbientSoundVolume();
        this.effectsAmbientSoundPitch = plugin.getConfigManager().getEffectsAmbientSoundPitch();
        this.claimAnimationEnabled = plugin.getConfigManager().isClaimAnimationEnabled();
        this.claimAnimationDelayTicks = plugin.getConfigManager().getClaimAnimationDelayTicks();
        this.claimAnimationLightningEnabled = plugin.getConfigManager().isClaimAnimationLightningEnabled();
        this.claimAnimationSound = resolveSound(
                plugin.getConfigManager().getClaimAnimationSound(),
                Sound.ITEM_TOTEM_USE,
                "grave.claim-animation.sound.type");
        this.claimAnimationSoundVolume = plugin.getConfigManager().getClaimAnimationSoundVolume();
        this.claimAnimationSoundPitch = plugin.getConfigManager().getClaimAnimationSoundPitch();

        refreshAllHolograms();
        refreshAllEffects();
    }

    public Optional<Grave> createGrave(Player player, Location deathLocation, List<ItemStack> drops, int droppedExp,
            String killerName, boolean killedByPlayer) {
        if (isWorldBlacklisted(deathLocation)) {
            return Optional.empty();
        }
        Location graveLocation = findValidGraveLocation(deathLocation);
        if (graveLocation == null) {
            return Optional.empty();
        }

        Block block = graveLocation.getBlock();
        if (!placeMarkerBlock(player, block)) {
            return Optional.empty();
        }

        List<ItemStack> storedItems = drops.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getType() != Material.AIR)
                .map(ItemStack::clone)
                .toList();
        if (graveMarkerType == GraveMarkerType.CHEST && storedItems.size() > 54) {
            List<ItemStack> chestItems = new ArrayList<>(storedItems.subList(0, 54));
            List<ItemStack> overflowItems = storedItems.subList(54, storedItems.size());
            Location overflowLocation = block.getLocation().add(0.5D, 0.5D, 0.5D);
            for (ItemStack overflowItem : overflowItems) {
                block.getWorld().dropItemNaturally(overflowLocation, overflowItem.clone());
            }
            storedItems = chestItems;
        }

        Location markerLocation = block.getLocation();

        UUID graveId = UUID.randomUUID();
        long despawnAtMillis = System.currentTimeMillis()
                + (Math.max(plugin.getConfigManager().getGraveDespawnTime(), 1) * 1000L);
        boolean publicAccess = GraveAccessPolicy.resolvePublicAccess(
                plugin.getConfigManager().isPublicPlayerKillAccess(),
                killedByPlayer);
        Grave grave = new Grave(
                graveId,
                player.getUniqueId(),
                player.getName(),
                player.getLevel(),
                killerName,
                markerLocation,
                storedItems,
                Math.max(droppedExp, 0),
                despawnAtMillis,
                graveMarkerType,
                publicAccess);

        gravesById.put(graveId, grave);
        gravesByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new LinkedHashSet<>()).add(graveId);

        indexGraveBlocks(grave);

        createOrUpdateHologram(grave);
        createOrUpdateEffects(grave);
        scheduleAutoRemoval(grave);

        return Optional.of(grave);
    }

    public int giveLocatorsForPlayer(Player player) {
        if (!plugin.getConfigManager().isLocatorMapEnabled()) {
            return 0;
        }

        Set<UUID> existingLocatorTargets = new HashSet<>();
        for (ItemStack item : player.getInventory().getContents()) {
            getLocatorTarget(item).ifPresent(existingLocatorTargets::add);
        }
        int givenLocators = 0;
        for (Grave grave : getGravesByPlayer(player.getUniqueId())) {
            if (!existingLocatorTargets.contains(grave.getId())) {
                giveLocatorMap(player, grave);
                existingLocatorTargets.add(grave.getId());
                givenLocators++;
            }
        }

        return givenLocators;
    }

    public Optional<Grave> getGraveByBlock(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }

        UUID graveId = gravesByBlock.get(toBlockKey(location));
        return graveId != null ? Optional.ofNullable(gravesById.get(graveId)) : Optional.empty();
    }

    public Optional<Grave> getGraveByMarkerBlock(Block block) {
        return getGraveByBlock(block.getLocation());
    }

    public Optional<Grave> getGraveByPlayer(UUID playerId) {
        return getGravesByPlayer(playerId).stream().findFirst();
    }

    public boolean hasActiveGrave(UUID playerId) {
        Set<UUID> graveIds = gravesByPlayer.get(playerId);
        return graveIds != null && graveIds.stream().anyMatch(gravesById::containsKey);
    }

    @SuppressWarnings("null")
    public List<Grave> getGravesByPlayer(UUID playerId) {
        Set<UUID> graveIds = gravesByPlayer.get(playerId);
        if (graveIds == null || graveIds.isEmpty()) {
            return List.of();
        }

        return graveIds.stream()
                .map(gravesById::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(Grave::getDespawnAtMillis))
                .toList();
    }

    public boolean claimGrave(Player player, Grave grave) {
        if (!canAccessGrave(player, grave)) {
            return false;
        }

        List<ItemStack> rewards = grave.getItems().stream()
                .filter(Objects::nonNull)
                .map(ItemStack::clone)
                .toList();
        int rewardExp = Math.max(grave.getExp(), 0);
        Location claimLocation = grave.getLocation().clone().add(0.5D, 0D, 0.5D);

        removeGrave(grave.getId());
        removeLocatorItems(player, grave.getId());

        playClaimAnimation(claimLocation);

        if (claimAnimationDelayTicks <= 0L) {
            deliverClaimRewards(player, claimLocation, rewards, rewardExp);
            return true;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> deliverClaimRewards(player, claimLocation, rewards, rewardExp),
                claimAnimationDelayTicks);

        return true;
    }

    public boolean canAccessGrave(Player player, Grave grave) {
        return grave.getOwner().equals(player.getUniqueId()) || grave.isPublicAccess();
    }

    public boolean teleportOwnerToGrave(Player player, Grave grave) {
        if (!grave.getOwner().equals(player.getUniqueId()) || grave.getLocation().getWorld() == null) {
            return false;
        }

        Location destination = grave.getLocation().clone().add(0.5D, 1D, 0.5D);
        player.teleport(destination);
        if (teleportFeedbackGate.shouldAnnounce(player.getUniqueId(), grave.getId(), System.currentTimeMillis())) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgLocatorUsed()));
        }
        return true;
    }

    public boolean giveLocatorForGrave(Player player, Grave grave) {
        if (!plugin.getConfigManager().isLocatorMapEnabled() || !grave.getOwner().equals(player.getUniqueId())) {
            return false;
        }

        removeLocatorItems(player, grave.getId());
        giveLocatorMap(player, grave);
        return true;
    }

    public void recordPvPCombat(Player firstPlayer, Player secondPlayer) {
        long nowMillis = System.currentTimeMillis();
        teleportCooldown.recordCombat(firstPlayer.getUniqueId(), nowMillis);
        teleportCooldown.recordCombat(secondPlayer.getUniqueId(), nowMillis);
    }

    public boolean isTeleportCooldownActive(Player player) {
        return !teleportCooldown.canTeleport(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getTeleportCooldownRemainingSeconds(Player player) {
        return teleportCooldown.remainingSeconds(player.getUniqueId(), System.currentTimeMillis());
    }

    private void playClaimAnimation(Location location) {
        if (!claimAnimationEnabled || location.getWorld() == null) {
            return;
        }

        World world = location.getWorld();
        world.playSound(location, claimAnimationSound, claimAnimationSoundVolume, claimAnimationSoundPitch);

        if (claimAnimationLightningEnabled) {
            world.strikeLightningEffect(location);
        }
    }

    private void deliverClaimRewards(Player player, Location fallbackLocation, List<ItemStack> items, int exp) {
        if (player.isOnline()) {
            giveItems(player, items);
            player.giveExp(exp);
            return;
        }

        World world = fallbackLocation.getWorld();
        if (world == null) {
            return;
        }

        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                world.dropItemNaturally(fallbackLocation, item.clone());
            }
        }

        if (exp > 0) {
            world.spawn(fallbackLocation, ExperienceOrb.class, orb -> orb.setExperience(exp));
        }
    }

    public void removeGraveForPlayer(UUID playerId, boolean removeLocator) {
        Set<UUID> graveIds = gravesByPlayer.get(playerId);
        if (graveIds == null || graveIds.isEmpty()) {
            return;
        }

        Player onlinePlayer = removeLocator ? Bukkit.getPlayer(playerId) : null;

        for (UUID graveId : new HashSet<>(graveIds)) {
            removeGrave(graveId);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                removeLocatorItems(onlinePlayer, graveId);
            }
        }
    }

    public Optional<UUID> getLocatorTarget(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP || !item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        @SuppressWarnings("null")
        String rawId = pdc.get(keys.graveIdKey(), PersistentDataType.STRING);

        if (rawId == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(rawId));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<Grave> getGraveById(UUID id) {
        return Optional.ofNullable(gravesById.get(id));
    }

    public void clearAll() {
        new HashSet<>(gravesById.keySet()).forEach(this::removeGrave);
        teleportFeedbackGate.clear();
        if (teleportCooldown != null) {
            teleportCooldown.clear();
        }
    }

    @SuppressWarnings("null")
    public boolean openGraveChest(Player player, Grave grave) {
        if (grave.getMarkerType() != GraveMarkerType.CHEST || !canAccessGrave(player, grave)) {
            return false;
        }

        Inventory inventory = graveChestInventories.computeIfAbsent(grave.getId(),
                ignored -> createGraveChestInventory(grave));
        player.openInventory(inventory);
        return true;
    }

    public void syncGraveChestInventory(Player player, UUID graveId, Inventory inventory) {
        Grave grave = gravesById.get(graveId);
        if (grave == null || grave.getMarkerType() != GraveMarkerType.CHEST) {
            return;
        }

        grave.replaceItemsFromInventory(inventory);
        if (!grave.isEmpty()) {
            return;
        }

        completeEmptyChestGrave(player, grave);
    }

    private Inventory createGraveChestInventory(Grave grave) {
        int size = Math.min(54, Math.max(9, ((grave.getItems().size() - 1) / 9 + 1) * 9));
        GraveChestHolder holder = new GraveChestHolder(grave.getId());
        Inventory inventory = Bukkit.createInventory(holder, size);
        holder.setInventory(inventory);
        for (ItemStack item : grave.getItems()) {
            if (item != null && item.getType() != Material.AIR) {
                inventory.addItem(item.clone());
            }
        }
        return inventory;
    }

    private void completeEmptyChestGrave(Player player, Grave grave) {
        int rewardExp = Math.max(grave.getExp(), 0);
        UUID graveId = grave.getId();

        removeGrave(graveId);

        Player owner = Bukkit.getPlayer(grave.getOwner());
        if (owner != null && owner.isOnline()) {
            removeLocatorItems(owner, graveId);
        }

        if (rewardExp > 0) {
            player.giveExp(rewardExp);
        }

        player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveClaimed()));
    }

    private void giveItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (tryAutoEquip(player, item)) {
                continue;
            }

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
            overflow.values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        }
    }

    private void removeGrave(UUID graveId) {
        Grave grave = gravesById.get(graveId);
        if (grave == null) {
            return;
        }

        deindexGraveBlocks(grave);
        gravesById.remove(graveId);

        Set<UUID> ownerGraves = gravesByPlayer.get(grave.getOwner());
        if (ownerGraves != null) {
            ownerGraves.remove(graveId);
            if (ownerGraves.isEmpty()) {
                gravesByPlayer.remove(grave.getOwner());
            }
        }

        BukkitTask task = removalTasks.remove(graveId);
        if (task != null) {
            task.cancel();
        }

        removeHologram(graveId);
        removeEffects(graveId);
        closeAndRemoveChestInventory(graveId);

        Location location = grave.getLocation();
        Block block = location.getBlock();
        Material markerMaterial = grave.getMarkerType().getMaterial();
        if (block.getType() == markerMaterial
                || (markerMaterial == Material.PLAYER_HEAD && block.getType() == Material.PLAYER_WALL_HEAD)) {
            if (block.getState() instanceof Container container) {
                container.getInventory().clear();
                container.update(true, false);
            }
            block.setType(Material.AIR, false);
        }
    }

    private void closeAndRemoveChestInventory(UUID graveId) {
        Inventory inventory = graveChestInventories.remove(graveId);
        if (inventory == null) {
            return;
        }

        for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
            viewer.closeInventory();
        }
    }

    private void removeLocatorItems(Player player, UUID graveId) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            Optional<UUID> target = getLocatorTarget(item);
            if (target.isPresent() && target.get().equals(graveId)) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private void scheduleAutoRemoval(Grave grave) {
        long ticks = Math.max((grave.getDespawnAtMillis() - System.currentTimeMillis()) / 50L, 1L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeGrave(grave.getId());
            Player owner = Bukkit.getPlayer(grave.getOwner());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveExpired()));
                removeLocatorItems(owner, grave.getId());
            }
        }, ticks);

        removalTasks.put(grave.getId(), task);
    }

    private void refreshAllHolograms() {
        for (UUID graveId : new HashSet<>(hologramEntitiesByGrave.keySet())) {
            removeHologram(graveId);
        }

        for (Grave grave : gravesById.values()) {
            createOrUpdateHologram(grave);
        }
    }

    private void refreshAllEffects() {
        for (UUID graveId : new HashSet<>(particleTasks.keySet())) {
            removeEffects(graveId);
        }

        for (Grave grave : gravesById.values()) {
            createOrUpdateEffects(grave);
        }
    }

    private void createOrUpdateEffects(Grave grave) {
        removeEffects(grave.getId());

        if (!grave.getMarkerType().supportsEffects()) {
            return;
        }

        if (!effectsEnabled || grave.getLocation().getWorld() == null) {
            return;
        }

        UUID graveId = grave.getId();
        BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private double angle;
            private double offsetY;

            @Override
            public void run() {
                Location markerLocation = grave.getLocation();
                World world = markerLocation.getWorld();
                if (world == null) {
                    return;
                }

                Location center = markerLocation.getBlock().getLocation().add(0.5D, 0.25D, 0.5D);

                double x = center.getX() + (Math.cos(angle) * effectsSpiralRadius);
                double z = center.getZ() + (Math.sin(angle) * effectsSpiralRadius);
                double y = center.getY() + offsetY;

                world.spawnParticle(effectsPrimaryParticle, x, y, z, effectsPrimaryCount, 0.05D, 0.08D, 0.05D, 0.005D);
                world.spawnParticle(effectsSecondaryParticle, center.getX(),
                        center.getY() + (effectsSpiralHeight * 0.5D), center.getZ(), effectsSecondaryCount, 0.25D,
                        0.45D, 0.25D, 0.002D);

                if (effectsAmbientSoundEnabled && ThreadLocalRandom.current().nextDouble() <= 0.09D) {
                    world.playSound(center, effectsAmbientSound, effectsAmbientSoundVolume, effectsAmbientSoundPitch);
                }

                angle += 0.33D;
                if (angle >= Math.PI * 2) {
                    angle -= Math.PI * 2;
                }

                offsetY += effectsVerticalSpeed;
                if (offsetY > effectsSpiralHeight) {
                    offsetY = 0D;
                }
            }
        }, effectsUpdateIntervalTicks, effectsUpdateIntervalTicks);

        particleTasks.put(graveId, particleTask);
    }

    private void createOrUpdateHologram(Grave grave) {
        removeHologram(grave.getId());

        if (!hologramEnabled || hologramLines.isEmpty()) {
            return;
        }

        if (grave.getLocation().getWorld() == null) {
            return;
        }

        Location hologramAnchor = grave.getLocation().getBlock().getLocation().add(0.5D, 0.0D, 0.5D);

        List<UUID> entityIds = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < hologramLines.size(); lineIndex++) {
            Location lineLocation = hologramAnchor.clone().add(
                    0.0D,
                    hologramBaseHeight + ((hologramLines.size() - 1 - lineIndex) * hologramLineSpacing),
                    0.0D);

            @SuppressWarnings("null")
            ArmorStand stand = lineLocation.getWorld().spawn(lineLocation, ArmorStand.class, spawned -> {
                spawned.setInvisible(true);
                spawned.setInvulnerable(true);
                spawned.setMarker(true);
                spawned.setGravity(false);
                spawned.setSilent(true);
                spawned.setCollidable(false);
                spawned.setCanPickupItems(false);
                spawned.setCustomNameVisible(true);
                spawned.setPersistent(false);
            });

            stand.customName(LegacyComponentSerializer.legacySection().deserialize(getHologramLine(grave, lineIndex)));
            entityIds.add(stand.getUniqueId());
        }

        hologramEntitiesByGrave.put(grave.getId(), entityIds);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> updateHologramText(grave),
                hologramUpdateIntervalTicks, hologramUpdateIntervalTicks);
        hologramTasks.put(grave.getId(), task);
    }

    private void updateHologramText(Grave grave) {
        List<UUID> entityIds = hologramEntitiesByGrave.get(grave.getId());
        if (entityIds == null || entityIds.isEmpty()) {
            return;
        }

        if (grave.getLocation().getWorld() == null) {
            return;
        }

        for (int i = 0; i < entityIds.size(); i++) {
            Entity entity = Bukkit.getEntity(entityIds.get(i));
            if (!(entity instanceof ArmorStand stand) || stand.isDead()) {
                continue;
            }
            stand.customName(LegacyComponentSerializer.legacySection().deserialize(getHologramLine(grave, i)));
        }
    }

    private void removeHologram(UUID graveId) {
        BukkitTask hologramTask = hologramTasks.remove(graveId);
        if (hologramTask != null) {
            hologramTask.cancel();
        }

        List<UUID> entities = hologramEntitiesByGrave.remove(graveId);
        if (entities == null) {
            return;
        }

        for (UUID entityId : entities) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private void removeEffects(UUID graveId) {
        BukkitTask effectTask = particleTasks.remove(graveId);
        if (effectTask != null) {
            effectTask.cancel();
        }
    }

    private String getHologramLine(Grave grave, int index) {
        if (index < 0 || index >= hologramLines.size()) {
            return "";
        }

        String killerName = grave.getKillerName() == null || grave.getKillerName().isBlank()
                ? plugin.getConfigManager().getMsgHologramUnknownKiller()
                : grave.getKillerName();
        Location location = grave.getLocation();
        String replaced = hologramLines.get(index)
                .replace("{player}", grave.getOwnerName())
                .replace("{player_level}", String.valueOf(grave.getOwnerLevel()))
                .replace("{killer}", killerName)
                .replace("{x}", String.valueOf(location.getBlockX()))
                .replace("{y}", String.valueOf(location.getBlockY()))
                .replace("{z}", String.valueOf(location.getBlockZ()))
                .replace("{time_left}", formatTimeLeft(grave.getDespawnAtMillis()));

        return MessageUtils.getColoredMessage(replaced);
    }

    private String formatTimeLeft(long despawnAtMillis) {
        long totalSeconds = Math.max((despawnAtMillis - System.currentTimeMillis()) / 1000L, 0L);

        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        return hours + "h " + minutes + "m " + seconds + "s";
    }

    private void giveLocatorMap(Player player, Grave grave) {
        ItemStack locator = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = locator.getItemMeta();
        if (meta == null) {
            return;
        }

        Location loc = grave.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";

        meta.displayName(MessageUtils.getColoredItemComponent(plugin.getConfigManager().getMsgLocatorItemName()));

        String worldLine = plugin.getConfigManager().getMsgLocatorItemWorld()
                .replace("{world}", worldName);
        String coordinatesLine = plugin.getConfigManager().getMsgLocatorItemCoordinates()
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()));

        meta.lore(List.of(
                MessageUtils.getColoredItemComponent(worldLine),
                MessageUtils.getColoredItemComponent(coordinatesLine),
                MessageUtils.getColoredItemComponent(plugin.getConfigManager().getMsgLocatorItemAction())));

        meta.getPersistentDataContainer().set(keys.graveIdKey(), PersistentDataType.STRING, grave.getId().toString());
        locator.setItemMeta(meta);

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(locator);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private Location findValidGraveLocation(Location deathLocation) {
        List<Location> candidates = List.of(
                deathLocation,
                deathLocation.clone().add(0, 1, 0),
                deathLocation.clone().add(0, -1, 0));

        for (Location candidate : candidates) {
            if (isValidGravePlacement(candidate)) {
                return candidate;
            }
        }

        for (int radius = 1; radius <= graveSearchMaxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius) {
                            continue;
                        }

                        Location around = deathLocation.clone().add(x, y, z);
                        if (isValidGravePlacement(around)) {
                            return around;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isValidGravePlacement(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Block block = location.getBlock();
        if (!isMarkerReplaceable(block.getType())) {
            return false;
        }

        return !hasVerticalGraveConflict(location);
    }

    private boolean isMarkerReplaceable(Material material) {
        return material.isAir()
                || Tag.FLOWERS.isTagged(material)
                || Tag.SMALL_FLOWERS.isTagged(material)
                || material == Material.TALL_GRASS
                || material == Material.SHORT_GRASS
                || material == Material.FERN
                || material == Material.LARGE_FERN
                || material == Material.DEAD_BUSH
                || material == Material.SNOW;
    }

    private boolean hasVerticalGraveConflict(Location location) {
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            Location checkLocation = location.clone().add(0, offsetY, 0);
            if (getGraveByBlock(checkLocation).isPresent()) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean placeMarkerBlock(Player player, Block block) {
        block.setType(graveMarkerMaterial, false);

        if (graveMarkerType == GraveMarkerType.HEAD && block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.update(true, false);
        }

        return block.getType() == graveMarkerMaterial;
    }

    public Material getGraveMarkerMaterial() {
        return graveMarkerMaterial;
    }

    public boolean isWorldBlacklisted(Location location) {
        if (location == null || location.getWorld() == null || blacklistedWorlds.isEmpty()) {
            return false;
        }

        return blacklistedWorlds.contains(location.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    private Particle resolveParticle(String configuredParticle, Particle fallback) {
        Particle resolved = ParticleNameResolver.resolve(configuredParticle, fallback);
        if (!ParticleNameResolver.isKnown(configuredParticle)
                && configuredParticle != null
                && !configuredParticle.isBlank()) {
            plugin.getLogger().warning("Invalid particle for grave.effects: " + configuredParticle
                    + ". Falling back to " + fallback + '.');
        }

        return resolved;
    }

    private Sound resolveSound(String configuredSound, Sound fallback, String configPath) {
        if (configuredSound == null || configuredSound.isBlank()) {
            return fallback;
        }

        String trimmedSound = configuredSound.trim();

        NamespacedKey key = NamespacedKey.fromString(trimmedSound.toLowerCase(Locale.ROOT));
        if (key != null) {
            Sound resolved = Registry.SOUNDS.get(key);
            if (resolved != null) {
                return resolved;
            }
        }

        String canonicalInput = trimmedSound.toLowerCase(Locale.ROOT).replace("_", "").replace(".", "");
        for (Sound sound : Registry.SOUNDS) {
            NamespacedKey soundKey = Registry.SOUNDS.getKey(sound);
            if (soundKey == null) {
                continue;
            }
            String soundKeyStr = soundKey.getKey().toLowerCase(Locale.ROOT).replace("_", "").replace(".", "");
            if (soundKeyStr.equals(canonicalInput)) {
                return sound;
            }
        }

        plugin.getLogger().warning("Unknown sound for " + configPath + ": " + configuredSound
                + ". Falling back to " + fallback + '.');
        return fallback;
    }

    private void indexGraveBlocks(Grave grave) {
        gravesByBlock.put(toBlockKey(grave.getLocation()), grave.getId());

    }

    private void deindexGraveBlocks(Grave grave) {
        gravesByBlock.remove(toBlockKey(grave.getLocation()));
    }

    private GraveBlockKey toBlockKey(Location location) {
        return new GraveBlockKey(
                location.getWorld().getUID(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private record GraveBlockKey(UUID worldId, int x, int y, int z) {
    }

    private boolean tryAutoEquip(Player player, ItemStack item) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null || item == null) {
            return false;
        }

        EquipmentSlot slot = item.getType().getEquipmentSlot();
        if (slot == null) {
            return false;
        }

        if (slot == EquipmentSlot.HAND || slot == EquipmentSlot.BODY) {
            return false;
        }

        ItemStack equippedItem = equipment.getItem(slot);
        if (equippedItem != null && equippedItem.getType() != Material.AIR) {
            return false;
        }

        if (item.getAmount() > 1) {
            return false;
        }

        switch (slot) {
            case HEAD -> equipment.setHelmet(item.clone());
            case CHEST -> equipment.setChestplate(item.clone());
            case LEGS -> equipment.setLeggings(item.clone());
            case FEET -> equipment.setBoots(item.clone());
            case OFF_HAND -> equipment.setItemInOffHand(item.clone());
            default -> {
                return false;
            }
        }

        return true;
    }
}
