package com.brekfst.fruitPouches.events;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.Pouch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for pouch interaction events
 */
public class PouchInteractListener implements Listener {

    private final FruitPouches plugin;

    /**
     * Create a new pouch interact listener
     *
     * @param plugin The plugin instance
     */
    public PouchInteractListener(FruitPouches plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the player interact event
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        String pouchId = Pouch.getPouchIdFromItem(plugin, item);

        if (pouchId == null) {
            return;
        }

        // Cancel the event to prevent placing blocks if the player is holding a block
        event.setCancelled(true);

        if (player.isSneaking()) {
            // Skip opening GUI if the player is sneaking
            return;
        }

        // Check if the player has permission to use this pouch
        Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

        if (pouch == null) {
            plugin.getDebug().log("Invalid pouch ID: " + pouchId);
            return;
        }

        if (!pouch.hasUsePermission(player)) {
            plugin.getMessageUtils().sendMessage(player, "general.no-permission");
            return;
        }

        // Get the player's pouch data
        Pouch playerPouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

        if (playerPouch == null) {
            plugin.getDebug().log("Failed to get player pouch data for " + player.getName() + ", pouch: " + pouchId);
            return;
        }

        // Open the pouch GUI
        plugin.getGuiManager().openPouchGUI(player, playerPouch);

        // Update last used time
        playerPouch.getStats().updateLastUsed();
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), playerPouch);
    }
}