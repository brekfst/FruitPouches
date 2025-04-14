package com.brekfst.fruitPouches.gui;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.Pouch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages GUIs for the plugin
 */
public class GuiManager {

    private final FruitPouches plugin;
    private final Map<UUID, PouchGUI> openGUIs;

    /**
     * Create a new GUI manager
     *
     * @param plugin The plugin instance
     */
    public GuiManager(FruitPouches plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
    }

    /**
     * Open a pouch GUI for a player
     *
     * @param player The player
     * @param pouch The pouch
     */
    public void openPouchGUI(Player player, Pouch pouch) {
        PouchGUI gui = new PouchGUI(plugin, player, pouch);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }

    /**
     * Close all open GUIs
     */
    public void closeAllGUIs() {
        for (Map.Entry<UUID, PouchGUI> entry : openGUIs.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());

            if (player != null) {
                player.closeInventory();
            }
        }

        openGUIs.clear();
    }

    /**
     * Check if an inventory is a plugin GUI
     *
     * @param inventory The inventory
     * @return true if the inventory is a plugin GUI
     */
    public boolean isPluginGUI(Inventory inventory) {
        for (PouchGUI gui : openGUIs.values()) {
            if (gui.getInventory().equals(inventory)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the PouchGUI for a player
     *
     * @param player The player
     * @return The PouchGUI, or null if not found
     */
    public PouchGUI getPlayerGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }

    /**
     * Remove a player's GUI
     *
     * @param player The player
     */
    public void removePlayerGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
    }
}