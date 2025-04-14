package com.brekfst.fruitPouches.events;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.Pouch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for pouch trade events
 */
public class PouchTradeListener implements Listener {

    private final FruitPouches plugin;

    /**
     * Create a new pouch trade listener
     *
     * @param plugin The plugin instance
     */
    public PouchTradeListener(FruitPouches plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the player drop item event
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        String pouchId = Pouch.getPouchIdFromItem(plugin, item);

        if (pouchId == null) {
            return;
        }

        // Check if the pouch is locked
        Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

        if (pouch == null) {
            return;
        }

        // Load the configuration for this pouch
        boolean locked = plugin.getConfigManager().getPouchesConfig().getBoolean("pouches." + pouchId + ".lock", false);

        if (locked) {
            event.setCancelled(true);
            plugin.getMessageUtils().sendMessage(player, "pouches.locked", "pouch", pouch.getDisplayName());
        }
    }

    /**
     * Handle the inventory click event
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // Skip if this is our plugin's GUI
        if (plugin.getGuiManager().isPluginGUI(inventory)) {
            return;
        }

        // Check if the clicked item is a pouch
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem != null && !currentItem.getType().isAir()) {
            String pouchId = Pouch.getPouchIdFromItem(plugin, currentItem);

            if (pouchId != null) {
                // Check if the pouch is locked
                Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

                if (pouch != null) {
                    // Load the configuration for this pouch
                    boolean locked = plugin.getConfigManager().getPouchesConfig().getBoolean("pouches." + pouchId + ".lock", false);

                    if (locked) {
                        // Block moving locked pouches in trading inventories
                        if (inventory.getType() == InventoryType.MERCHANT ||
                                inventory.getType() == InventoryType.CHEST ||
                                inventory.getType() == InventoryType.DISPENSER ||
                                inventory.getType() == InventoryType.DROPPER ||
                                inventory.getType() == InventoryType.HOPPER) {

                            event.setCancelled(true);
                            plugin.getMessageUtils().sendMessage(player, "pouches.locked", "pouch", pouch.getDisplayName());
                        }
                    }
                }
            }
        }

        // Check if the cursor item is a pouch
        ItemStack cursorItem = event.getCursor();

        if (cursorItem != null && !cursorItem.getType().isAir()) {
            String pouchId = Pouch.getPouchIdFromItem(plugin, cursorItem);

            if (pouchId != null) {
                // Check if the pouch is locked
                Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

                if (pouch != null) {
                    // Load the configuration for this pouch
                    boolean locked = plugin.getConfigManager().getPouchesConfig().getBoolean("pouches." + pouchId + ".lock", false);

                    if (locked) {
                        // Block moving locked pouches in trading inventories
                        if (inventory.getType() == InventoryType.MERCHANT ||
                                inventory.getType() == InventoryType.CHEST ||
                                inventory.getType() == InventoryType.DISPENSER ||
                                inventory.getType() == InventoryType.DROPPER ||
                                inventory.getType() == InventoryType.HOPPER) {

                            event.setCancelled(true);
                            plugin.getMessageUtils().sendMessage(player, "pouches.locked", "pouch", pouch.getDisplayName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle the inventory drag event
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // Skip if this is our plugin's GUI
        if (plugin.getGuiManager().isPluginGUI(inventory)) {
            return;
        }

        // Check if the old cursor is a pouch
        ItemStack oldCursor = event.getOldCursor();

        if (oldCursor != null && !oldCursor.getType().isAir()) {
            String pouchId = Pouch.getPouchIdFromItem(plugin, oldCursor);

            if (pouchId != null) {
                // Check if the pouch is locked
                Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

                if (pouch != null) {
                    // Load the configuration for this pouch
                    boolean locked = plugin.getConfigManager().getPouchesConfig().getBoolean("pouches." + pouchId + ".lock", false);

                    if (locked) {
                        // Block moving locked pouches in trading inventories
                        if (inventory.getType() == InventoryType.MERCHANT ||
                                inventory.getType() == InventoryType.CHEST ||
                                inventory.getType() == InventoryType.DISPENSER ||
                                inventory.getType() == InventoryType.DROPPER ||
                                inventory.getType() == InventoryType.HOPPER) {

                            event.setCancelled(true);
                            plugin.getMessageUtils().sendMessage(player, "pouches.locked", "pouch", pouch.getDisplayName());
                        }
                    }
                }
            }
        }
    }
}