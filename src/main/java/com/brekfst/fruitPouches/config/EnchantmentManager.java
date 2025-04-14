package com.brekfst.fruitPouches.config;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.CustomItem;
import com.brekfst.fruitPouches.models.Pouch;
import com.brekfst.fruitPouches.models.PouchEnchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages enchantments for pouches
 */
public class EnchantmentManager {

    private final FruitPouches plugin;
    private final Map<String, PouchEnchantment.Type> enchantmentTypes;

    /**
     * Create a new enchantment manager
     *
     * @param plugin The plugin instance
     */
    public EnchantmentManager(FruitPouches plugin) {
        this.plugin = plugin;
        this.enchantmentTypes = new HashMap<>();

        // Load all enchantment types
        loadEnchantmentTypes();
    }

    /**
     * Load all enchantment types from the configuration
     */
    public void loadEnchantmentTypes() {
        enchantmentTypes.clear();

        FileConfiguration config = plugin.getConfigManager().getEnchantmentsConfig();
        ConfigurationSection enchantmentsSection = config.getConfigurationSection("enchantments");

        if (enchantmentsSection != null) {
            for (String id : enchantmentsSection.getKeys(false)) {
                ConfigurationSection enchantmentSection = enchantmentsSection.getConfigurationSection(id);
                if (enchantmentSection != null) {
                    PouchEnchantment.Type type = new PouchEnchantment.Type(id, enchantmentSection);
                    enchantmentTypes.put(id, type);
                    plugin.getDebug().log("Loaded enchantment type: " + id);
                }
            }
        }

        plugin.getDebug().log("Loaded " + enchantmentTypes.size() + " enchantment types");
    }

    /**
     * Get an enchantment type by ID
     *
     * @param id The enchantment type ID
     * @return The enchantment type, or null if not found
     */
    public PouchEnchantment.Type getEnchantmentType(String id) {
        return enchantmentTypes.get(id);
    }

    /**
     * Get all enchantment types
     *
     * @return All enchantment types
     */
    public Map<String, PouchEnchantment.Type> getAllEnchantmentTypes() {
        return Collections.unmodifiableMap(enchantmentTypes);
    }

    /**
     * Check if an enchantment type exists
     *
     * @param id The enchantment type ID
     * @return true if the enchantment type exists
     */
    public boolean enchantmentTypeExists(String id) {
        return enchantmentTypes.containsKey(id);
    }

    /**
     * Get all enchantment type IDs
     *
     * @return All enchantment type IDs
     */
    public Set<String> getAllEnchantmentTypeIds() {
        return Collections.unmodifiableSet(enchantmentTypes.keySet());
    }

    /**
     * Apply an enchantment to a pouch
     *
     * @param player The player
     * @param pouchId The pouch ID
     * @param enchantmentId The enchantment ID
     * @param level The enchantment level
     * @return true if the enchantment was applied
     */
    public boolean applyEnchantment(Player player, String pouchId, String enchantmentId, int level) {
        // Check if the enchantment type exists
        PouchEnchantment.Type enchantmentType = getEnchantmentType(enchantmentId);
        if (enchantmentType == null) {
            plugin.getMessageUtils().sendMessage(player, "enchantments.invalid-enchantment", "enchantment", enchantmentId);
            return false;
        }

        // Check if the level is valid
        if (level <= 0 || level > enchantmentType.getMaxLevel()) {
            plugin.getMessageUtils().sendMessage(player, "enchantments.max-level", "enchantment", enchantmentType.getDisplay());
            return false;
        }

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

        // Get the pouch data
        Pouch pouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

        if (pouch == null) {
            plugin.getMessageUtils().sendMessage(player, "pouches.invalid-pouch", "pouch", pouchId);
            return false;
        }

        // Check if the pouch already has the enchantment
        for (PouchEnchantment enchantment : pouch.getEnchantments()) {
            if (enchantment.getType().getId().equals(enchantmentId)) {
                if (enchantment.getLevel() >= level) {
                    plugin.getMessageUtils().sendMessage(player, "enchantments.max-level", "enchantment", enchantmentType.getDisplay());
                    return false;
                }

                // Update the enchantment level
                pouch.getEnchantments().remove(enchantment);
                pouch.getEnchantments().add(new PouchEnchantment(enchantmentType, level));

                // Save the pouch data
                plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

                // Update the item in the player's inventory
                player.getInventory().setItem(pouchSlot, pouch.toItemStack(plugin));

                // Send message to the player
                plugin.getMessageUtils().sendMessage(player, "enchantments.applied", "enchantment", enchantmentType.getDisplay(), "level", String.valueOf(level), "pouch", pouch.getDisplayName());

                return true;
            }
        }

        // Check if the pouch has any free enchantment slots
        if (pouch.getEnchantments().size() >= pouch.getEnchantmentSlots()) {
            plugin.getMessageUtils().sendMessage(player, "enchantments.max-slots", "pouch", pouch.getDisplayName());
            return false;
        }

        // Check if the player can afford the enchantment
        Map<String, Object> cost = enchantmentType.getCostForLevel(level);
        if (cost.containsKey("item") && cost.containsKey("amount")) {
            String itemId = cost.get("item").toString();
            int amount = Integer.parseInt(cost.get("amount").toString());

            if (!hasEnoughItems(player, itemId, amount)) {
                plugin.getMessageUtils().sendMessage(player, "enchantments.cannot-afford", "cost", amount + "x " + (itemId.startsWith("custom:") ? itemId.substring(7) : itemId));
                return false;
            }

            // Remove the items from the player's inventory
            removeItems(player, itemId, amount);
        }

        // Add the enchantment
        pouch.getEnchantments().add(new PouchEnchantment(enchantmentType, level));

        // Save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Update the item in the player's inventory
        player.getInventory().setItem(pouchSlot, pouch.toItemStack(plugin));

        // Send message to the player
        plugin.getMessageUtils().sendMessage(player, "enchantments.applied", "enchantment", enchantmentType.getDisplay(), "level", String.valueOf(level), "pouch", pouch.getDisplayName());

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
