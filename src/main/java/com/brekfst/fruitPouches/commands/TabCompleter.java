package com.brekfst.fruitPouches.commands;

import com.brekfst.fruitPouches.FruitPouches;
import com.fruitster.fruitpouches.models.PouchSkin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tab completer for the plugin commands
 */
public class TabCompleter implements org.bukkit.command.TabCompleter {

    private final FruitPouches plugin;

    /**
     * Create a new tab completer
     *
     * @param plugin The plugin instance
     */
    public TabCompleter(FruitPouches plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Command completions
            List<String> commands = new ArrayList<>();

            if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.saveitem")) {
                commands.add("saveitem");
            }

            if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.give")) {
                commands.add("give");
            }

            if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.reload")) {
                commands.add("reload");
            }

            if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.stats")) {
                commands.add("stats");
            }

            if (sender.hasPermission("fruitpouch.skin")) {
                commands.add("skin");
            }

            if (sender.hasPermission("fruitpouch.upgrade")) {
                commands.add("upgrade");
            }

            if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.enchant")) {
                commands.add("enchant");
            }

            commands.add("help");

            return filterCompletions(commands, args[0]);
        } else if (args.length == 2) {
            // Sub-command completions
            switch (args[0].toLowerCase()) {
                case "saveitem":
                    // No completions for saveitem
                    break;
                case "give":
                    // Player names
                    return filterCompletions(getOnlinePlayerNames(), args[1]);
                case "stats":
                    // Pouch IDs
                    if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.stats")) {
                        return filterCompletions(new ArrayList<>(plugin.getPouchManager().getAllPouchIds()), args[1]);
                    }
                    break;
                case "skin":
                    // Skin sub-commands
                    if (sender.hasPermission("fruitpouch.skin")) {
                        List<String> skinCommands = new ArrayList<>();
                        skinCommands.add("shop");
                        skinCommands.add("apply");
                        return filterCompletions(skinCommands, args[1]);
                    }
                    break;
                case "upgrade":
                    // Pouch IDs
                    if (sender.hasPermission("fruitpouch.upgrade")) {
                        return filterCompletions(new ArrayList<>(plugin.getPouchManager().getAllPouchIds()), args[1]);
                    }
                    break;
                case "enchant":
                    // Pouch IDs
                    if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.enchant")) {
                        return filterCompletions(new ArrayList<>(plugin.getPouchManager().getAllPouchIds()), args[1]);
                    }
                    break;
            }
        } else if (args.length == 3) {
            // Third argument completions
            switch (args[0].toLowerCase()) {
                case "give":
                    // Pouch IDs
                    if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.give")) {
                        return filterCompletions(new ArrayList<>(plugin.getPouchManager().getAllPouchIds()), args[2]);
                    }
                    break;
                case "skin":
                    // Pouch IDs for skin shop or apply
                    if (sender.hasPermission("fruitpouch.skin")) {
                        if (args[1].equalsIgnoreCase("shop") || args[1].equalsIgnoreCase("apply")) {
                            return filterCompletions(new ArrayList<>(plugin.getPouchManager().getAllPouchIds()), args[2]);
                        }
                    }
                    break;
                case "enchant":
                    // Enchantment IDs
                    if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.enchant")) {
                        return filterCompletions(new ArrayList<>(plugin.getEnchantmentManager().getAllEnchantmentTypeIds()), args[2]);
                    }
                    break;
            }
        } else if (args.length == 4) {
            // Fourth argument completions
            switch (args[0].toLowerCase()) {
                case "give":
                    // Amount suggestions
                    if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.give")) {
                        List<String> amounts = new ArrayList<>();
                        amounts.add("1");
                        amounts.add("5");
                        amounts.add("10");
                        amounts.add("64");
                        return filterCompletions(amounts, args[3]);
                    }
                    break;
                case "skin":
                    // Skin IDs for apply
                    if (sender.hasPermission("fruitpouch.skin") && args[1].equalsIgnoreCase("apply")) {
                        String pouchId = args[2];

                        if (plugin.getPouchManager().pouchExists(pouchId)) {
                            Map<String, PouchSkin> skins = plugin.getSkinManager().getPouchSkins(pouchId);
                            return filterCompletions(new ArrayList<>(skins.keySet()), args[3]);
                        }
                    }
                    break;
                case "enchant":
                    // Enchantment levels
                    if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.enchant")) {
                        String enchantmentId = args[2];

                        if (plugin.getEnchantmentManager().enchantmentTypeExists(enchantmentId)) {
                            int maxLevel = plugin.getEnchantmentManager().getEnchantmentType(enchantmentId).getMaxLevel();
                            List<String> levels = new ArrayList<>();

                            for (int i = 1; i <= maxLevel; i++) {
                                levels.add(String.valueOf(i));
                            }

                            return filterCompletions(levels, args[3]);
                        }
                    }
                    break;
            }
        }

        return completions;
    }

    /**
     * Filter completions by a prefix
     *
     * @param completions The completions to filter
     * @param prefix The prefix to filter by
     * @return The filtered completions
     */
    private List<String> filterCompletions(List<String> completions, String prefix) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Get the names of all online players
     *
     * @return The names of all online players
     */
    private List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        return names;
    }
}