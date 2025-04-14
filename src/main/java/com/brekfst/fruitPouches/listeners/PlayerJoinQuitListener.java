package com.brekfst.fruitPouches.listeners;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player join and quit events
 */
public class PlayerJoinQuitListener implements Listener {

    private final FruitPouches plugin;

    /**
     * Create a new player join/quit listener
     *
     * @param plugin The plugin instance
     */
    public PlayerJoinQuitListener(FruitPouches plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the player join event
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer().getUniqueId());
    }

    /**
     * Handle the player quit event
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data
        plugin.getPlayerDataManager().savePlayerData(event.getPlayer().getUniqueId());
    }
}