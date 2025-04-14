package com.brekfst.fruitPouches.utils;

import com.brekfst.fruitPouches.FruitPouches;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Hook for HeadDatabase plugin
 */
public class HeadDatabaseHook {

    private final FruitPouches plugin;
    private boolean enabled;
    private HeadDatabaseAPI api;

    /**
     * Create a new HeadDatabase hook
     *
     * @param plugin The plugin instance
     */
    public HeadDatabaseHook(FruitPouches plugin) {
        this.plugin = plugin;
        this.enabled = false;
        this.api = null;

        // Try to hook into HeadDatabase
        if (Bukkit.getPluginManager().getPlugin("HeadDatabase") != null) {
            try {
                this.api = new HeadDatabaseAPI();
                this.enabled = true;
                plugin.getDebug().log("Successfully hooked into HeadDatabase");
            } catch (Exception e) {
                plugin.getDebug().logException(e, "Failed to hook into HeadDatabase");
            }
        } else {
            plugin.getDebug().log("HeadDatabase plugin not found, skin features will be disabled");
        }
    }

    /**
     * Check if HeadDatabase is enabled
     *
     * @return true if HeadDatabase is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get a head from HeadDatabase
     *
     * @param id The head ID
     * @return The head, or null if not found
     */
    public ItemStack getHeadFromID(String id) {
        if (!enabled || api == null) {
            return null;
        }

        try {
            return api.getItemHead(id);
        } catch (Exception e) {
            plugin.getDebug().logException(e, "Failed to get head from HeadDatabase with ID: " + id);
            return null;
        }
    }
}