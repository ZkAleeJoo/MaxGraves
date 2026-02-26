package org.zkaleejoo.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.grave.Grave;
import org.zkaleejoo.utils.MessageUtils;

import java.util.Optional;
import java.util.UUID;

public class GraveListener implements Listener {

    private static final String TELEPORT_PERMISSION = "maxgrave.tp";

    private final MaxGraves plugin;

    public GraveListener(MaxGraves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isCreateOnDeath()) {
            return;
        }

        Player player = (Player) event.getEntity();
        String killerName = player.getKiller() != null ? player.getKiller().getName() : "Environment";

        plugin.getGraveManager().createGrave(player, player.getLocation(), event.getDrops(), event.getDroppedExp(), killerName).ifPresentOrElse(grave -> {
            event.getDrops().clear();
            event.setDroppedExp(0);

            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveCreated()));
        }, () -> player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveCreateFail())));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            int locatorsGiven = plugin.getGraveManager().giveLocatorsForPlayer(player);
            if (locatorsGiven > 0) {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgMapReceived()));
            }
        });
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

        boolean cancelledByAnotherPlugin = event.isCancelled();
        event.setCancelled(true);

        Player player = event.getPlayer();
        Grave grave = graveOptional.get();

        if (!grave.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyOwnerCanClaim()));
            return true;
        }

        if (cancelledByAnotherPlugin) {
            return true;
        }

        if (plugin.getGraveManager().claimGrave(player, grave)) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveClaimed()));
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

        if (!grave.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyOwnerCanClaim()));
            return;
        }

        if (cancelledByAnotherPlugin) {
            return;
        }

        if (plugin.getGraveManager().claimGrave(player, grave)) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveClaimed()));
        }
    }


    @EventHandler
    public void onInfoMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(MessageUtils.getColoredMessage(plugin.getConfigManager().getMsgInfoMenuTitle()))) {
            event.setCancelled(true);
        }
    }
    private void handleLocatorUse(PlayerInteractEvent event) {
        ItemStack usedItem = event.getItem();
        Optional<UUID> graveIdOptional = plugin.getGraveManager().getLocatorTarget(usedItem);
        if (graveIdOptional.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission(TELEPORT_PERMISSION)) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission()));
            return;
        }

        Optional<Grave> graveOptional = plugin.getGraveManager().getGraveById(graveIdOptional.get());
        if (graveOptional.isEmpty()) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgGraveNotFound()));
            return;
        }

        Grave grave = graveOptional.get();
        if (!grave.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyOwnerCanUseMap()));
            return;
        }

        Location destination = grave.getLocation().clone().add(0.5, 1, 0.5);
        player.teleport(destination);
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgLocatorUsed()));
    }

    private boolean isGraveMarker(Block block) {
        Material marker = plugin.getGraveManager().getGraveMarkerMaterial();
        if (block.getType() == marker) {
            return true;
        }

        return marker == Material.PLAYER_HEAD && block.getType() == Material.PLAYER_WALL_HEAD;
    }
}
