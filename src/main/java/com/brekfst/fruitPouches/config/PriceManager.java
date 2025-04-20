package com.brekfst.fruitPouches.config;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages item selling prices for the plugin
 */
public class PriceManager {
    private final FruitPouches plugin;
    private final Map<String, Double> itemPrices;
    private double defaultPrice;

    /**
     * Create a new price manager
     *
     * @param plugin The plugin instance
     */
    public PriceManager(FruitPouches plugin) {
        this.plugin = plugin;
        this.itemPrices = new HashMap<>();

        // Create the prices.yml file if it doesn't exist
        File file = new File(plugin.getDataFolder(), "prices.yml");
        if (!file.exists()) {
            plugin.saveResource("prices.yml", false);
        }

        // Load prices
        loadPrices();
    }

    /**
     * Load all prices from the configuration
     */
    public void loadPrices() {
        itemPrices.clear();

        // Load config
        File file = new File(plugin.getDataFolder(), "prices.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Get default price
        defaultPrice = config.getDouble("default-value", 1.0);

        // Load all prices
        if (config.isConfigurationSection("prices")) {
            for (String key : config.getConfigurationSection("prices").getKeys(false)) {
                double price = config.getDouble("prices." + key, defaultPrice);
                itemPrices.put(key, price);
                plugin.getDebug().log("Loaded price for " + key + ": " + price);
            }
        }

        plugin.getDebug().log("Loaded " + itemPrices.size() + " item prices");
    }

    /**
     * Save an item price to the configuration
     *
     * @param itemKey The item key (material name or custom:id)
     * @param price The price
     */
    public void savePrice(String itemKey, double price) {
        // Add to cache
        itemPrices.put(itemKey, price);

        // Save to file
        File file = new File(plugin.getDataFolder(), "prices.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("prices." + itemKey, price);

        try {
            config.save(file);
            plugin.getDebug().log("Saved price for " + itemKey + ": " + price);
        } catch (IOException e) {
            plugin.getDebug().logException(e, "Failed to save prices.yml");
        }
    }

    /**
     * Get the price of an item
     *
     * @param item The item to get the price of
     * @return The price of the item
     */
    public double getPrice(ItemStack item) {
        if (item == null) {
            return 0.0;
        }

        // Check for custom items
        String customItemId = plugin.getCustomItemManager().matchCustomItem(item);
        if (customItemId != null) {
            String key = "custom:" + customItemId;
            if (itemPrices.containsKey(key)) {
                return itemPrices.get(key);
            }
        }

        // Check for vanilla items
        String materialName = item.getType().name();
        return itemPrices.getOrDefault(materialName, defaultPrice);
    }

    /**
     * Get the default price
     *
     * @return The default price
     */
    public double getDefaultPrice() {
        return defaultPrice;
    }

    /**
     * Set the default price
     *
     * @param defaultPrice The new default price
     */
    public void setDefaultPrice(double defaultPrice) {
        this.defaultPrice = defaultPrice;

        // Save to file
        File file = new File(plugin.getDataFolder(), "prices.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("default-value", defaultPrice);

        try {
            config.save(file);
            plugin.getDebug().log("Saved default price: " + defaultPrice);
        } catch (IOException e) {
            plugin.getDebug().logException(e, "Failed to save prices.yml");
        }
    }
}
