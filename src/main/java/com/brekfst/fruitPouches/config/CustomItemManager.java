package com.brekfst.fruitPouches.config;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.CustomItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages custom items for the plugin
 */
public class CustomItemManager {

    private final FruitPouches plugin;
    private final Map<String, CustomItem> customItems;

    /**
     * Create a new custom item manager
     *
     * @param plugin The plugin instance
     */
    public CustomItemManager(FruitPouches plugin) {
        this.plugin = plugin;
        this.customItems = new HashMap<>();

        // Load all custom items
        loadCustomItems();
    }

    /**
     * Load all custom items from the configuration
     */
    public void loadCustomItems() {
        customItems.clear();

        FileConfiguration config = plugin.getConfigManager().getCustomItemsConfig();
        ConfigurationSection customItemsSection = config.getConfigurationSection("custom_items");

        if (customItemsSection != null) {
            for (String id : customItemsSection.getKeys(false)) {
                ConfigurationSection itemSection = customItemsSection.getConfigurationSection(id);
                if (itemSection != null) {
                    CustomItem customItem = new CustomItem(id, itemSection);
                    customItems.put(id, customItem);
                    plugin.getDebug().log("Loaded custom item: " + id);
                }
            }
        }

        plugin.getDebug().log("Loaded " + customItems.size() + " custom items");
    }

    /**
     * Get a custom item by ID
     *
     * @param id The custom item ID
     * @return The custom item, or null if not found
     */
    public CustomItem getCustomItem(String id) {
        return customItems.get(id);
    }

    /**
     * Get all custom items
     *
     * @return All custom items
     */
    public Map<String, CustomItem> getAllCustomItems() {
        return Collections.unmodifiableMap(customItems);
    }

    /**
     * Check if a custom item exists
     *
     * @param id The custom item ID
     * @return true if the custom item exists
     */
    public boolean customItemExists(String id) {
        return customItems.containsKey(id);
    }

    /**
     * Save a custom item
     *
     * @param id The custom item ID
     * @param item The item to save
     * @param force Whether to overwrite an existing custom item
     * @return true if the item was saved
     */
    public boolean saveCustomItem(String id, ItemStack item, boolean force) {
        if (customItemExists(id) && !force) {
            return false;
        }

        CustomItem customItem = new CustomItem(id, item);
        customItems.put(id, customItem);

        // Save to config
        FileConfiguration config = plugin.getConfigManager().getCustomItemsConfig();
        ConfigurationSection customItemsSection = config.getConfigurationSection("custom_items");

        if (customItemsSection == null) {
            customItemsSection = config.createSection("custom_items");
        }

        ConfigurationSection itemSection = customItemsSection.createSection(id);
        customItem.saveToConfig(itemSection);

        plugin.getConfigManager().saveConfig("custom_items.yml");
        plugin.getDebug().log("Saved custom item: " + id);

        return true;
    }

    /**
     * Remove a custom item
     *
     * @param id The custom item ID
     * @return true if the item was removed
     */
    public boolean removeCustomItem(String id) {
        if (!customItemExists(id)) {
            return false;
        }

        customItems.remove(id);

        // Remove from config
        FileConfiguration config = plugin.getConfigManager().getCustomItemsConfig();
        ConfigurationSection customItemsSection = config.getConfigurationSection("custom_items");

        if (customItemsSection != null) {
            customItemsSection.set(id, null);
        }

        plugin.getConfigManager().saveConfig("custom_items.yml");
        plugin.getDebug().log("Removed custom item: " + id);

        return true;
    }

    /**
     * Get all custom item IDs
     *
     * @return All custom item IDs
     */
    public Set<String> getAllCustomItemIds() {
        return Collections.unmodifiableSet(customItems.keySet());
    }

    /**
     * Match an ItemStack to a custom item
     *
     * @param item The ItemStack to match
     * @return The custom item ID, or null if no match
     */
    public String matchCustomItem(ItemStack item) {
        for (Map.Entry<String, CustomItem> entry : customItems.entrySet()) {
            if (entry.getValue().matches(item)) {
                return entry.getKey();
            }
        }

        return null;
    }
}