package com.brekfst.fruitPouches.data;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.Pouch;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player data for pouches
 */
public class PlayerDataManager extends DataManager {

    private final Map<UUID, Map<String, Pouch>> playerPouches;

    /**
     * Create a new player data manager
     *
     * @param plugin The plugin instance
     */
    public PlayerDataManager(FruitPouches plugin) {
        super(plugin);
        this.playerPouches = new ConcurrentHashMap<>();
    }

    /**
     * Load a player's pouch data
     *
     * @param playerId The player UUID
     */
    public void loadPlayerData(UUID playerId) {
        File file = getPlayerDataFile(playerId);

        if (!file.exists()) {
            // Create a new data file
            Map<String, Pouch> pouches = new HashMap<>();
            playerPouches.put(playerId, pouches);
            return;
        }

        // Load async
        plugin.getAsyncExecutor().submit(() -> {
            try {
                YamlConfiguration config = loadConfig(file);
                Map<String, Pouch> pouches = new HashMap<>();

                // Load pouches
                ConfigurationSection pouchesSection = config.getConfigurationSection("pouches");
                if (pouchesSection != null) {
                    for (String pouchId : pouchesSection.getKeys(false)) {
                        Pouch pouchTemplate = plugin.getPouchManager().getPouch(pouchId);

                        if (pouchTemplate != null) {
                            Pouch pouch = new Pouch(pouchId, plugin.getConfigManager().getPouchesConfig().getConfigurationSection("pouches." + pouchId));
                            // Fixed: Don't cast to YamlConfiguration
                            pouch.loadFromConfig(plugin, pouchesSection.getConfigurationSection(pouchId));
                            pouches.put(pouchId, pouch);
                        }
                    }
                }

                playerPouches.put(playerId, pouches);

                // Load skins
                Map<String, Set<String>> skins = new HashMap<>();
                ConfigurationSection skinsSection = config.getConfigurationSection("skins");

                if (skinsSection != null) {
                    for (String pouchId : skinsSection.getKeys(false)) {
                        Set<String> pouchSkins = new HashSet<>(skinsSection.getStringList(pouchId));
                        skins.put(pouchId, pouchSkins);
                    }
                }

                plugin.getSkinManager().setPlayerSkins(playerId, skins);

                plugin.getDebug().log("Loaded player data for " + playerId);
            } catch (Exception e) {
                plugin.getDebug().logException(e, "Failed to load player data for " + playerId);
            }
        });
    }

    /**
     * Save a player's pouch data
     *
     * @param playerId The player UUID
     */
    public void savePlayerData(UUID playerId) {
        Map<String, Pouch> pouches = playerPouches.get(playerId);

        if (pouches == null || pouches.isEmpty()) {
            return;
        }

        // Save async
        plugin.getAsyncExecutor().submit(() -> {
            try {
                File file = getPlayerDataFile(playerId);
                YamlConfiguration config = loadConfig(file);

                // Save pouches
                ConfigurationSection pouchesSection = config.createSection("pouches");

                for (Map.Entry<String, Pouch> entry : pouches.entrySet()) {
                    ConfigurationSection pouchSection = pouchesSection.createSection(entry.getKey());
                    // Fixed: Don't cast to YamlConfiguration
                    entry.getValue().saveToConfig(pouchSection);
                }

                // Save skins
                Map<String, Set<String>> skins = plugin.getSkinManager().getPlayerSkins(playerId);
                ConfigurationSection skinsSection = config.createSection("skins");

                for (Map.Entry<String, Set<String>> entry : skins.entrySet()) {
                    skinsSection.set(entry.getKey(), new ArrayList<>(entry.getValue()));
                }

                // Backup before saving if enabled
                if (plugin.getConfigManager().isAutoBackupEnabled()) {
                    createBackup(file);
                }

                saveConfig(config, file);

                plugin.getDebug().log("Saved player data for " + playerId);
            } catch (Exception e) {
                plugin.getDebug().logException(e, "Failed to save player data for " + playerId);
            }
        });
    }

    /**
     * Refresh all player pouches with updated configuration
     * Called after a config reload to ensure all pouches use the latest settings
     */
    public void refreshAllPlayerPouches() {
        plugin.getDebug().log("Refreshing all player pouch data with updated configurations...");

        // Process all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            Map<String, Pouch> playerPouchData = playerPouches.get(playerId);

            if (playerPouchData == null || playerPouchData.isEmpty()) {
                continue;
            }

            // Get the player's inventory pouches to update them
            boolean inventoryChanged = false;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType().isAir()) {
                    continue;
                }

                String pouchId = Pouch.getPouchIdFromItem(plugin, item);
                if (pouchId == null) {
                    continue;
                }

                // Get the updated pouch template
                Pouch templatePouch = plugin.getPouchManager().getPouch(pouchId);
                if (templatePouch == null) {
                    continue; // This pouch type no longer exists in config
                }

                // Get the player-specific pouch data
                Pouch playerPouch = playerPouchData.get(pouchId);
                if (playerPouch == null) {
                    continue;
                }

                // Update the player pouch with new configuration properties
                // but keep player-specific data (contents, level, etc.)
                playerPouch.updateFromTemplate(templatePouch);

                // Update the item in the player's inventory
                player.getInventory().setItem(i, playerPouch.toItemStack(plugin));
                inventoryChanged = true;

                plugin.getDebug().log("Updated pouch " + pouchId + " in " + player.getName() + "'s inventory");
            }

            // Update the player's inventory display if any pouches were updated
            if (inventoryChanged) {
                player.updateInventory();
            }
        }

        plugin.getDebug().log("Player pouch refresh complete");
    }

    /**
     * Save a player's pouch
     *
     * @param playerId The player UUID
     * @param pouch The pouch
     */
    public void savePlayerPouch(UUID playerId, Pouch pouch) {
        Map<String, Pouch> pouches = playerPouches.computeIfAbsent(playerId, k -> new HashMap<>());
        pouches.put(pouch.getId(), pouch);

        // Schedule async save
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            savePlayerData(playerId);
        }
    }

    /**
     * Get a player's pouch
     *
     * @param playerId The player UUID
     * @param pouchId The pouch ID
     * @return The pouch, or a new pouch if not found
     */
    public Pouch getPlayerPouch(UUID playerId, String pouchId) {
        Map<String, Pouch> pouches = playerPouches.computeIfAbsent(playerId, k -> new HashMap<>());

        if (pouches.containsKey(pouchId)) {
            return pouches.get(pouchId);
        }

        // Create a new pouch
        Pouch pouchTemplate = plugin.getPouchManager().getPouch(pouchId);

        if (pouchTemplate != null) {
            Pouch pouch = new Pouch(pouchId, plugin.getConfigManager().getPouchesConfig().getConfigurationSection("pouches." + pouchId));
            pouches.put(pouchId, pouch);
            return pouch;
        }

        return null;
    }

    /**
     * Get all pouches for a player
     *
     * @param playerId The player UUID
     * @return All pouches for the player
     */
    public Map<String, Pouch> getPlayerPouches(UUID playerId) {
        return Collections.unmodifiableMap(playerPouches.computeIfAbsent(playerId, k -> new HashMap<>()));
    }

    /**
     * Save a player's skins
     *
     * @param playerId The player UUID
     * @param skins The skins
     */
    public void savePlayerSkins(UUID playerId, Map<String, Set<String>> skins) {
        plugin.getSkinManager().setPlayerSkins(playerId, skins);

        // Schedule async save
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            savePlayerData(playerId);
        }
    }

    /**
     * Remove a player's data
     *
     * @param playerId The player UUID
     */
    public void removePlayerData(UUID playerId) {
        playerPouches.remove(playerId);
    }

    /**
     * Save all player data
     */
    public void saveAllPlayerData() {
        for (UUID playerId : playerPouches.keySet()) {
            savePlayerData(playerId);
        }
    }
}