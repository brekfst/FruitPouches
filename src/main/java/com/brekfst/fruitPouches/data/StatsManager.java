package com.brekfst.fruitPouches.data;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages global stats for pouches
 */
public class StatsManager extends DataManager {

    private final Map<String, Map<String, Integer>> globalStats;

    /**
     * Create a new stats manager
     *
     * @param plugin The plugin instance
     */
    public StatsManager(FruitPouches plugin) {
        super(plugin);
        this.globalStats = new ConcurrentHashMap<>();

        // Load global stats
        loadGlobalStats();
    }

    /**
     * Load global stats from file
     */
    private void loadGlobalStats() {
        File file = new File(plugin.getDataFolder(), "data/stats.yml");

        if (!file.exists()) {
            return;
        }

        // Load async
        plugin.getAsyncExecutor().submit(() -> {
            try {
                YamlConfiguration config = loadConfig(file);

                for (String pouchId : config.getKeys(false)) {
                    ConfigurationSection pouchSection = config.getConfigurationSection(pouchId);

                    if (pouchSection != null) {
                        Map<String, Integer> pouchStats = new HashMap<>();

                        for (String key : pouchSection.getKeys(false)) {
                            pouchStats.put(key, pouchSection.getInt(key));
                        }

                        globalStats.put(pouchId, pouchStats);
                    }
                }

                plugin.getDebug().log("Loaded global stats for " + globalStats.size() + " pouches");
            } catch (Exception e) {
                plugin.getDebug().logException(e, "Failed to load global stats");
            }
        });
    }

    /**
     * Save global stats to file
     */
    public void saveGlobalStats() {
        if (globalStats.isEmpty()) {
            return;
        }

        // Save async
        plugin.getAsyncExecutor().submit(() -> {
            try {
                File file = new File(plugin.getDataFolder(), "data/stats.yml");
                YamlConfiguration config = new YamlConfiguration();

                for (Map.Entry<String, Map<String, Integer>> entry : globalStats.entrySet()) {
                    ConfigurationSection pouchSection = config.createSection(entry.getKey());

                    for (Map.Entry<String, Integer> statEntry : entry.getValue().entrySet()) {
                        pouchSection.set(statEntry.getKey(), statEntry.getValue());
                    }
                }

                // Backup before saving if enabled
                if (plugin.getConfigManager().isAutoBackupEnabled()) {
                    createBackup(file);
                }

                saveConfig(config, file);

                plugin.getDebug().log("Saved global stats");
            } catch (Exception e) {
                plugin.getDebug().logException(e, "Failed to save global stats");
            }
        });
    }

    /**
     * Increment a global stat
     *
     * @param pouchId The pouch ID
     * @param key The stat key
     * @param amount The amount to increment by
     */
    public void incrementGlobalStat(String pouchId, String key, int amount) {
        Map<String, Integer> pouchStats = globalStats.computeIfAbsent(pouchId, k -> new ConcurrentHashMap<>());
        pouchStats.put(key, pouchStats.getOrDefault(key, 0) + amount);
    }

    /**
     * Get a global stat
     *
     * @param pouchId The pouch ID
     * @param key The stat key
     * @return The stat value
     */
    public int getGlobalStat(String pouchId, String key) {
        Map<String, Integer> pouchStats = globalStats.getOrDefault(pouchId, new HashMap<>());
        return pouchStats.getOrDefault(key, 0);
    }

    /**
     * Get all global stats for a pouch
     *
     * @param pouchId The pouch ID
     * @return All stats for the pouch
     */
    public Map<String, Integer> getPouchStats(String pouchId) {
        return new HashMap<>(globalStats.getOrDefault(pouchId, new HashMap<>()));
    }

    /**
     * Track items collected by a player
     *
     * @param pouchId The pouch ID
     * @param playerId The player UUID
     * @param amount The amount of items collected
     */
    public void trackItemsCollected(String pouchId, UUID playerId, int amount) {
        // Update global stats
        incrementGlobalStat(pouchId, "items_collected", amount);
        incrementGlobalStat(pouchId, "items_collected_by_" + playerId.toString(), amount);
    }

    /**
     * Track actions performed by a player
     *
     * @param pouchId The pouch ID
     * @param playerId The player UUID
     */
    public void trackActionPerformed(String pouchId, UUID playerId) {
        // Update global stats
        incrementGlobalStat(pouchId, "actions_performed", 1);
        incrementGlobalStat(pouchId, "actions_performed_by_" + playerId.toString(), 1);
    }

    /**
     * Save all stats
     */
    public void saveAllStats() {
        saveGlobalStats();
    }
}