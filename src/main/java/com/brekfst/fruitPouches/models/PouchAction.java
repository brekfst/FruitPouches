package com.brekfst.fruitPouches.models;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an action that can be performed by a pouch
 */
public class PouchAction {

    private final String type;
    private final Map<String, Object> config;

    /**
     * Create a new pouch action
     *
     * @param type The action type
     * @param config The action configuration
     */
    public PouchAction(String type, Map<String, Object> config) {
        this.type = type;
        this.config = new HashMap<>(config);
    }

    /**
     * Execute this action
     *
     * @param plugin The plugin instance
     * @param player The player
     * @param pouch The pouch
     * @return true if the action was successful
     */
    public boolean execute(FruitPouches plugin, Player player, Pouch pouch) {
        switch (type) {
            case "merge":
                return executeMergeAction(plugin, player, pouch);
            case "transfer":
                return executeTransferAction(plugin, player, pouch);
            case "convert":
                return executeConvertAction(plugin, player, pouch);
            default:
                plugin.getDebug().log("Unknown action type: " + type);
                return false;
        }
    }

    /**
     * Execute a merge action
     *
     * @param plugin The plugin instance
     * @param player The player
     * @param pouch The pouch
     * @return true if the action was successful
     */
    private boolean executeMergeAction(FruitPouches plugin, Player player, Pouch pouch) {
        int threshold = (int) config.getOrDefault("threshold", 25);
        String inputId = (String) config.get("input");
        String outputId = (String) config.get("output");
        String message = (String) config.getOrDefault("message", "&aMerged items successfully!");

        if (inputId == null || outputId == null) {
            plugin.getDebug().log("Missing input or output in merge action for pouch: " + pouch.getId());
            return false;
        }

        // Find the CustomItem or Material
        CustomItem inputCustomItem = null;
        CustomItem outputCustomItem = null;
        ItemStack outputItem = null;

        if (inputId.startsWith("custom:")) {
            String customId = inputId.substring(7);
            inputCustomItem = plugin.getCustomItemManager().getCustomItem(customId);
            if (inputCustomItem == null) {
                plugin.getDebug().log("Unknown custom item: " + customId + " in merge action");
                return false;
            }
        }

        if (outputId.startsWith("custom:")) {
            String customId = outputId.substring(7);
            outputCustomItem = plugin.getCustomItemManager().getCustomItem(customId);
            if (outputCustomItem == null) {
                plugin.getDebug().log("Unknown custom item: " + customId + " in merge action");
                return false;
            }
        }

        // Count how many input items are in the pouch
        int count = 0;
        List<String> keysToRemove = new ArrayList<>();

        for (Map.Entry<String, ItemStack> entry : pouch.getContents().entrySet()) {
            ItemStack item = entry.getValue();
            boolean matches = false;

            if (inputCustomItem != null) {
                matches = inputCustomItem.matches(item);
            } else {
                // For vanilla items
                try {
                    matches = item.getType().name().equals(inputId);
                } catch (IllegalArgumentException e) {
                    plugin.getDebug().log("Invalid material name in merge action: " + inputId);
                    return false;
                }
            }

            if (matches) {
                count += item.getAmount();
                keysToRemove.add(entry.getKey());
            }
        }

        // Check if we have enough items
        if (count < threshold) {
            player.sendMessage(ChatColor.RED + "Not enough items to merge! Need " + threshold + ", have " + count);
            return false;
        }

        // Remove the input items
        Map<String, ItemStack> contents = pouch.getContents();
        for (String key : keysToRemove) {
            contents.remove(key);
        }
        pouch.setContents(contents);

        // Create the output item
        if (outputCustomItem != null) {
            outputItem = outputCustomItem.toItemStack();
        } else {
            // For vanilla items
            try {
                outputItem = new ItemStack(org.bukkit.Material.valueOf(outputId));
            } catch (IllegalArgumentException e) {
                plugin.getDebug().log("Invalid material name in merge action: " + outputId);
                return false;
            }
        }

        // Add the output item to the pouch
        pouch.addItem(outputItem);

        // Update stats
        pouch.getStats().incrementActionsPerformed();

        // Send message to the player
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        return true;
    }

    /**
     * Execute a transfer action
     *
     * @param plugin The plugin instance
     * @param player The player
     * @param pouch The pouch
     * @return true if the action was successful
     */
    private boolean executeTransferAction(FruitPouches plugin, Player player, Pouch pouch) {
        String targetId = (String) config.get("target");
        List<String> itemIds = (List<String>) config.getOrDefault("items", new ArrayList<>());
        String message = (String) config.getOrDefault("message", "&aTransferred items successfully!");

        if (targetId == null || itemIds.isEmpty()) {
            plugin.getDebug().log("Missing target or items in transfer action for pouch: " + pouch.getId());
            return false;
        }

        // Find the target pouch in the player's inventory
        ItemStack targetPouchItem = null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && Pouch.isPouchOfType(plugin, item, targetId)) {
                targetPouchItem = item;
                break;
            }
        }

        if (targetPouchItem == null) {
            player.sendMessage(ChatColor.RED + "Target pouch not found in your inventory!");
            return false;
        }

        // Get the target pouch
        Pouch targetPouch = plugin.getPouchManager().getPouch(targetId);
        if (targetPouch == null) {
            plugin.getDebug().log("Unknown target pouch: " + targetId + " in transfer action");
            return false;
        }

        // Load the target pouch's data
        targetPouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), targetId);

        // Collect items to transfer
        List<String> keysToRemove = new ArrayList<>();
        List<ItemStack> itemsToTransfer = new ArrayList<>();

        for (Map.Entry<String, ItemStack> entry : pouch.getContents().entrySet()) {
            ItemStack item = entry.getValue();
            boolean shouldTransfer = false;

            for (String itemId : itemIds) {
                if (itemId.startsWith("custom:")) {
                    String customId = itemId.substring(7);
                    CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customId);
                    if (customItem != null && customItem.matches(item)) {
                        shouldTransfer = true;
                        break;
                    }
                } else {
                    // For vanilla items
                    try {
                        if (item.getType().name().equals(itemId)) {
                            shouldTransfer = true;
                            break;
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getDebug().log("Invalid material name in transfer action: " + itemId);
                        continue;
                    }
                }
            }

            if (shouldTransfer) {
                keysToRemove.add(entry.getKey());
                itemsToTransfer.add(item.clone());
            }
        }

        if (itemsToTransfer.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No items to transfer!");
            return false;
        }

        // Remove the items from the source pouch
        Map<String, ItemStack> contents = pouch.getContents();
        for (String key : keysToRemove) {
            contents.remove(key);
        }
        pouch.setContents(contents);

        // Add the items to the target pouch
        int transferredCount = 0;
        for (ItemStack item : itemsToTransfer) {
            if (targetPouch.addItem(item)) {
                transferredCount += item.getAmount();
            } else {
                // Handle overflow
                if (targetPouch.getOverflowMode().equals("inventory")) {
                    player.getInventory().addItem(item);
                    player.sendMessage(ChatColor.YELLOW + "The target pouch is full! Items went to your inventory.");
                } else if (targetPouch.getOverflowMode().equals("drop")) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    player.sendMessage(ChatColor.YELLOW + "The target pouch is full! Items were dropped at your feet.");
                } else if (targetPouch.getOverflowMode().equals("sell") && plugin.getVaultHook().isEnabled()) {
                    double amount = plugin.getVaultHook().sellItem(player, item);
                    player.sendMessage(ChatColor.GREEN + "The target pouch is full! Items were sold for " +
                            plugin.getVaultHook().formatMoney(amount) + ".");
                }
            }
        }

        // Update stats
        if (transferredCount > 0) {
            pouch.getStats().incrementActionsPerformed();
            targetPouch.getStats().incrementItemsCollected(transferredCount);
            targetPouch.getStats().updateLastUsed();

            // Send message to the player
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

            // Save the target pouch
            plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), targetPouch);

            return true;
        }

        return false;
    }

    /**
     * Execute a convert action
     *
     * @param plugin The plugin instance
     * @param player The player
     * @param pouch The pouch
     * @return true if the action was successful
     */
    private boolean executeConvertAction(FruitPouches plugin, Player player, Pouch pouch) {
        int inputAmount = (int) config.getOrDefault("input_amount", 1);
        String inputId = (String) config.get("input");
        int outputAmount = (int) config.getOrDefault("output_amount", 1);
        String outputId = (String) config.get("output");
        String message = (String) config.getOrDefault("message", "&aConverted items successfully!");

        if (inputId == null || outputId == null) {
            plugin.getDebug().log("Missing input or output in convert action for pouch: " + pouch.getId());
            return false;
        }

        // Find the CustomItem or Material
        CustomItem inputCustomItem = null;
        CustomItem outputCustomItem = null;
        ItemStack outputItem = null;

        if (inputId.startsWith("custom:")) {
            String customId = inputId.substring(7);
            inputCustomItem = plugin.getCustomItemManager().getCustomItem(customId);
            if (inputCustomItem == null) {
                plugin.getDebug().log("Unknown custom item: " + customId + " in convert action");
                return false;
            }
        }

        if (outputId.startsWith("custom:")) {
            String customId = outputId.substring(7);
            outputCustomItem = plugin.getCustomItemManager().getCustomItem(customId);
            if (outputCustomItem == null) {
                plugin.getDebug().log("Unknown custom item: " + customId + " in convert action");
                return false;
            }
        }

        // Count how many input items are in the pouch
        int count = 0;
        Map<String, Integer> itemsToRemove = new HashMap<>();

        for (Map.Entry<String, ItemStack> entry : pouch.getContents().entrySet()) {
            ItemStack item = entry.getValue();
            boolean matches = false;

            if (inputCustomItem != null) {
                matches = inputCustomItem.matches(item);
            } else {
                // For vanilla items
                try {
                    matches = item.getType().name().equals(inputId);
                } catch (IllegalArgumentException e) {
                    plugin.getDebug().log("Invalid material name in convert action: " + inputId);
                    return false;
                }
            }

            if (matches) {
                int available = item.getAmount();
                int needed = inputAmount - count;
                int toTake = Math.min(available, needed);

                count += toTake;
                itemsToRemove.put(entry.getKey(), toTake);

                if (count >= inputAmount) {
                    break;
                }
            }
        }

        // Check if we have enough items
        if (count < inputAmount) {
            player.sendMessage(ChatColor.RED + "Not enough items to convert! Need " + inputAmount + ", have " + count);
            return false;
        }

        // Remove the input items
        Map<String, ItemStack> contents = pouch.getContents();
        for (Map.Entry<String, Integer> entry : itemsToRemove.entrySet()) {
            String key = entry.getKey();
            int amountToRemove = entry.getValue();
            ItemStack item = contents.get(key);

            if (item.getAmount() <= amountToRemove) {
                contents.remove(key);
            } else {
                item.setAmount(item.getAmount() - amountToRemove);
            }
        }
        pouch.setContents(contents);

        // Create the output item
        if (outputCustomItem != null) {
            outputItem = outputCustomItem.toItemStack(outputAmount);
        } else {
            // For vanilla items
            try {
                outputItem = new ItemStack(org.bukkit.Material.valueOf(outputId), outputAmount);
            } catch (IllegalArgumentException e) {
                plugin.getDebug().log("Invalid material name in convert action: " + outputId);
                return false;
            }
        }

        // Add the output item to the pouch
        if (!pouch.addItem(outputItem)) {
            // Handle overflow
            if (pouch.getOverflowMode().equals("inventory")) {
                player.getInventory().addItem(outputItem);
                player.sendMessage(ChatColor.YELLOW + "The pouch is full! Items went to your inventory.");
            } else if (pouch.getOverflowMode().equals("drop")) {
                player.getWorld().dropItemNaturally(player.getLocation(), outputItem);
                player.sendMessage(ChatColor.YELLOW + "The pouch is full! Items were dropped at your feet.");
            } else if (pouch.getOverflowMode().equals("sell") && plugin.getVaultHook().isEnabled()) {
                double amount = plugin.getVaultHook().sellItem(player, outputItem);
                player.sendMessage(ChatColor.GREEN + "The pouch is full! Items were sold for " +
                        plugin.getVaultHook().formatMoney(amount) + ".");
            }
        }

        // Update stats
        pouch.getStats().incrementActionsPerformed();

        // Send message to the player
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        return true;
    }

    /**
     * Get the type of this action
     *
     * @return The action type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the configuration of this action
     *
     * @return The action configuration
     */
    public Map<String, Object> getConfig() {
        return new HashMap<>(config);
    }
}