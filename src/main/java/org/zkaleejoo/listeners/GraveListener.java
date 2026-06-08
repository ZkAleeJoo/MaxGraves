package org.zkaleejoo.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.grave.Grave;
import org.zkaleejoo.grave.GraveChestHolder;
import org.zkaleejoo.grave.GraveCreationPolicy;
import org.zkaleejoo.grave.GraveMarkerType;
import org.zkaleejoo.utils.MessageUtils;
import java.util.Optional;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.zkaleejoo.commands.InfoMenuHolder;

public class GraveListener implements Listener {

    private static final String TELEPORT_PERMISSION = "maxgrave.tp";

    private final MaxGraves plugin;
    private final Map<UUID, DeathSnapshot> deathSnapshots = new HashMap<>();

    public GraveListener(MaxGraves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathSnapshot(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isCreateOnDeath()) {
            return;
        }

        Player player = (Player) event.getEntity();
        long worldTick = player.getWorld().getFullTime();
        deathSnapshots.put(player.getUniqueId(), new DeathSnapshot(
                worldTick,
                copyItems(player.getInventory().getContents()),
                Math.max(event.getDroppedExp(), 0),
                Math.max(player.getTotalExperience(), 0)));
        logDeathDebug(
                "LOWEST_SNAPSHOT",
                player,
                event,
                "snapshotItems=" + deathSnapshots.get(player.getUniqueId()).items().size()
                        + ", snapshotDroppedExp=" + deathSnapshots.get(player.getUniqueId()).snapshotDroppedExp()
                        + ", totalExp=" + deathSnapshots.get(player.getUniqueId()).totalExperience());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathFinalize(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isCreateOnDeath()) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();
        long worldTick = player.getWorld().getFullTime();

        DeathSnapshot snapshot = deathSnapshots.get(playerId);
        if (snapshot == null || snapshot.deathTick() != worldTick) {
            snapshot = new DeathSnapshot(
                    worldTick,
                    copyItems(player.getInventory().getContents()),
                    Math.max(event.getDroppedExp(), 0),
                    Math.max(player.getTotalExperience(), 0));
            deathSnapshots.put(playerId, snapshot);
        }

        logDeathDebug(
                "MONITOR_BEFORE",
                player,
                event,
                "snapshotPresent=" + (snapshot != null)
                        + ", snapshotItems=" + snapshot.items().size()
                        + ", snapshotDroppedExp=" + snapshot.snapshotDroppedExp()
                        + ", snapshotTotalExp=" + snapshot.totalExperience());

        if (snapshot.processed()) {
            logDeathDebug("MONITOR_SKIP_PROCESSED", player, event, "alreadyProcessed=true");
            return;
        }
        deathSnapshots.put(playerId, snapshot.markProcessed());

        if (plugin.getGraveManager().isWorldBlacklisted(player.getLocation())) {
            logDeathDebug("MONITOR_ABORT", player, event, "reason=world_blacklisted");
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgWorldBlacklisted()));
            deathSnapshots.remove(playerId);
            return;
        }

        if (event.getKeepInventory()) {
            logDeathDebug("MONITOR_ABORT", player, event, "reason=keep_inventory_true");
            deathSnapshots.remove(playerId);
            return;
        }

        if (!GraveCreationPolicy.shouldCreateGrave(
                plugin.getConfigManager().isSingleActiveGraveLimit(),
                plugin.getGraveManager().hasActiveGrave(playerId))) {
            logDeathDebug("MONITOR_ABORT", player, event, "reason=single_active_grave_limit");
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgActiveGraveLimit()));
            deathSnapshots.remove(playerId);
            return;
        }

        List<ItemStack> graveItems = !snapshot.items().isEmpty()
                ? copyItems(snapshot.items())
                : copyItems(event.getDrops());
        int graveExp = event.getKeepLevel() ? 0 : resolveGraveExp(event, snapshot);

        String killerName = resolveKillerName(player);
        boolean killedByPlayer = wasKilledByPlayer(player);

        plugin.getGraveManager()
                .createGrave(player, player.getLocation(), graveItems, graveExp, killerName, killedByPlayer)
                .ifPresentOrElse(grave -> {
                    int dropsBeforeClear = event.getDrops().size();
                    int expBeforeClear = event.getDroppedExp();
                    event.getDrops().clear();
                    event.setDroppedExp(0);

                    logDeathDebug(
                            "MONITOR_GRAVE_CREATED",
                            player,
                            event,
                            "graveId=" + grave.getId()
                                    + ", sourceItems=" + graveItems.size()
                                    + ", sourceExp=" + graveExp
                                    + ", dropsBeforeClear=" + dropsBeforeClear
                                    + ", expBeforeClear=" + expBeforeClear
                                    + ", dropsAfterClear=" + event.getDrops().size()
                                    + ", expAfterClear=" + event.getDroppedExp());

                    player.sendMessage(MessageUtils.getColoredMessage(
                            plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveCreated()));
                }, () -> {
                    logDeathDebug(
                            "MONITOR_GRAVE_CREATE_FAIL",
                            player,
                            event,
                            "sourceItems=" + graveItems.size()
                                    + ", sourceExp=" + graveExp
                                    + ", reason=grave_manager_returned_empty");
                    player.sendMessage(MessageUtils.getColoredMessage(
                            plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveCreateFail()));
                });
        logDeathDebug(
                "MONITOR_AFTER",
                player,
                event,
                "finalDrops=" + event.getDrops().size() + ", finalDroppedExp=" + event.getDroppedExp());
        deathSnapshots.remove(playerId);
    }

    private int resolveGraveExp(PlayerDeathEvent event, DeathSnapshot snapshot) {
        int eventExp = Math.max(event.getDroppedExp(), 0);
        if (eventExp > 0) {
            return eventExp;
        }

        if (snapshot.snapshotDroppedExp() > 0) {
            return snapshot.snapshotDroppedExp();
        }

        return Math.max(snapshot.totalExperience(), 0);
    }

    private List<ItemStack> copyItems(ItemStack[] items) {
        List<ItemStack> copiedItems = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            copiedItems.add(item.clone());
        }

        return copiedItems;
    }

    private List<ItemStack> copyItems(List<ItemStack> items) {
        List<ItemStack> copiedItems = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            copiedItems.add(item.clone());
        }

        return copiedItems;
    }

    private void logDeathDebug(String phase, Player player, PlayerDeathEvent event, String details) {
        if (!plugin.getConfigManager().isDebugDeathEvents()) {
            return;
        }

        String damageCause = "NONE";
        if (player.getLastDamageCause() != null && player.getLastDamageCause().getCause() != null) {
            damageCause = player.getLastDamageCause().getCause().name();
        }

        plugin.getLogger().info("[DeathDebug] phase=" + phase
                + ", player=" + player.getName()
                + ", uuid=" + player.getUniqueId()
                + ", world=" + player.getWorld().getName()
                + ", keepInventory=" + event.getKeepInventory()
                + ", keepLevel=" + event.getKeepLevel()
                + ", eventDrops=" + event.getDrops().size()
                + ", eventDroppedExp=" + event.getDroppedExp()
                + ", damageCause=" + damageCause
                + ", details={" + details + "}");
    }

    private String resolveKillerName(Player player) {
        Player directPlayerKiller = player.getKiller();
        if (directPlayerKiller != null) {
            return directPlayerKiller.getName();
        }

        EntityDamageEvent lastDamageCause = player.getLastDamageCause();
        if (!(lastDamageCause instanceof EntityDamageByEntityEvent damageByEntityEvent)) {
            return "Environment";
        }

        Entity damager = damageByEntityEvent.getDamager();
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity shooterEntity) {
                return getEntityDisplayName(shooterEntity);
            }

            return "Projectile";
        }

        return getEntityDisplayName(damager);
    }

    private boolean wasKilledByPlayer(Player player) {
        if (player.getKiller() != null) {
            return true;
        }

        EntityDamageEvent lastDamageCause = player.getLastDamageCause();
        if (!(lastDamageCause instanceof EntityDamageByEntityEvent damageByEntityEvent)) {
            return false;
        }

        Entity damager = damageByEntityEvent.getDamager();
        if (damager instanceof Player) {
            return true;
        }

        if (damager instanceof Projectile projectile) {
            return projectile.getShooter() instanceof Player;
        }

        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerPvPCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttackingPlayer(event);
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        plugin.getGraveManager().recordPvPCombat(victim, attacker);
    }

    private Player resolveAttackingPlayer(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }

        if (damager instanceof Tameable tameable && tameable.getOwner() instanceof Player player) {
            return player;
        }

        return null;
    }

    private String getEntityDisplayName(Entity entity) {
        if (entity instanceof Player killerPlayer) {
            return killerPlayer.getName();
        }

        if (entity instanceof Tameable tameable && tameable.getOwner() instanceof Player owner) {
            return owner.getName() + "'s " + formatEntityTypeName(entity);
        }

        if (entity instanceof LivingEntity livingEntity && livingEntity.customName() != null) {
            String customName = PlainTextComponentSerializer.plainText().serialize(livingEntity.customName());
            if (!customName.isBlank()) {
                return customName;
            }
        }

        return formatEntityTypeName(entity);
    }

    private String formatEntityTypeName(Entity entity) {
        String[] words = entity.getType().name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        return builder.isEmpty() ? "Environment" : builder.toString();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        deathSnapshots.remove(event.getPlayer().getUniqueId());
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            int locatorsGiven = plugin.getGraveManager().giveLocatorsForPlayer(player);
            if (locatorsGiven > 0) {
                player.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgMapReceived()));
            }
        });
    }

    private record DeathSnapshot(long deathTick, List<ItemStack> items, int snapshotDroppedExp, int totalExperience,
            boolean processed) {
        private DeathSnapshot(long deathTick, List<ItemStack> items, int snapshotDroppedExp, int totalExperience) {
            this(deathTick, items, snapshotDroppedExp, totalExperience, false);
        }

        private DeathSnapshot markProcessed() {
            return new DeathSnapshot(deathTick, items, snapshotDroppedExp, totalExperience, true);
        }
    }

    @EventHandler
    public void onGraveInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (handleChestClaim(event)) {
                return;
            }
            handleLocatorUse(event);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            handleLocatorUse(event);
        }
    }

    private boolean handleChestClaim(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || !isGraveMarker(clicked)) {
            return false;
        }

        Optional<Grave> graveOptional = plugin.getGraveManager().getGraveByMarkerBlock(clicked);
        if (graveOptional.isEmpty()) {
            return false;
        }

        boolean cancelledByAnotherPlugin = event.useInteractedBlock() == Event.Result.DENY;
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        Player player = event.getPlayer();
        Grave grave = graveOptional.get();

        if (!plugin.getGraveManager().canAccessGrave(player, grave)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyOwnerCanClaim()));
            return true;
        }

        if (cancelledByAnotherPlugin) {
            return true;
        }

        if (grave.getMarkerType() == GraveMarkerType.CHEST) {
            plugin.getGraveManager().openGraveChest(player, grave);
            return true;
        }

        if (plugin.getGraveManager().claimGrave(player, grave)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveClaimed()));
        }

        return true;
    }

    @EventHandler
    public void onGraveBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isGraveMarker(block)) {
            return;
        }

        Optional<Grave> graveOptional = plugin.getGraveManager().getGraveByMarkerBlock(block);
        if (graveOptional.isEmpty()) {
            return;
        }

        boolean cancelledByAnotherPlugin = event.isCancelled();
        event.setCancelled(true);

        Player player = event.getPlayer();
        Grave grave = graveOptional.get();

        if (!plugin.getGraveManager().canAccessGrave(player, grave)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyOwnerCanClaim()));
            return;
        }

        if (cancelledByAnotherPlugin) {
            return;
        }

        if (grave.getMarkerType() == GraveMarkerType.CHEST) {
            plugin.getGraveManager().openGraveChest(player, grave);
            return;
        }

        if (plugin.getGraveManager().claimGrave(player, grave)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveClaimed()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedGraveBlock);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedGraveBlock);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBurn(BlockBurnEvent event) {
        if (isProtectedGraveBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (isProtectedGraveBlock(event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isProtectedGraveBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isProtectedGraveBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isPhysicalGraveContainer(event.getSource()) || isPhysicalGraveContainer(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInfoMenuClick(InventoryClickEvent event) {
        if (!isInfoMenu(event)) {
            handleGraveChestClick(event);
            return;
        }

        event.setCancelled(true);
        Inventory topInventory = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= topInventory.getSize()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InfoMenuHolder holder = (InfoMenuHolder) topInventory.getHolder(false);
        holder.getAction(rawSlot)
                .ifPresent(action -> plugin.getInfoMenuManager().handleClick(player, holder, action, event.getClick()));
    }

    @EventHandler
    public void onInfoMenuDrag(InventoryDragEvent event) {
        if (!isInfoMenu(event)) {
            handleGraveChestDrag(event);
            return;
        }

        event.setCancelled(true);
    }


    private boolean isInfoMenu(InventoryClickEvent event) {
        return event.getView().getTopInventory().getHolder(false) instanceof InfoMenuHolder;
    }

    private boolean isInfoMenu(InventoryDragEvent event) {
        return event.getView().getTopInventory().getHolder(false) instanceof InfoMenuHolder;
    }

    private void handleGraveChestClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof GraveChestHolder holder)) {
            return;
        }

        boolean clickedTopInventory = event.getRawSlot() >= 0 && event.getRawSlot() < topInventory.getSize();
        if (!clickedTopInventory) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
            }
            return;
        }

        if (!isGraveChestExtractionAction(event.getAction())) {
            event.setCancelled(true);
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getGraveManager().syncGraveChestInventory(player, holder.getGraveId(), topInventory));
    }

    private void handleGraveChestDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder(false) instanceof GraveChestHolder)) {
            return;
        }

        boolean affectsGraveChest = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= 0 && slot < topInventory.getSize());
        if (affectsGraveChest) {
            event.setCancelled(true);
        }
    }

    private boolean isGraveChestExtractionAction(InventoryAction action) {
        return action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.PICKUP_SOME
                || action == InventoryAction.PICKUP_HALF
                || action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.DROP_ALL_SLOT
                || action == InventoryAction.DROP_ONE_SLOT
                || action == InventoryAction.NOTHING;
    }

    private boolean isPhysicalGraveContainer(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder(false);
        if (!(holder instanceof Container container)) {
            return false;
        }

        return plugin.getGraveManager().getGraveByMarkerBlock(container.getBlock()).isPresent();
    }

    private void handleLocatorUse(PlayerInteractEvent event) {
        ItemStack usedItem = event.getItem();
        Optional<UUID> graveIdOptional = plugin.getGraveManager().getLocatorTarget(usedItem);
        if (graveIdOptional.isEmpty()) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        Player player = event.getPlayer();
        if (!plugin.getConfigManager().isLocatorMapEnabled()) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgLocatorDisabled()));
            return;
        }

        if (!player.hasPermission(TELEPORT_PERMISSION)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission()));
            return;
        }

        Optional<Grave> graveOptional = plugin.getGraveManager().getGraveById(graveIdOptional.get());
        if (graveOptional.isEmpty()) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveNotFound()));
            return;
        }

        Grave grave = graveOptional.get();
        if (!grave.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyOwnerCanUseMap()));
            return;
        }

        if (sendTeleportCooldownMessage(player)) {
            return;
        }

        if (!plugin.getGraveManager().teleportOwnerToGrave(player, grave)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveNotFound()));
        }
    }

    private boolean sendTeleportCooldownMessage(Player player) {
        if (!plugin.getGraveManager().isTeleportCooldownActive(player)) {
            return false;
        }

        String message = plugin.getConfigManager().getMsgTeleportCooldown()
                .replace("{seconds}", String.valueOf(plugin.getGraveManager().getTeleportCooldownRemainingSeconds(player)));
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
        return true;
    }

    private boolean isGraveMarker(Block block) {
        if (plugin.getGraveManager().getGraveByMarkerBlock(block).isPresent()) {
            return true;
        }

        Material marker = plugin.getGraveManager().getGraveMarkerMaterial();
        if (block.getType() == marker) {
            return true;
        }

        return marker == Material.PLAYER_HEAD && block.getType() == Material.PLAYER_WALL_HEAD;
    }

    private boolean isProtectedGraveBlock(Block block) {
        if (!isGraveMarker(block)) {
            return false;
        }

        return plugin.getGraveManager().getGraveByMarkerBlock(block).isPresent();
    }

}
