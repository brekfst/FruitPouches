package com.brekfst.fruitPouches.config;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.CustomItem;
import com.brekfst.fruitPouches.models.Pouch;
import com.brekfst.fruitPouches.models.PouchUpgrade;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages pouches for the plugin
 */
public class PouchManager {

    private final FruitPouches plugin;
    private final Map<String, Pouch> pouches;

    /**
     * Create a new pouch manager
     *
     * @param plugin The plugin instance
     */
    public PouchManager(FruitPouches plugin) {
        this.plugin = plugin;
        this.pouches = new HashMap<>();

        // Load all pouches
        loadPouches();
    }

    /**
     * Load all pouches from the configuration
     */
    public void loadPouches() {
        // Clear existing pouches first
        pouches.clear();
        plugin.getDebug().log("Cleared existing pouch configurations");

        // Load from configuration
        FileConfiguration config = plugin.getConfigManager().getPouchesConfig();
        ConfigurationSection pouchesSection = config.getConfigurationSection("pouches");

        if (pouchesSection != null) {
            for (String id : pouchesSection.getKeys(false)) {
                ConfigurationSection pouchSection = pouchesSection.getConfigurationSection(id);
                if (pouchSection != null) {
                    try {
                        Pouch pouch = new Pouch(id, pouchSection);
                        pouches.put(id, pouch);
                        plugin.getDebug().log("Loaded pouch configuration: " + id);
                    } catch (Exception e) {
                        plugin.getDebug().logException(e, "Failed to load pouch: " + id);
                    }
                }
            }
        }

        plugin.getDebug().log("Loaded " + pouches.size() + " pouch configurations");
    }

    /**
     * Get a pouch by ID
     *
     * @param id The pouch ID
     * @return The pouch, or null if not found
     */
    public Pouch getPouch(String id) {
        return pouches.get(id);
    }

    /**
     * Get all pouches
     *
     * @return All pouches
     */
    public Map<String, Pouch> getAllPouches() {
        return Collections.unmodifiableMap(pouches);
    }

    /**
     * Check if a pouch exists
     *
     * @param id The pouch ID
     * @return true if the pouch exists
     */
    public boolean pouchExists(String id) {
        return pouches.containsKey(id);
    }

    /**
     * Get all pouch IDs
     *
     * @return All pouch IDs
     */
    public Set<String> getAllPouchIds() {
        return Collections.unmodifiableSet(pouches.keySet());
    }

    /**
     * Give a pouch to a player
     *
     * @param player The player
     * @param pouchId The pouch ID
     * @param amount The amount
     * @return true if the pouch was given
     */
    public boolean givePouch(Player player, String pouchId, int amount) {
        if (!pouchExists(pouchId)) {
            plugin.getMessageUtils().sendMessage(player, "pouches.invalid-pouch", "pouch", pouchId);
            return false;
        }

        Pouch pouch = getPouch(pouchId);
        ItemStack pouchItem = pouch.toItemStack(plugin);
        pouchItem.setAmount(amount);

        HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(pouchItem);

        if (!notAdded.isEmpty()) {
            plugin.getMessageUtils().sendMessage(player, "pouches.full-inventory");

            // Drop the items that couldn't be added
            for (ItemStack item : notAdded.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        plugin.getMessageUtils().sendMessage(player, "pouches.received", "amount", String.valueOf(amount), "pouch", pouch.getDisplayName());

        return true;
    }

    /**
     * Upgrade a pouch
     *
     * @param player The player
     * @param pouchId The pouch ID
     * @return true if the pouch was upgraded
     */
    public boolean upgradePouch(Player player, String pouchId) {
        // Find the pouch in the player's inventory
        ItemStack pouchItem = null;
        int pouchSlot = -1;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && Pouch.isPouchOfType(plugin, item, pouchId)) {
                pouchItem = item;
                pouchSlot = i;
                break;
            }
        }

        if (pouchItem == null) {
            plugin.getMessageUtils().sendMessage(player, "pouches.invalid-pouch", "pouch", pouchId);
            return false;
        }

        // Get the pouch template
        Pouch pouchTemplate = getPouch(pouchId);
        if (pouchTemplate == null) {
            plugin.getMessageUtils().sendMessage(player, "pouches.invalid-pouch", "pouch", pouchId);
            return false;
        }

        // Get the player's pouch data
        Pouch pouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

        if (pouch == null) {
            plugin.getMessageUtils().sendMessage(player, "pouches.invalid-pouch", "pouch", pouchId);
            return false;
        }

        // Check if the pouch can be upgraded
        int currentLevel = pouch.getCurrentLevel();
        if (currentLevel >= pouchTemplate.getUpgrades().size()) {
            plugin.getMessageUtils().sendMessage(player, "upgrades.max-level", "pouch", pouch.getDisplayName());
            return false;
        }

        // Get the next upgrade
        PouchUpgrade upgrade = pouchTemplate.getUpgrades().get(currentLevel);

        // Check if the player can afford the upgrade
        Map<String, Object> cost = upgrade.getCost();
        if (cost.containsKey("item") && cost.containsKey("amount")) {
            String itemId = cost.get("item").toString();
            int amount = Integer.parseInt(cost.get("amount").toString());

            if (!hasEnoughItems(player, itemId, amount)) {
                plugin.getMessageUtils().sendMessage(player, "upgrades.cannot-afford", "cost", amount + "x " + (itemId.startsWith("custom:") ? itemId.substring(7) : itemId));
                return false;
            }

            // Remove the items from the player's inventory
            removeItems(player, itemId, amount);
        }

        // Upgrade the pouch
        pouch.setCurrentLevel(currentLevel + 1);

        // Save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Update the item in the player's inventory
        player.getInventory().setItem(pouchSlot, pouch.toItemStack(plugin));

        // Send message to the player
        plugin.getMessageUtils().sendMessage(player, "upgrades.upgraded", "pouch", pouch.getDisplayName(), "level", String.valueOf(currentLevel + 1));

        return true;
    }

    /**
     * Check if a player has enough items
     *
     * @param player The player
     * @param itemId The item ID
     * @param amount The amount
     * @return true if the player has enough items
     */
    private boolean hasEnoughItems(Player player, String itemId, int amount) {
        if (itemId.startsWith("custom:")) {
            String customId = itemId.substring(7);
            CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customId);

            if (customItem == null) {
                return false;
            }

            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && customItem.matches(item)) {
                    count += item.getAmount();
                }
            }

            return count >= amount;
        } else {
            // For vanilla items
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(itemId);
                int count = 0;

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == material) {
                        count += item.getAmount();
                    }
                }

                return count >= amount;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    /**
     * Remove items from a player's inventory
     *
     * @param player The player
     * @param itemId The item ID
     * @param amount The amount
     */
    private void removeItems(Player player, String itemId, int amount) {
        int remaining = amount;

        if (itemId.startsWith("custom:")) {
            String customId = itemId.substring(7);
            CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customId);

            if (customItem == null) {
                return;
            }

            for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
                ItemStack item = player.getInventory().getItem(i);

                if (item != null && customItem.matches(item)) {
                    int toRemove = Math.min(item.getAmount(), remaining);
                    remaining -= toRemove;

                    if (item.getAmount() - toRemove <= 0) {
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - toRemove);
                    }
                }
            }
        } else {
            // For vanilla items
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(itemId);

                for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
                    ItemStack item = player.getInventory().getItem(i);

                    if (item != null && item.getType() == material) {
                        int toRemove = Math.min(item.getAmount(), remaining);
                        remaining -= toRemove;

                        if (item.getAmount() - toRemove <= 0) {
                            player.getInventory().setItem(i, null);
                        } else {
                            item.setAmount(item.getAmount() - toRemove);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
    }
}