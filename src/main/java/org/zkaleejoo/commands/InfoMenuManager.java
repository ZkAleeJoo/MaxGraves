package org.zkaleejoo.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.grave.Grave;
import org.zkaleejoo.grave.GraveMarkerType;
import org.zkaleejoo.utils.MessageUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class InfoMenuManager {

    private static final String TELEPORT_PERMISSION = "maxgrave.tp";

    private final MaxGraves plugin;

    public InfoMenuManager(MaxGraves plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("null")
    public void openMainMenu(Player player, int requestedPage) {
        List<Grave> graves = plugin.getGraveManager().getGravesByPlayer(player.getUniqueId());
        if (graves.isEmpty()) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInfoNoGrave()));
            return;
        }

        MainConfigManager config = plugin.getConfigManager();
        InfoMenuLayout layout = new InfoMenuLayout(config.getInfoMenuSize(), config.getInfoMenuGraveSlots());
        int page = layout.clampPage(requestedPage, graves.size());
        int pages = layout.pageCount(graves.size());
        String title = replaceMenuPlaceholders(config.getMsgInfoMenuTitle(), page, pages, graves.size());
        InfoMenuHolder holder = new InfoMenuHolder(player.getUniqueId(), InfoMenuHolder.ViewType.MAIN, page, null);
        Inventory inventory = Bukkit.createInventory(holder, layout.size(), MessageUtils.getColoredComponent(title));

        applyFiller(inventory, layout);
        List<Integer> itemIndexes = layout.itemIndexesForPage(page, graves.size());
        for (int slotIndex = 0; slotIndex < itemIndexes.size(); slotIndex++) {
            int graveIndex = itemIndexes.get(slotIndex);
            int slot = layout.graveSlots().get(slotIndex);
            Grave grave = graves.get(graveIndex);

            inventory.setItem(slot, buildGraveItem(grave, graveIndex + 1, page, pages, graves.size()));
            holder.registerAction(slot, InfoMenuAction.GRAVE, grave.getId());
        }

        addMainButton(inventory, holder, "previous-page", InfoMenuAction.PREVIOUS_PAGE, page > 0, page, pages,
                graves.size(), null, 0);
        addMainButton(inventory, holder, "next-page", InfoMenuAction.NEXT_PAGE, page + 1 < pages, page, pages,
                graves.size(), null, 0);
        addMainButton(inventory, holder, "refresh", InfoMenuAction.REFRESH, true, page, pages, graves.size(), null, 0);
        addMainButton(inventory, holder, "close", InfoMenuAction.CLOSE, true, page, pages, graves.size(), null, 0);

        player.openInventory(inventory);
    }

    @SuppressWarnings("null")
    public void openDetailsMenu(Player player, UUID graveId, int page) {
        Optional<Grave> graveOptional = plugin.getGraveManager().getGraveById(graveId);
        if (graveOptional.isEmpty() || !graveOptional.get().getOwner().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInfoMenuExpired()));
            openMainMenu(player, page);
            return;
        }

        Grave grave = graveOptional.get();
        List<Grave> graves = plugin.getGraveManager().getGravesByPlayer(player.getUniqueId());
        int index = Math.max(graves.indexOf(grave), 0) + 1;
        MainConfigManager config = plugin.getConfigManager();
        InfoMenuLayout layout = new InfoMenuLayout(config.getInfoMenuSize(), config.getInfoMenuGraveSlots());
        int pages = layout.pageCount(graves.size());
        String title = replaceGravePlaceholders(config.getMsgInfoDetailsTitle(), grave, index, page, pages,
                graves.size());
        InfoMenuHolder holder = new InfoMenuHolder(player.getUniqueId(), InfoMenuHolder.ViewType.DETAILS, page,
                graveId);
        Inventory inventory = Bukkit.createInventory(holder, layout.size(), MessageUtils.getColoredComponent(title));

        applyFiller(inventory, layout);
        int summarySlot = config.getInfoMenuDetailsSummarySlot();
        if (summarySlot >= 0 && summarySlot < inventory.getSize()) {
            inventory.setItem(summarySlot, buildDetailsSummaryItem(grave, index, page, pages, graves.size()));
        }

        addDetailsButton(inventory, holder, "back", InfoMenuAction.BACK, true, grave, index, page, pages,
                graves.size());
        addDetailsButton(inventory, holder, "teleport", InfoMenuAction.TELEPORT,
                config.getInfoMenuButton("teleport").enabled(), grave, index, page, pages, graves.size());
        addDetailsButton(inventory, holder, "locator", InfoMenuAction.LOCATOR,
                config.isLocatorMapEnabled() && config.getInfoMenuButton("locator").enabled(), grave, index, page,
                pages, graves.size());
        addDetailsButton(inventory, holder, "refresh", InfoMenuAction.REFRESH, true, grave, index, page, pages,
                graves.size());
        addDetailsButton(inventory, holder, "close", InfoMenuAction.CLOSE, true, grave, index, page, pages,
                graves.size());

        player.openInventory(inventory);
    }

    public void handleClick(Player player, InfoMenuHolder holder, InfoMenuClickAction clickAction,
            ClickType clickType) {
        if (!holder.getViewerId().equals(player.getUniqueId())) {
            return;
        }

        playClickSound(player);

        switch (clickAction.action()) {
            case GRAVE -> handleGraveClick(player, holder, clickAction, clickType);
            case TELEPORT ->
                clickAction.graveIdOptional().ifPresent(graveId -> handleTeleport(player, holder, graveId));
            case LOCATOR -> clickAction.graveIdOptional().ifPresent(graveId -> handleLocator(player, holder, graveId));
            case DETAILS ->
                clickAction.graveIdOptional().ifPresent(graveId -> openDetailsMenu(player, graveId, holder.getPage()));
            case PREVIOUS_PAGE -> openMainMenu(player, holder.getPage() - 1);
            case NEXT_PAGE -> openMainMenu(player, holder.getPage() + 1);
            case REFRESH -> refresh(player, holder);
            case CLOSE -> player.closeInventory();
            case BACK -> openMainMenu(player, holder.getPage());
        }
    }

    private void handleGraveClick(Player player, InfoMenuHolder holder, InfoMenuClickAction clickAction,
            ClickType clickType) {
        Optional<UUID> graveIdOptional = clickAction.graveIdOptional();
        if (graveIdOptional.isEmpty()) {
            return;
        }

        if (clickType.isRightClick()) {
            if (plugin.getConfigManager().isLocatorMapEnabled()
                    && plugin.getConfigManager().getInfoMenuButton("locator").enabled()) {
                handleLocator(player, holder, graveIdOptional.get());
            }
            return;
        }

        if (clickType.isShiftClick()) {
            if (plugin.getConfigManager().getInfoMenuButton("details").enabled()) {
                openDetailsMenu(player, graveIdOptional.get(), holder.getPage());
            }
            return;
        }

        if (plugin.getConfigManager().getInfoMenuButton("teleport").enabled()) {
            handleTeleport(player, holder, graveIdOptional.get());
            return;
        }

        if (plugin.getConfigManager().getInfoMenuButton("details").enabled()) {
            openDetailsMenu(player, graveIdOptional.get(), holder.getPage());
        }
    }

    private void handleTeleport(Player player, InfoMenuHolder holder, UUID graveId) {
        Optional<Grave> graveOptional = getOwnedActiveGrave(player, holder, graveId);
        if (graveOptional.isEmpty()) {
            return;
        }

        if (!player.hasPermission(TELEPORT_PERMISSION)) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission()));
            return;
        }

        if (sendTeleportCooldownMessage(player)) {
            return;
        }

        boolean teleported = plugin.getGraveManager().teleportOwnerToGrave(player, graveOptional.get());
        if (!teleported) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInfoTeleportUnavailable()));
            return;
        }

        player.closeInventory();
    }

    private void handleLocator(Player player, InfoMenuHolder holder, UUID graveId) {
        if (!plugin.getConfigManager().isLocatorMapEnabled()) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgLocatorDisabled()));
            return;
        }

        Optional<Grave> graveOptional = getOwnedActiveGrave(player, holder, graveId);
        if (graveOptional.isEmpty()) {
            return;
        }

        if (plugin.getGraveManager().giveLocatorForGrave(player, graveOptional.get())) {
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInfoLocatorReceived()));
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

    private Optional<Grave> getOwnedActiveGrave(Player player, InfoMenuHolder holder, UUID graveId) {
        Optional<Grave> graveOptional = plugin.getGraveManager().getGraveById(graveId)
                .filter(grave -> grave.getOwner().equals(player.getUniqueId()));
        if (graveOptional.isPresent()) {
            return graveOptional;
        }

        player.sendMessage(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInfoMenuExpired()));
        refresh(player, holder);
        return Optional.empty();
    }

    private void refresh(Player player, InfoMenuHolder holder) {
        if (holder.getViewType() == InfoMenuHolder.ViewType.DETAILS && holder.getGraveId() != null) {
            openDetailsMenu(player, holder.getGraveId(), holder.getPage());
            return;
        }

        openMainMenu(player, holder.getPage());
    }

    private void addMainButton(Inventory inventory, InfoMenuHolder holder, String key, InfoMenuAction action,
            boolean shouldRender, int page, int pages, int graveCount, Grave grave, int index) {
        InfoMenuButton button = plugin.getConfigManager().getInfoMenuButton(key);
        if (!shouldRender || !button.enabled() || !isValidSlot(button.slot(), inventory)) {
            return;
        }

        inventory.setItem(button.slot(), buildConfiguredButton(button, grave, index, page, pages, graveCount));
        holder.registerAction(button.slot(), action, grave == null ? null : grave.getId());
    }

    private void addDetailsButton(Inventory inventory, InfoMenuHolder holder, String key, InfoMenuAction action,
            boolean shouldRender, Grave grave, int index, int page, int pages, int graveCount) {
        addMainButton(inventory, holder, key, action, shouldRender, page, pages, graveCount, grave, index);
    }

    private ItemStack buildGraveItem(Grave grave, int index, int page, int pages, int graveCount) {
        MainConfigManager config = plugin.getConfigManager();
        Material material = resolveGraveItemMaterial(grave);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(MessageUtils.getColoredItemComponent(
                replaceGravePlaceholders(config.getMsgInfoMenuItemName(), grave, index, page, pages, graveCount)));
        meta.lore(config.getMsgInfoMenuLore().stream()
                .map(line -> replaceGravePlaceholders(line, grave, index, page, pages, graveCount))
                .map(MessageUtils::getColoredItemComponent)
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildDetailsSummaryItem(Grave grave, int index, int page, int pages, int graveCount) {
        MainConfigManager config = plugin.getConfigManager();
        ItemStack item = new ItemStack(resolveMaterial(config.getInfoMenuItemMaterial(), Material.PAPER));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(MessageUtils.getColoredItemComponent(
                replaceGravePlaceholders(config.getMsgInfoDetailsSummaryName(), grave, index, page, pages,
                        graveCount)));
        meta.lore(config.getMsgInfoDetailsSummaryLore().stream()
                .map(line -> replaceGravePlaceholders(line, grave, index, page, pages, graveCount))
                .map(MessageUtils::getColoredItemComponent)
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfiguredButton(InfoMenuButton button, Grave grave, int index, int page, int pages,
            int graveCount) {
        ItemStack item = new ItemStack(button.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(MessageUtils.getColoredItemComponent(
                replaceButtonPlaceholders(button.name(), grave, index, page, pages, graveCount)));
        meta.lore(button.lore().stream()
                .map(line -> replaceButtonPlaceholders(line, grave, index, page, pages, graveCount))
                .map(MessageUtils::getColoredItemComponent)
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    private void applyFiller(Inventory inventory, InfoMenuLayout layout) {
        MainConfigManager config = plugin.getConfigManager();
        InfoMenuButton filler = config.getInfoMenuFillerItem();
        if (!filler.enabled() || filler.material() == Material.AIR) {
            return;
        }

        ItemStack fillerItem = buildConfiguredButton(filler, null, 0, 0, 1, 0);
        for (int slot : InfoMenuDecorationPolicy.fillerSlots(inventory.getSize(), layout.graveSlots(),
                config.isInfoMenuFillerFillGraveSlots())) {
            inventory.setItem(slot, fillerItem);
        }
    }

    private Material resolveGraveItemMaterial(Grave grave) {
        MainConfigManager config = plugin.getConfigManager();
        long secondsLeft = Math.max((grave.getDespawnAtMillis() - System.currentTimeMillis()) / 1000L, 0L);
        if (config.getInfoMenuExpiringThresholdSeconds() > 0
                && secondsLeft <= config.getInfoMenuExpiringThresholdSeconds()) {
            return resolveMaterial(config.getInfoMenuExpiringItemMaterial(), Material.CLOCK);
        }

        if (grave.isPublicAccess()) {
            return resolveMaterial(config.getInfoMenuPublicItemMaterial(), Material.ENDER_EYE);
        }

        if (grave.getMarkerType() == GraveMarkerType.CHEST) {
            return resolveMaterial(config.getInfoMenuChestItemMaterial(), Material.CHEST);
        }

        if (grave.getMarkerType() == GraveMarkerType.HEAD) {
            return resolveMaterial(config.getInfoMenuHeadItemMaterial(), Material.PLAYER_HEAD);
        }

        return resolveMaterial(config.getInfoMenuItemMaterial(), Material.PAPER);
    }

    private String replaceButtonPlaceholders(String text, Grave grave, int index, int page, int pages, int graveCount) {
        String replaced = replaceMenuPlaceholders(text, page, pages, graveCount)
                .replace("{previous_page}", String.valueOf(Math.max(page, 1)))
                .replace("{next_page}", String.valueOf(Math.min(page + 2, pages)));
        if (grave == null) {
            return replaced;
        }

        return replaceGravePlaceholders(replaced, grave, index, page, pages, graveCount);
    }

    private String replaceMenuPlaceholders(String text, int page, int pages, int graveCount) {
        if (text == null) {
            return "";
        }

        return text
                .replace("{page}", String.valueOf(page + 1))
                .replace("{pages}", String.valueOf(Math.max(pages, 1)))
                .replace("{graves}", String.valueOf(graveCount));
    }

    private String replaceGravePlaceholders(String text, Grave grave, int index, int page, int pages, int graveCount) {
        String replaced = replaceMenuPlaceholders(text, page, pages, graveCount);
        String worldName = grave.getLocation().getWorld() != null ? grave.getLocation().getWorld().getName()
                : "unknown";
        String killer = grave.getKillerName() == null || grave.getKillerName().isBlank()
                ? plugin.getConfigManager().getMsgHologramUnknownKiller()
                : grave.getKillerName();
        String access = grave.isPublicAccess()
                ? plugin.getConfigManager().getMsgInfoAccessPublic()
                : plugin.getConfigManager().getMsgInfoAccessPrivate();

        return replaced
                .replace("{index}", String.valueOf(index))
                .replace("{world}", worldName)
                .replace("{x}", String.valueOf(grave.getLocation().getBlockX()))
                .replace("{y}", String.valueOf(grave.getLocation().getBlockY()))
                .replace("{z}", String.valueOf(grave.getLocation().getBlockZ()))
                .replace("{time_left}", formatTimeLeft(grave.getDespawnAtMillis()))
                .replace("{id}", grave.getId().toString())
                .replace("{items}", String.valueOf(countItems(grave)))
                .replace("{exp}", String.valueOf(Math.max(grave.getExp(), 0)))
                .replace("{killer}", killer)
                .replace("{type}", grave.getMarkerType().name())
                .replace("{access}", access);
    }

    private int countItems(Grave grave) {
        return (int) grave.getItems().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getType() != Material.AIR)
                .count();
    }

    private boolean isValidSlot(int slot, Inventory inventory) {
        return slot >= 0 && slot < inventory.getSize();
    }

    private Material resolveMaterial(String materialName, Material fallback) {
        Material material = Material.matchMaterial(materialName == null ? fallback.name() : materialName);
        return material != null && material.isItem() ? material : fallback;
    }

    @SuppressWarnings("null")
    private void playClickSound(Player player) {
        MainConfigManager config = plugin.getConfigManager();
        if (!config.isInfoMenuSoundsEnabled()) {
            return;
        }

        Sound sound = resolveSound(config.getInfoMenuClickSound(), Sound.UI_BUTTON_CLICK);
        player.playSound(player.getLocation(), sound, config.getInfoMenuClickSoundVolume(),
                config.getInfoMenuClickSoundPitch());
    }

    private Sound resolveSound(String configuredSound, Sound fallback) {
        if (configuredSound == null || configuredSound.isBlank()) {
            return fallback;
        }

        NamespacedKey key = NamespacedKey.fromString(configuredSound.trim().toLowerCase(Locale.ROOT));
        if (key != null) {
            Sound resolved = Registry.SOUNDS.get(key);
            if (resolved != null) {
                return resolved;
            }
        }

        String canonicalInput = configuredSound.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(".", "");
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

        return fallback;
    }

    private String formatTimeLeft(long despawnAtMillis) {
        long totalSeconds = Math.max((despawnAtMillis - System.currentTimeMillis()) / 1000L, 0L);

        long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;

        return hours + "h " + minutes + "m " + seconds + "s";
    }
}
