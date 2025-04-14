package com.brekfst.fruitPouches.data;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Base class for data managers
 */
public class DataManager {

    protected final FruitPouches plugin;

    /**
     * Create a new data manager
     *
     * @param plugin The plugin instance
     */
    public DataManager(FruitPouches plugin) {
        this.plugin = plugin;

        // Create data directory
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        // Create player data directory
        File playerDataDir = new File(dataDir, "players");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }
    }

    /**
     * Get the player data file
     *
     * @param playerId The player UUID
     * @return The player data file
     */
    protected File getPlayerDataFile(UUID playerId) {
        return new File(plugin.getDataFolder(), "data/players/" + playerId.toString() + ".yml");
    }

    /**
     * Load YAML configuration from a file
     *
     * @param file The file
     * @return The YAML configuration
     */
    protected YamlConfiguration loadConfig(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getDebug().logException(e, "Failed to create file: " + file.getPath());
            }
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Save YAML configuration to a file
     *
     * @param config The YAML configuration
     * @param file The file
     */
    protected void saveConfig(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getDebug().logException(e, "Failed to save file: " + file.getPath());
        }
    }

    /**
     * Create a backup of a file
     *
     * @param file The file
     */
    protected void createBackup(File file) {
        if (!file.exists()) {
            return;
        }

        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String fileName = file.getName();
        String backupFileName = fileName.replace(".yml", "") + "-" + System.currentTimeMillis() + ".yml";
        File backupFile = new File(backupDir, backupFileName);

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.save(backupFile);
            plugin.getDebug().log("Created backup of " + fileName + " to " + backupFileName);
        } catch (IOException e) {
            plugin.getDebug().logException(e, "Failed to create backup of " + fileName);
        }
    }
}