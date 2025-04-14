package com.brekfst.fruitPouches.gui;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.Pouch;
import com.brekfst.fruitPouches.models.PouchUpgrade;
import com.brekfst.fruitPouches.utils.ItemUtils;
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
 * GUI for upgrading pouches
 */
public class UpgradeGUI implements Listener {

    private final FruitPouches plugin;
    private final Player player;
    private final String pouchId;
    private final Inventory inventory;
    private final Map<Integer, Integer> upgradeSlots;

    /**
     * Create a new upgrade GUI
     *
     * @param plugin The plugin instance
     * @param player The player
     * @param pouchId The pouch ID
     */
    public UpgradeGUI(FruitPouches plugin, Player player, String pouchId) {
        this.plugin = plugin;
        this.player = player;
        this.pouchId = pouchId;
        this.upgradeSlots = new HashMap<>();

        // Create the inventory
        Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

        if (pouch == null) {
            this.inventory = Bukkit.createInventory(null, 9, ChatColor.RED + "Invalid Pouch");
            return;
        }

        this.inventory = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&6Upgrade: &r" + pouch.getDisplayName()));

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
        upgradeSlots.clear();

        // Get the pouch
        Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

        if (pouch == null) {
            // Invalid pouch
            ItemStack invalidPouch = ItemUtils.createItem(Material.BARRIER, ChatColor.RED + "Invalid Pouch",
                    List.of(ChatColor.GRAY + "The pouch is invalid."));

            inventory.setItem(13, invalidPouch);
            return;
        }

        // Get the player's pouch data
        Pouch playerPouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

        if (playerPouch == null) {
            // Invalid player pouch
            ItemStack invalidPouch = ItemUtils.createItem(Material.BARRIER, ChatColor.RED + "Invalid Pouch",
                    List.of(ChatColor.GRAY + "The pouch is invalid."));

            inventory.setItem(13, invalidPouch);
            return;
        }

        // Get the current level
        int currentLevel = playerPouch.getCurrentLevel();

        // Get the upgrades
        List<PouchUpgrade> upgrades = pouch.getUpgrades();

        if (upgrades.isEmpty()) {
            // No upgrades available
            ItemStack noUpgrades = ItemUtils.createItem(Material.BARRIER, ChatColor.RED + "No Upgrades Available",
                    List.of(ChatColor.GRAY + "There are no upgrades available for this pouch."));

            inventory.setItem(13, noUpgrades);
            return;
        }

        // Show current pouch info
        List<String> currentLore = new ArrayList<>();
        currentLore.add(ChatColor.GRAY + "Level: " + ChatColor.GREEN + currentLevel);
        currentLore.add(ChatColor.GRAY + "Slots: " + ChatColor.AQUA + playerPouch.getSlots());
        currentLore.add(ChatColor.GRAY + "Enchantment Slots: " + ChatColor.LIGHT_PURPLE + playerPouch.getEnchantmentSlots());

        ItemStack currentInfo = ItemUtils.createItem(Material.BOOK, ChatColor.YELLOW + "Current Pouch", currentLore);
        inventory.setItem(4, currentInfo);

        // Show available upgrades
        if (currentLevel >= upgrades.size()) {
            // Max level reached
            ItemStack maxLevel = ItemUtils.createItem(Material.BARRIER, ChatColor.RED + "Maximum Level Reached",
                    List.of(ChatColor.GRAY + "This pouch is already at the maximum level."));

            inventory.setItem(13, maxLevel);
            return;
        }

        // Add upgrade buttons
        for (int i = currentLevel; i < upgrades.size() && i < currentLevel + 3; i++) {
            PouchUpgrade upgrade = upgrades.get(i);
            int slot = 11 + ((i - currentLevel) * 2);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.GREEN + (i + 1));
            lore.add(ChatColor.GRAY + "Slots: " + ChatColor.AQUA + upgrade.getSlots());
            lore.add(ChatColor.GRAY + "Enchantment Slots: " + ChatColor.LIGHT_PURPLE + upgrade.getEnchantmentSlots());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Cost: " + ChatColor.WHITE + upgrade.getFormattedCost());
            lore.add(ChatColor.YELLOW + "Click to upgrade");

            ItemStack upgradeButton = ItemUtils.createItem(Material.EXPERIENCE_BOTTLE, ChatColor.GREEN + "Upgrade to Level " + (i + 1), lore);

            inventory.setItem(slot, upgradeButton);
            upgradeSlots.put(slot, i + 1);
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

        // Check if this is an upgrade slot
        if (upgradeSlots.containsKey(slot)) {
            int level = upgradeSlots.get(slot);
            handleUpgradeClick(level);
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
     * Handle an upgrade click
     *
     * @param level The level to upgrade to
     */
    private void handleUpgradeClick(int level) {
        // Get the pouch
        Pouch pouch = plugin.getPouchManager().getPouch(pouchId);

        if (pouch == null) {
            return;
        }

        // Get the player's pouch data
        Pouch playerPouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

        if (playerPouch == null) {
            return;
        }

        // Get the current level
        int currentLevel = playerPouch.getCurrentLevel();

        // Check if the upgrade is valid
        if (level <= currentLevel || level > pouch.getUpgrades().size()) {
            return;
        }

        // Only allow upgrading one level at a time
        if (level > currentLevel + 1) {
            plugin.getMessageUtils().sendMessage(player, "upgrades.must-upgrade-in-order");
            return;
        }

        // Upgrade the pouch
        plugin.getPouchManager().upgradePouch(player, pouchId);

        // Close the inventory
        Bukkit.getScheduler().runTaskLater(plugin, player::closeInventory, 1L);
    }
}