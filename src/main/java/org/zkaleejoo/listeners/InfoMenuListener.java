package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.commands.InfoMenuClickPolicy;
import org.zkaleejoo.commands.InfoMenuHolder;

public class InfoMenuListener implements Listener {

    private final MaxGraves plugin;

    public InfoMenuListener(MaxGraves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof InfoMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || !InfoMenuClickPolicy.isTopInventoryClick(event.getRawSlot(), inventory.getSize())) {
            return;
        }

        holder.getAction(event.getRawSlot())
                .ifPresent(action -> plugin.getInfoMenuManager().handleClick(player, holder, action, event.getClick()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof InfoMenuHolder
                && InfoMenuClickPolicy.touchesTopInventory(event.getRawSlots(), inventory.getSize())) {
            event.setCancelled(true);
        }
    }
}
