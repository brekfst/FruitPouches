package com.brekfst.fruitPouches.commands;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.gui.SkinShopGUI;
import com.brekfst.fruitPouches.gui.UpgradeGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Main command executor for the plugin
 */
public class FruitPouchCommand implements CommandExecutor {

    private final FruitPouches plugin;

    /**
     * Create a new command executor
     *
     * @param plugin The plugin instance
     */
    public FruitPouchCommand(FruitPouches plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "saveitem":
                return handleSaveItem(sender, args);
            case "give":
                return handleGive(sender, args);
            case "reload":
                return handleReload(sender);
            case "stats":
                return handleStats(sender, args);
            case "skin":
                return handleSkin(sender, args);
            case "upgrade":
                return handleUpgrade(sender, args);
            case "enchant":
                return handleEnchant(sender, args);
            case "help":
                showHelp(sender);
                return true;
            default:
                plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
                return true;
        }
    }

    /**
     * Handle the saveitem command
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was successful
     */
    private boolean handleSaveItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtils().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("fruitpouch.admin") && !sender.hasPermission("fruitpouch.saveitem")) {
            plugin.getMessageUtils().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
            return true;
        }

        Player player = (Player) sender;
        String id = args[1];
        boolean force = args.length > 2 && args[2].equals("-f");

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            plugin.getMessageUtils().sendMessage(sender, "items.invalid-item");
            return true;
        }

        if (plugin.getCustomItemManager().customItemExists(id) && !force) {
            plugin.getMessageUtils().sendMessage(sender, "items.already-exists", "id", id);
            return true;
        }

        if (plugin.getCustomItemManager().saveCustomItem(id, item, force)) {
            plugin.getMessageUtils().sendMessage(sender, "items.saved", "id", id);
            return true;
        } else {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
            return true;
        }
    }

    /**
     * Handle the give command
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was successful
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fruitpouch.admin") && !sender.hasPermission("fruitpouch.give")) {
            plugin.getMessageUtils().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
            return true;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-player", "player", playerName);
            return true;
        }

        String pouchId = args[2];

        if (!plugin.getPouchManager().pouchExists(pouchId)) {
            plugin.getMessageUtils().sendMessage(sender, "pouches.invalid-pouch", "pouch", pouchId);
            return true;
        }

        int amount = 1;

        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);

                if (amount < 1) {
                    plugin.getMessageUtils().sendMessage(sender, "general.invalid-amount", "amount", args[3]);
                    return true;
                }
            } catch (NumberFormatException e) {
                plugin.getMessageUtils().sendMessage(sender, "general.invalid-amount", "amount", args[3]);
                return true;
            }
        }

        if (plugin.getPouchManager().givePouch(target, pouchId, amount)) {
            plugin.getMessageUtils().sendMessage(sender, "pouches.give", "player", target.getName(), "pouch", plugin.getPouchManager().getPouch(pouchId).getDisplayName(), "amount", String.valueOf(amount));
            return true;
        } else {
            return true;
        }
    }


    /**
     * Handle the reload command
     *
     * @param sender The command sender
     * @return true if the command was successful
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("fruitpouch.admin") && !sender.hasPermission("fruitpouch.reload")) {
            plugin.getMessageUtils().sendMessage(sender, "general.no-permission");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Reloading FruitPouches configuration...");

        // Save any pending data before reload
        plugin.getPlayerDataManager().saveAllPlayerData();
        plugin.getStatsManager().saveAllStats();

        // Reload all configuration files
        plugin.getConfigManager().reloadAllConfigs();

        // Clear and reload all managers that depend on configuration
        plugin.getCustomItemManager().loadCustomItems();
        plugin.getPouchManager().loadPouches();
        plugin.getEnchantmentManager().loadEnchantmentTypes();
        plugin.getSkinManager().loadSkins();
        plugin.getMessageUtils().loadMessages();
        plugin.getPriceManager().loadPrices(); // Reload prices

        // Refresh all player pouches with the updated configuration
        plugin.getPlayerDataManager().refreshAllPlayerPouches();

        // Refresh any active GUIs
        plugin.getGuiManager().closeAllGUIs();

        // Final success message
        plugin.getMessageUtils().sendMessage(sender, "general.reload");
        plugin.getDebug().log("Plugin configuration reloaded by " + sender.getName());

        return true;
    }

    /**
     * Handle the stats command
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was successful
     */
    private boolean handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fruitpouch.admin") && !sender.hasPermission("fruitpouch.stats")) {
            plugin.getMessageUtils().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
            return true;
        }

        String pouchId = args[1];

        if (!plugin.getPouchManager().pouchExists(pouchId)) {
            plugin.getMessageUtils().sendMessage(sender, "pouches.invalid-pouch", "pouch", pouchId);
            return true;
        }

        // Display global stats
        sender.sendMessage(plugin.getMessageUtils().getMessage("stats.header", "pouch", plugin.getPouchManager().getPouch(pouchId).getDisplayName()));
        sender.sendMessage(plugin.getMessageUtils().getMessage("stats.items-collected", "amount", String.valueOf(plugin.getStatsManager().getGlobalStat(pouchId, "items_collected"))));
        sender.sendMessage(plugin.getMessageUtils().getMessage("stats.actions-performed", "amount", String.valueOf(plugin.getStatsManager().getGlobalStat(pouchId, "actions_performed"))));

        return true;
    }

    /**
     * Handle the skin command
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was successful
     */
    private boolean handleSkin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtils().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("fruitpouch.skin")) {
            plugin.getMessageUtils().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
            return true;
        }

        Player player = (Player) sender;
        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "shop":
                if (args.length < 3) {
                    plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
                    return true;
                }

                String pouchId = args[2];

                if (!plugin.getPouchManager().pouchExists(pouchId)) {
                    plugin.getMessageUtils().sendMessage(sender, "pouches.invalid-pouch", "pouch", pouchId);
                    return true;
                }

                // Open skin shop GUI
                SkinShopGUI skinShopGUI = new SkinShopGUI(plugin, player, pouchId);
                skinShopGUI.open();

                return true;
            case "apply":
                if (args.length < 4) {
                    plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
                    return true;
                }

                String applyPouchId = args[2];
                String skinId = args[3];

                if (!plugin.getPouchManager().pouchExists(applyPouchId)) {
                    plugin.getMessageUtils().sendMessage(sender, "pouches.invalid-pouch", "pouch", applyPouchId);
                    return true;
                }

                if (!plugin.getSkinManager().skinExists(applyPouchId, skinId)) {
                    plugin.getMessageUtils().sendMessage(sender, "skins.invalid-skin", "skin", skinId);
                    return true;
                }

                plugin.getSkinManager().applySkin(player, applyPouchId, skinId);

                return true;
            default:
                plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
                return true;
        }
    }

    /**
     * Handle the upgrade command
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was successful
     */
    private boolean handleUpgrade(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtils().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("fruitpouch.upgrade")) {
            plugin.getMessageUtils().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
            return true;
        }

        Player player = (Player) sender;
        String pouchId = args[1];

        if (!plugin.getPouchManager().pouchExists(pouchId)) {
            plugin.getMessageUtils().sendMessage(sender, "pouches.invalid-pouch", "pouch", pouchId);
            return true;
        }

        // Check if the player has permission to upgrade this pouch
        if (!plugin.getPouchManager().getPouch(pouchId).hasUpgradePermission(player)) {
            plugin.getMessageUtils().sendMessage(sender, "general.no-permission");
            return true;
        }

        // Open upgrade GUI
        UpgradeGUI upgradeGUI = new UpgradeGUI(plugin, player, pouchId);
        upgradeGUI.open();

        return true;
    }

    /**
     * Handle the enchant command
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was successful
     */
    private boolean handleEnchant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtils().sendMessage(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission("fruitpouch.admin") && !sender.hasPermission("fruitpouch.enchant")) {
            plugin.getMessageUtils().sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length < 4) {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-command");
            return true;
        }

        Player player = (Player) sender;
        String pouchId = args[1];
        String enchantmentId = args[2];
        int level;

        try {
            level = Integer.parseInt(args[3]);

            if (level < 1) {
                plugin.getMessageUtils().sendMessage(sender, "general.invalid-amount", "amount", args[3]);
                return true;
            }
        } catch (NumberFormatException e) {
            plugin.getMessageUtils().sendMessage(sender, "general.invalid-amount", "amount", args[3]);
            return true;
        }

        if (!plugin.getPouchManager().pouchExists(pouchId)) {
            plugin.getMessageUtils().sendMessage(sender, "pouches.invalid-pouch", "pouch", pouchId);
            return true;
        }

        if (!plugin.getEnchantmentManager().enchantmentTypeExists(enchantmentId)) {
            plugin.getMessageUtils().sendMessage(sender, "enchantments.invalid-enchantment", "enchantment", enchantmentId);
            return true;
        }

        plugin.getEnchantmentManager().applyEnchantment(player, pouchId, enchantmentId, level);

        return true;
    }

    /**
     * Show the help message
     *
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6==== FruitPouches Help ====");

        if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.saveitem")) {
            sender.sendMessage("§e/fruitpouch saveitem <id> [-f] §7- Save a custom item");
        }

        if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.give")) {
            sender.sendMessage("§e/fruitpouch give <player> <pouch> [amount] §7- Give a pouch to a player");
        }

        if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.reload")) {
            sender.sendMessage("§e/fruitpouch reload §7- Reload the plugin configuration");
        }

        if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.stats")) {
            sender.sendMessage("§e/fruitpouch stats <pouch> §7- View pouch statistics");
        }

        if (sender.hasPermission("fruitpouch.skin")) {
            sender.sendMessage("§e/fruitpouch skin shop <pouch> §7- Open the skin shop");
            sender.sendMessage("§e/fruitpouch skin apply <pouch> <skin> §7- Apply a skin to a pouch");
        }

        if (sender.hasPermission("fruitpouch.upgrade")) {
            sender.sendMessage("§e/fruitpouch upgrade <pouch> §7- Upgrade a pouch");
        }

        if (sender.hasPermission("fruitpouch.admin") || sender.hasPermission("fruitpouch.enchant")) {
            sender.sendMessage("§e/fruitpouch enchant <pouch> <enchantment> <level> §7- Apply an enchantment to a pouch");
        }

        sender.sendMessage("§e/fruitpouch help §7- Show this help message");
    }
}