package org.zkaleejoo.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.grave.Grave;
import org.zkaleejoo.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "maxgrave.admin";
    private static final String INFO_PERMISSION = "maxgrave.info";

    private final MaxGraves plugin;

    public MainCommand(MaxGraves plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission()));
                return true;
            }

            plugin.reloadConfig();
            plugin.getConfigManager().reloadConfig();
            plugin.getGraveManager().reloadSettings();
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgPluginReload()));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            if (!sender.hasPermission(INFO_PERMISSION)) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission()));
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayersCommand()));
                return true;
            }

            List<Grave> graves = plugin.getGraveManager().getGravesByPlayer(player.getUniqueId());
            if (graves.isEmpty()) {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInfoNoGrave()));
                return true;
            }

            openInfoMenu(player, graves);
            return true;
        }

        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgUsageCommand()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission(INFO_PERMISSION)) {
                completions.add("info");
            }
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                completions.addAll(Arrays.asList("reload"));
            }
            return filterCompletions(completions, args[0]);
        }

        return completions;
    }

    private void openInfoMenu(Player player, List<Grave> graves) {
        int size = Math.min(54, ((graves.size() - 1) / 9 + 1) * 9);
        String menuTitle = MessageUtils.getColoredMessage(plugin.getConfigManager().getMsgInfoMenuTitle());
        Inventory inventory = Bukkit.createInventory(null, size, menuTitle);

        for (int i = 0; i < size && i < graves.size(); i++) {
            Grave grave = graves.get(i);
            inventory.setItem(i, buildGraveItem(grave, i + 1));
        }

        player.openInventory(inventory);
    }

    private ItemStack buildGraveItem(Grave grave, int index) {
        Material itemMaterial = Material.matchMaterial(plugin.getConfigManager().getInfoMenuItemMaterial());
        if (itemMaterial == null || !itemMaterial.isItem()) {
            itemMaterial = Material.PAPER;
        }

        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Location location = grave.getLocation();
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";

        meta.setDisplayName(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getMsgInfoMenuItemName().replace("{index}", String.valueOf(index))));

        List<String> lore = plugin.getConfigManager().getMsgInfoMenuLore().stream()
                .map(line -> replaceInfoMenuPlaceholders(line, grave, worldName, index))
                .map(MessageUtils::getColoredMessage)
                .toList();
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String replaceInfoMenuPlaceholders(String line, Grave grave, String worldName, int index) {
        Location location = grave.getLocation();

        return line
                .replace("{index}", String.valueOf(index))
                .replace("{world}", worldName)
                .replace("{x}", String.valueOf(location.getBlockX()))
                .replace("{y}", String.valueOf(location.getBlockY()))
                .replace("{z}", String.valueOf(location.getBlockZ()))
                .replace("{time_left}", formatTimeLeft(grave.getDespawnAtMillis()))
                .replace("{id}", grave.getId().toString());
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }

    private String formatTimeLeft(long despawnAtMillis) {
        long totalSeconds = Math.max((despawnAtMillis - System.currentTimeMillis()) / 1000L, 0L);

        long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;

        return hours + "h " + minutes + "m " + seconds + "s";
    }
}
