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

import java.util.*;

public class GraveManager {

    private final MaxGraves plugin;
    private final NamespacedKeySet keys;
    private final Map<UUID, Grave> gravesById = new HashMap<>();
    private final Map<UUID, Set<UUID>> gravesByPlayer = new HashMap<>();
    private final Map<UUID, BukkitTask> removalTasks = new HashMap<>();
    private final Map<UUID, List<UUID>> hologramEntitiesByGrave = new HashMap<>();
    private final Map<UUID, BukkitTask> hologramTasks = new HashMap<>();
    private Material graveMarkerMaterial;
    private boolean hologramEnabled;
    private double hologramBaseHeight;
    private double hologramLineSpacing;
    private long hologramUpdateIntervalTicks;
    private List<String> hologramLines;

    public GraveManager(MaxGraves plugin) {
        this.plugin = plugin;
        this.keys = new NamespacedKeySet(plugin);
        reloadSettings();
    }


    public void reloadSettings() {
        this.graveMarkerMaterial = resolveMarkerMaterial(plugin.getConfigManager().getGraveMarkerBlock());
        this.hologramEnabled = plugin.getConfigManager().isHologramEnabled();
        this.hologramBaseHeight = plugin.getConfigManager().getHologramBaseHeight();
        this.hologramLineSpacing = plugin.getConfigManager().getHologramLineSpacing();
        this.hologramUpdateIntervalTicks = plugin.getConfigManager().getHologramUpdateIntervalTicks();
        this.hologramLines = plugin.getConfigManager().getHologramLines();

        refreshAllHolograms();
    }

    public Optional<Grave> createGrave(Player player, Location deathLocation, List<ItemStack> drops, int droppedExp, String killerName) {
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

        UUID graveId = UUID.randomUUID();
        long despawnAtMillis = System.currentTimeMillis() + (Math.max(plugin.getConfigManager().getGraveDespawnTime(), 1) * 1000L);
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
                despawnAtMillis
        );

        gravesById.put(graveId, grave);
        gravesByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new LinkedHashSet<>()).add(graveId);

        createOrUpdateHologram(grave);
        scheduleAutoRemoval(grave);

        return Optional.of(grave);
    }

    public int giveLocatorsForPlayer(Player player) {
        int givenLocators = 0;
        for (Grave grave : getGravesByPlayer(player.getUniqueId())) {
            boolean hasLocator = Arrays.stream(player.getInventory().getContents())
                    .map(this::getLocatorTarget)
                    .anyMatch(target -> target.isPresent() && target.get().equals(grave.getId()));

            if (!hasLocator) {
                giveLocatorMap(player, grave);
                givenLocators++;
            }
        }

        return givenLocators;
    }

    public Optional<Grave> getGraveByBlock(Location location) {
        return gravesById.values().stream()
                .filter(grave -> {
                    Location loc = grave.getLocation();
                    Location secondaryLoc = grave.getSecondaryLocation();

                    return isSameBlockLocation(loc, location) || isSameBlockLocation(secondaryLoc, location);
                })
                .findFirst();
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

        giveItems(player, grave.getItems());
        player.giveExp(grave.getExp());

        removeGrave(grave.getId(), true);
        removeLocatorItems(player, grave.getId());

        return true;
    }

    public void removeGraveForPlayer(UUID playerId, boolean removeLocator) {
        Set<UUID> graveIds = gravesByPlayer.get(playerId);
        if (graveIds == null || graveIds.isEmpty()) {
            return;
        }

        for (UUID graveId : new HashSet<>(graveIds)) {
            removeGrave(graveId, removeLocator);
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
        new HashSet<>(gravesById.keySet()).forEach(graveId -> removeGrave(graveId, false));
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

    private void removeGrave(UUID graveId, boolean dropContents) {
        Grave grave = gravesById.remove(graveId);
        if (grave == null) {
            return;
        }

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

        Location location = grave.getLocation();
        Block block = location.getBlock();
        if (block.getType() == graveMarkerMaterial || block.getType() == Material.CHEST || block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
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

    private void removeLocatorItems(Player player, UUID graveId) {
        for (ItemStack item : player.getInventory().getContents()) {
            Optional<UUID> target = getLocatorTarget(item);
            if (target.isPresent() && target.get().equals(graveId)) {
                player.getInventory().remove(item);
            }
        }
    }

    private void scheduleAutoRemoval(Grave grave) {
        long ticks = Math.max((grave.getDespawnAtMillis() - System.currentTimeMillis()) / 50L, 1L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeGrave(grave.getId(), true);
            Player owner = Bukkit.getPlayer(grave.getOwner());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveExpired()));
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
                    0.0D
            );

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

            stand.setCustomName(getHologramLine(grave, lineIndex));
            entityIds.add(stand.getUniqueId());
        }

        hologramEntitiesByGrave.put(grave.getId(), entityIds);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> updateHologramText(grave), hologramUpdateIntervalTicks, hologramUpdateIntervalTicks);
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
            stand.setCustomName(getHologramLine(grave, i));
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

    private String getHologramLine(Grave grave, int index) {
        if (index < 0 || index >= hologramLines.size()) {
            return "";
        }

        String killerName = grave.getKillerName() == null || grave.getKillerName().isBlank() ? plugin.getConfigManager().getMsgHologramUnknownKiller() : grave.getKillerName();
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

        meta.setDisplayName(MessageUtils.getColoredMessage(plugin.getConfigManager().getMsgLocatorItemName()));

        String worldLine = plugin.getConfigManager().getMsgLocatorItemWorld()
                .replace("{world}", worldName);
        String coordinatesLine = plugin.getConfigManager().getMsgLocatorItemCoordinates()
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()));

        meta.setLore(List.of(
                MessageUtils.getColoredMessage(worldLine),
                MessageUtils.getColoredMessage(coordinatesLine),
                MessageUtils.getColoredMessage(plugin.getConfigManager().getMsgLocatorItemAction())
        ));

        meta.getPersistentDataContainer().set(keys.graveIdKey(), PersistentDataType.STRING, grave.getId().toString());
        locator.setItemMeta(meta);

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(locator);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private Location findValidGraveLocation(Location deathLocation) {
        List<Location> candidates = List.of(
                deathLocation,
                deathLocation.clone().add(0, 1, 0),
                deathLocation.clone().add(0, -1, 0)
        );

        for (Location candidate : candidates) {
            if (isChestPlaceable(candidate)) {
                return candidate;
            }
        }

        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location around = deathLocation.clone().add(x, y, z);
                    if (isChestPlaceable(around)) {
                        return around;
                    }
                }
            }
        }

        return null;
    }

    private boolean isChestPlaceable(Location location) {
        Block block = location.getBlock();
        return block.getType().isAir() || block.isPassable();
    }

    private boolean placeMarkerBlock(Player player, Block block) {
        block.setType(graveMarkerMaterial, false);

        if (graveMarkerMaterial == Material.PLAYER_HEAD && block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.update(true, false);
        }

        return block.getType() == graveMarkerMaterial;
    }

    public Material getGraveMarkerMaterial() {
        return graveMarkerMaterial;
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
