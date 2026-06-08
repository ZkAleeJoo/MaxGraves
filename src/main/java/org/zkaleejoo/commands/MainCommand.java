package org.zkaleejoo.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxGraves;
import org.zkaleejoo.grave.Grave;
import org.zkaleejoo.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                sender.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission()));
                return true;
            }

            plugin.reloadPluginState();
            sender.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgPluginReload()));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            if (!sender.hasPermission(INFO_PERMISSION)) {
                sender.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgNoPermission()));
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgOnlyPlayersCommand()));
                return true;
            }

            List<Grave> graves = plugin.getGraveManager().getGravesByPlayer(player.getUniqueId());
            if (graves.isEmpty()) {
                player.sendMessage(MessageUtils.getColoredMessage(
                        plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgInfoNoGrave()));
                return true;
            }

            plugin.getInfoMenuManager().openMainMenu(player, 0);
            return true;
        }

        sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgUsageCommand()));
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

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
}
