package com.brekfst.fruitPouches.config;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Manages configuration files for the plugin
 */
public class ConfigManager {

    private final FruitPouches plugin;
    private final Map<String, FileConfiguration> configs;

    /**
     * Create a new config manager
     *
     * @param plugin The plugin instance
     */
    public ConfigManager(FruitPouches plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();

        // Load all configs
        loadConfig("config.yml");
        loadConfig("custom_items.yml");
        loadConfig("pouches.yml");
        loadConfig("enchantments.yml");
        loadConfig("messages.yml");
        loadConfig("skins.yml");
    }

    /**
     * Load a configuration file
     *
     * @param fileName The file name
     */
    public void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);

        plugin.getDebug().log("Loaded configuration file: " + fileName);
    }

    /**
     * Save a configuration file
     *
     * @param fileName The file name
     */
    public void saveConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        FileConfiguration config = configs.get(fileName);

        if (config != null) {
            try {
                config.save(file);
                plugin.getDebug().log("Saved configuration file: " + fileName);
            } catch (IOException e) {
                plugin.getDebug().logException(e, "Failed to save configuration file: " + fileName);
            }
        }
    }

    /**
     * Reload a configuration file
     *
     * @param fileName The file name
     */
    public void reloadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getDebug().log("Created new configuration file: " + fileName + " (was missing)");
        }

        // Force reload from disk instead of cache
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Also load the default config from the jar to get any missing values
            if (plugin.getResource(fileName) != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(plugin.getResource(fileName), StandardCharsets.UTF_8));
                config.setDefaults(defaultConfig);
            }

            configs.put(fileName, config);
            plugin.getDebug().log("Reloaded configuration file: " + fileName);
        } catch (Exception e) {
            plugin.getDebug().logException(e, "Failed to reload configuration file: " + fileName);
        }
    }

    /**
     * Reload all configuration files
     */
    public void reloadAllConfigs() {
        plugin.getDebug().log("Starting full configuration reload...");

        // Reload the plugin's main config first
        plugin.reloadConfig();
        configs.put("config.yml", plugin.getConfig());

        // Then reload all other config files
        for (String fileName : new HashSet<>(configs.keySet())) {
            if (!fileName.equals("config.yml")) {
                reloadConfig(fileName);
            }
        }

        plugin.getDebug().log("Configuration reload complete.");
    }

    /**
     * Get a configuration file
     *
     * @param fileName The file name
     * @return The configuration file
     */
    public FileConfiguration getConfig(String fileName) {
        return configs.getOrDefault(fileName, null);
    }

    /**
     * Get the plugin's main configuration
     *
     * @return The main configuration
     */
    public FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }

    /**
     * Get the custom items configuration
     *
     * @return The custom items configuration
     */
    public FileConfiguration getCustomItemsConfig() {
        return getConfig("custom_items.yml");
    }

    /**
     * Get the pouches configuration
     *
     * @return The pouches configuration
     */
    public FileConfiguration getPouchesConfig() {
        return getConfig("pouches.yml");
    }

    /**
     * Get the enchantments configuration
     *
     * @return The enchantments configuration
     */
    public FileConfiguration getEnchantmentsConfig() {
        return getConfig("enchantments.yml");
    }

    /**
     * Get the messages configuration
     *
     * @return The messages configuration
     */
    public FileConfiguration getMessagesConfig() {
        return getConfig("messages.yml");
    }

    /**
     * Get the skins configuration
     *
     * @return The skins configuration
     */
    public FileConfiguration getSkinsConfig() {
        return getConfig("skins.yml");
    }

    /**
     * Create a backup of a configuration file
     *
     * @param fileName The file name
     */
    public void createBackup(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            return;
        }

        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String backupFileName = fileName.replace(".yml", "") + "-" + System.currentTimeMillis() + ".yml";
        File backupFile = new File(backupDir, backupFileName);

        try {
            FileConfiguration config = configs.get(fileName);
            if (config != null) {
                config.save(backupFile);
                plugin.getDebug().log("Created backup of " + fileName + " to " + backupFileName);
            }
        } catch (IOException e) {
            plugin.getDebug().logException(e, "Failed to create backup of " + fileName);
        }
    }

    /**
     * Create backups of all configuration files
     */
    public void createAllBackups() {
        for (String fileName : configs.keySet()) {
            createBackup(fileName);
        }
    }

    /**
     * Checks if the plugin should create automatic backups
     *
     * @return true if automatic backups are enabled
     */
    public boolean isAutoBackupEnabled() {
        return getMainConfig().getBoolean("data.enable-backups", true);
    }

    /**
     * Get the auto backup interval in minutes
     *
     * @return The auto backup interval
     */
    public int getAutoBackupInterval() {
        return getMainConfig().getInt("data.backup-interval", 60);
    }
}