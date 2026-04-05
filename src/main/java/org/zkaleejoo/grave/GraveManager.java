package org.zkaleejoo.grave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.utils.MessageUtils;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;

import java.util.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;


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
        this.graveMarkerMaterial = resolveMarkerMaterial(plugin.getConfigManager().getGraveMarkerBlock());
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
            String killerName) {
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

        Location markerLocation = block.getLocation();
        Map<Location, BlockData> originalBlocks = corruptEnvironment(markerLocation);

        UUID graveId = UUID.randomUUID();
        long despawnAtMillis = System.currentTimeMillis()
                + (Math.max(plugin.getConfigManager().getGraveDespawnTime(), 1) * 1000L);
        Grave grave = new Grave(
                graveId,
                player.getUniqueId(),
                player.getName(),
                player.getLevel(),
                killerName,
                markerLocation,
                null,
                storedItems,
                Math.max(droppedExp, 0),
                despawnAtMillis,
                originalBlocks);

        gravesById.put(graveId, grave);
        gravesByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new LinkedHashSet<>()).add(graveId);

        indexGraveBlocks(grave);

        createOrUpdateHologram(grave);
        createOrUpdateEffects(grave);
        scheduleAutoRemoval(grave);

        return Optional.of(grave);
    }

    public int giveLocatorsForPlayer(Player player) {
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

    public Optional<Grave> getGraveByChestBlock(Block block) {
        return getGraveByMarkerBlock(block);
    }

    public Optional<Grave> getGraveByMarkerBlock(Block block) {
        Optional<Grave> directMatch = getGraveByBlock(block.getLocation());
        if (directMatch.isPresent()) {
            return directMatch;
        }

        if (block.getType() != Material.CHEST) {
            return Optional.empty();
        }

        if (!(block.getState() instanceof Chest chest)) {
            return Optional.empty();
        }

        InventoryHolder holder = chest.getInventory().getHolder();
        if (!(holder instanceof DoubleChest doubleChest)) {
            return Optional.empty();
        }

        if (doubleChest.getLeftSide() instanceof Chest leftChest) {
            Optional<Grave> leftMatch = getGraveByBlock(leftChest.getLocation());
            if (leftMatch.isPresent()) {
                return leftMatch;
            }
        }

        if (doubleChest.getRightSide() instanceof Chest rightChest) {
            return getGraveByBlock(rightChest.getLocation());
        }

        return Optional.empty();
    }

    public Optional<Grave> getGraveByPlayer(UUID playerId) {
        return getGravesByPlayer(playerId).stream().findFirst();
    }

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
        if (!grave.getOwner().equals(player.getUniqueId())) {
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

        restoreEnvironment(grave);

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

        Location location = grave.getLocation();
        Block block = location.getBlock();
        if (block.getType() == graveMarkerMaterial || block.getType() == Material.CHEST
                || block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
            block.setType(Material.AIR, false);
        }

        Location secondaryLocation = grave.getSecondaryLocation();
        if (secondaryLocation != null) {
            Block secondaryBlock = secondaryLocation.getBlock();
            if (secondaryBlock.getType() == Material.CHEST) {
                secondaryBlock.setType(Material.AIR, false);
            }
        }
    }

    private Map<Location, BlockData> corruptEnvironment(Location markerLocation) {
        Map<Location, BlockData> originalBlocks = new HashMap<>();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location target = markerLocation.clone().add(x, 0, z);
                if (isSameBlockLocation(target, markerLocation)) {
                    continue;
                }

                Block nearbyBlock = resolveCorruptionTargetBlock(target);
                originalBlocks.put(nearbyBlock.getLocation(), nearbyBlock.getBlockData().clone());

                Material corruptedType = getCorruptedType(nearbyBlock.getType());
                if (corruptedType != nearbyBlock.getType()) {
                    nearbyBlock.setType(corruptedType, false);
                }
            }
        }

        return originalBlocks;
    }

    private Block resolveCorruptionTargetBlock(Location location) {
        Block block = location.getBlock();
        if (!(block.getType().isAir() || block.isPassable())) {
            return block;
        }

        Block below = block.getRelative(0, -1, 0);
        if (below.getY() >= below.getWorld().getMinHeight()) {
            return below;
        }

        return block;
    }

    private void restoreEnvironment(Grave grave) {
        for (Map.Entry<Location, BlockData> entry : grave.getOriginalBlocks().entrySet()) {
            Location location = entry.getKey();
            BlockData originalData = entry.getValue();

            if (location == null || originalData == null || location.getWorld() == null) {
                continue;
            }

            location.getBlock().setBlockData(originalData.clone(), false);
        }
    }

    private Material getCorruptedType(Material material) {
        if (material == Material.GRASS_BLOCK) {
            return Material.PODZOL;
        }

        if (material == Material.STONE) {
            return ThreadLocalRandom.current().nextBoolean() ? Material.GRANITE : Material.ANDESITE;
        }

        if (Tag.FLOWERS.isTagged(material) || Tag.SMALL_FLOWERS.isTagged(material)) {
            return Material.AIR;
        }

        return material;
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

        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(MessageUtils.getColoredMessage(plugin.getConfigManager().getMsgLocatorItemName())));

        String worldLine = plugin.getConfigManager().getMsgLocatorItemWorld()
                .replace("{world}", worldName);
        String coordinatesLine = plugin.getConfigManager().getMsgLocatorItemCoordinates()
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()));

        meta.lore(List.of(
                LegacyComponentSerializer.legacySection().deserialize(MessageUtils.getColoredMessage(worldLine)),
                LegacyComponentSerializer.legacySection().deserialize(MessageUtils.getColoredMessage(coordinatesLine)),
                LegacyComponentSerializer.legacySection().deserialize(MessageUtils.getColoredMessage(plugin.getConfigManager().getMsgLocatorItemAction()))));

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

    private boolean placeMarkerBlock(Player player, Block block) {
        block.setType(graveMarkerMaterial, false);

        if (graveMarkerMaterial == Material.PLAYER_HEAD && block.getState() instanceof Skull skull) {
            skull.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
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

    private Material resolveMarkerMaterial(String configuredMaterial) {
        if (configuredMaterial == null || configuredMaterial.isBlank()) {
            return Material.PLAYER_HEAD;
        }

        Material resolved = Material.matchMaterial(configuredMaterial.trim(), false);
        if (resolved == Material.CHEST || resolved == Material.PLAYER_HEAD) {
            return resolved;
        }

        return Material.PLAYER_HEAD;
    }

    private Particle resolveParticle(String configuredParticle, Particle fallback) {
        if (configuredParticle == null || configuredParticle.isBlank()) {
            return fallback;
        }

        try {
            return Particle.valueOf(configuredParticle.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid particle for grave.effects: " + configuredParticle
                    + ". Falling back to " + fallback + '.');
            return fallback;
        }
    }

    private Sound resolveSound(String configuredSound, Sound fallback, String configPath) {
        if (configuredSound == null || configuredSound.isBlank()) {
            return fallback;
        }

        String trimmedSound = configuredSound.trim();

        try {
            return Sound.valueOf(trimmedSound.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
        }

        NamespacedKey key = NamespacedKey.fromString(trimmedSound.toLowerCase(Locale.ROOT));
        if (key == null) {
            plugin.getLogger().warning("Invalid sound key for " + configPath + ": " + configuredSound
                    + ". Falling back to " + fallback + '.');
            return fallback;
        }

        Sound resolved = Registry.SOUNDS.get(key);
        if (resolved == null) {
            plugin.getLogger().warning("Unknown sound for " + configPath + ": " + configuredSound
                    + ". Falling back to " + fallback + '.');
            return fallback;
        }

        return resolved;
    }

    private boolean isSameBlockLocation(Location first, Location second) {
        return first != null
                && second != null
                && first.getWorld() != null
                && second.getWorld() != null
                && first.getWorld().getUID().equals(second.getWorld().getUID())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private void indexGraveBlocks(Grave grave) {
        gravesByBlock.put(toBlockKey(grave.getLocation()), grave.getId());

        Location secondaryLocation = grave.getSecondaryLocation();
        if (secondaryLocation != null && secondaryLocation.getWorld() != null) {
            gravesByBlock.put(toBlockKey(secondaryLocation), grave.getId());
        }
    }

    private void deindexGraveBlocks(Grave grave) {
        gravesByBlock.remove(toBlockKey(grave.getLocation()));

        Location secondaryLocation = grave.getSecondaryLocation();
        if (secondaryLocation != null && secondaryLocation.getWorld() != null) {
            gravesByBlock.remove(toBlockKey(secondaryLocation));
        }
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
