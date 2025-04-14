package com.brekfst.fruitPouches.gui;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.Pouch;
import com.brekfst.fruitPouches.utils.ItemUtils;
import com.fruitster.fruitpouches.models.PouchSkin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for the skin shop
 */
public class SkinShopGUI implements Listener {

    private final FruitPouches plugin;
    private final Player player;
    private final String pouchId;
    private final Inventory inventory;
    private final Map<Integer, String> skinSlots;

    /**
     * Create a new skin shop GUI
     *
     * @param plugin The plugin instance
     * @param player The player
     * @param pouchId The pouch ID
     */
    public SkinShopGUI(FruitPouches plugin, Player player, String pouchId) {
        this.plugin = plugin;
        this.player = player;
        this.pouchId = pouchId;
        this.skinSlots = new HashMap<>();

        // Create the inventory
        Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

        if (pouch == null) {
            this.inventory = Bukkit.createInventory(null, 9, ChatColor.RED + "Invalid Pouch");
            return;
        }

        this.inventory = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', "&6Skin Shop: &r" + pouch.getDisplayName()));

        // Register this listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Initialize the GUI
        initializeGUI();
    }

    /**
     * Initialize the GUI
     */
    private void initializeGUI() {
        // Clear the inventory
        inventory.clear();
        skinSlots.clear();

        // Get the pouch skins
        Map<String, PouchSkin> skins = plugin.getSkinManager().getPouchSkins(pouchId);

        if (skins.isEmpty()) {
            // No skins available
            ItemStack noSkins = ItemUtils.createItem(Material.BARRIER, ChatColor.RED + "No Skins Available",
                    List.of(ChatColor.GRAY + "There are no skins available for this pouch."));

            inventory.setItem(22, noSkins);
            return;
        }

        // Add skins to the inventory
        int slot = 0;

        for (Map.Entry<String, PouchSkin> entry : skins.entrySet()) {
            String skinId = entry.getKey();
            PouchSkin skin = entry.getValue();

            // Get the item from Head Database
            ItemStack skinItem;

            if (!skin.getHdbId().isEmpty() && plugin.getHeadDatabaseHook().isEnabled()) {
                skinItem = plugin.getHeadDatabaseHook().getHeadFromID(skin.getHdbId());

                if (skinItem == null) {
                    skinItem = new ItemStack(Material.PLAYER_HEAD);
                }
            } else {
                skinItem = new ItemStack(Material.PLAYER_HEAD);
            }

            // Set the item metadata
            List<String> lore = new ArrayList<>(skin.getLore());
            lore.add("");

            boolean owned = plugin.getSkinManager().playerOwnsSkin(player.getUniqueId(), pouchId, skinId);

            if (owned) {
                lore.add(ChatColor.GREEN + "You own this skin");
                lore.add(ChatColor.YELLOW + "Click to apply");
            } else {
                lore.add(ChatColor.YELLOW + "Cost: " + ChatColor.WHITE + skin.getFormattedCost());
                lore.add(ChatColor.YELLOW + "Click to purchase");
            }

            ItemStack displayItem = ItemUtils.createItem(skinItem.getType(), skin.getName(), lore);

            inventory.setItem(slot, displayItem);
            skinSlots.put(slot, skinId);

            slot++;
        }
    }

    /**
     * Open the GUI
     */
    public void open() {
        // Play sound
        String soundName = plugin.getConfigManager().getMainConfig().getString("gui.open-sound", "BLOCK_CHEST_OPEN");

        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getDebug().log("Invalid sound: " + soundName);
        }

        player.openInventory(inventory);
    }

    /**
     * Handle the inventory click event
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) {
            return;
        }

        int slot = event.getSlot();

        // Check if this is a skin slot
        if (skinSlots.containsKey(slot)) {
            String skinId = skinSlots.get(slot);
            handleSkinClick(skinId);
        }
    }

    /**
     * Handle the inventory drag event
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory() == inventory) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle the inventory close event
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory) {
            return;
        }

        // Play close sound
        String soundName = plugin.getConfigManager().getMainConfig().getString("gui.close-sound", "BLOCK_CHEST_CLOSE");

        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getDebug().log("Invalid sound: " + soundName);
        }

        // Unregister this listener
        HandlerList.unregisterAll(this);
    }

    /**
     * Handle a skin click
     *
     * @param skinId The skin ID
     */
    private void handleSkinClick(String skinId) {
        // Apply the skin
        plugin.getSkinManager().applySkin(player, pouchId, skinId);

        // Close the inventory
        Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 1L);
    }
}