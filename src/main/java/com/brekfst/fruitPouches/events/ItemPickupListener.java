package com.brekfst.fruitPouches.events;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.Pouch;
import com.brekfst.fruitPouches.models.PouchEnchantment;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for item pickup events
 */
public class ItemPickupListener implements Listener {

    private final FruitPouches plugin;
    private final Map<UUID, Long> playerCooldowns;
    private final Map<UUID, Long> itemCooldowns;
    private final int pickupDelay;
    private final double pickupRange;
    private final boolean autoPickup;

    /**
     * Create a new item pickup listener
     *
     * @param plugin The plugin instance
     */
    public ItemPickupListener(FruitPouches plugin) {
        this.plugin = plugin;
        this.playerCooldowns = new HashMap<>();
        this.itemCooldowns = new HashMap<>();
        this.pickupDelay = plugin.getConfigManager().getMainConfig().getInt("general.pickup-delay", 0);
        this.pickupRange = plugin.getConfigManager().getMainConfig().getDouble("general.pickup-range", 3.0);
        this.autoPickup = plugin.getConfigManager().getMainConfig().getBoolean("general.auto-pickup", true);

        // Start a repeating task to check for items periodically
        startPickupTask();
    }

    /**
     * Start a repeating task to check for nearby items
     */
    private void startPickupTask() {
        int checkInterval = 5; // Check every 5 ticks (0.25 seconds)

        new BukkitRunnable() {
            @Override
            public void run() {
                // Skip if auto-pickup is disabled
                if (!autoPickup) {
                    return;
                }

                // Process all online players
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    processPlayerPickup(player);
                }
            }
        }.runTaskTimer(plugin, 20L, checkInterval);
    }

    /**
     * Process pickup for a player
     *
     * @param player The player
     */
    private void processPlayerPickup(Player player) {
        // Skip players in spectator mode
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Check cooldown
        if (playerCooldowns.containsKey(player.getUniqueId())) {
            long lastPickup = playerCooldowns.get(player.getUniqueId());

            if (System.currentTimeMillis() - lastPickup < pickupDelay) {
                return;
            }
        }

        // Find pouches in inventory
        List<Map.Entry<String, Integer>> pouches = findPouchesInInventory(player);

        if (pouches.isEmpty()) {
            return;
        }

        // Set cooldown
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Calculate pickup range with enchantments
        double range = calculatePickupRange(player, pouches);

        // Get nearby items
        List<Item> nearbyItems = getNearbyItems(player, range);

        // Try to pick up items
        for (Item item : nearbyItems) {
            tryPickupItem(player, item, pouches);
        }
    }

    /**
     * Find pouches in a player's inventory
     *
     * @param player The player
     * @return List of pouch ID and slot entries
     */
    private List<Map.Entry<String, Integer>> findPouchesInInventory(Player player) {
        List<Map.Entry<String, Integer>> pouches = new ArrayList<>();

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);

            if (item != null && !item.getType().isAir()) {
                String pouchId = Pouch.getPouchIdFromItem(plugin, item);

                if (pouchId != null) {
                    pouches.add(Map.entry(pouchId, i));
                }
            }
        }

        return pouches;
    }

    /**
     * Calculate pickup range with enchantments
     *
     * @param player The player
     * @param pouches List of pouch entries
     * @return The calculated pickup range
     */
    private double calculatePickupRange(Player player, List<Map.Entry<String, Integer>> pouches) {
        double range = this.pickupRange;

        for (Map.Entry<String, Integer> entry : pouches) {
            String pouchId = entry.getKey();
            Pouch pouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

            if (pouch != null) {
                for (PouchEnchantment enchantment : pouch.getEnchantments()) {
                    if (enchantment.getType().getId().equals("efficiency")) {
                        range += enchantment.getType().getEffectValueForLevel("pickup_range", enchantment.getLevel());
                    }
                }
            }
        }

        return range;
    }

    /**
     * Get nearby items
     *
     * @param player The player
     * @param range The pickup range
     * @return List of nearby items
     */
    private List<Item> getNearbyItems(Player player, double range) {
        List<Entity> nearbyEntities = player.getNearbyEntities(range, range, range);
        List<Item> nearbyItems = new ArrayList<>();

        for (Entity entity : nearbyEntities) {
            if (entity instanceof Item) {
                nearbyItems.add((Item) entity);
            }
        }

        return nearbyItems;
    }

    /**
     * Try to pick up an item
     *
     * @param player The player
     * @param item The item
     * @param pouches List of pouch entries
     */
    private void tryPickupItem(Player player, Item item, List<Map.Entry<String, Integer>> pouches) {
        // Skip items on cooldown
        if (itemCooldowns.containsKey(item.getUniqueId())) {
            long lastPickup = itemCooldowns.get(item.getUniqueId());

            if (System.currentTimeMillis() - lastPickup < 1000) {
                return;
            }
        }

        ItemStack itemStack = item.getItemStack();

        // Debug log the item being checked
        plugin.getDebug().log("Checking item for pickup: " + itemStack.getType().name() + " x" + itemStack.getAmount());

        // Try to add the item to a pouch
        for (Map.Entry<String, Integer> entry : pouches) {
            String pouchId = entry.getKey();

            // Debug log the pouch being checked
            plugin.getDebug().log("Checking pouch: " + pouchId);

            Pouch pouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

            if (pouch != null && pouch.hasUsePermission(player) && pouch.meetsPickupConditions(player)) {
                // Debug exact item details
                plugin.getDebug().log("Item type: " + itemStack.getType().name());
                plugin.getDebug().log("Item has meta: " + itemStack.hasItemMeta());
                if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                    plugin.getDebug().log("Item display name: " + itemStack.getItemMeta().getDisplayName());
                }

                // Check if the pouch can pick up this item
                if (pouch.canPickup(plugin, itemStack)) {
                    plugin.getDebug().log("Pouch can pick up item: " + itemStack.getType().name());

                    // Create a clone to avoid modifying the original ItemStack
                    ItemStack clonedItem = itemStack.clone();

                    // Try to add the item to the pouch
                    if (pouch.addItem(clonedItem)) {
                        // Remove the original item
                        item.remove();

                        // Play pickup sound
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, 1.0f);

                        // Update stats
                        plugin.getStatsManager().trackItemsCollected(pouchId, player.getUniqueId(), clonedItem.getAmount());

                        // Save the pouch data
                        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

                        // Notify the player
                        if (plugin.getConfigManager().getMainConfig().getBoolean("general.pickup-messages", true)) {
                            player.sendMessage(ChatColor.GRAY + "[" + ChatColor.translateAlternateColorCodes('&', pouch.getDisplayName()) +
                                    ChatColor.GRAY + "] " + ChatColor.GREEN + "Picked up " + clonedItem.getAmount() + "x " +
                                    getItemDisplayName(clonedItem));
                        }

                        return; // Successfully picked up
                    } else {
                        // Handle overflow
                        handleOverflow(player, pouch, clonedItem);
                        return; // Handled through overflow
                    }
                } else {
                    plugin.getDebug().log("Pouch cannot pick up item: " + itemStack.getType().name());
                }
            }
        }
    }

    /**
     * Get the display name of an item
     *
     * @param item The item
     * @return The display name
     */
    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        } else {
            return formatMaterialName(item.getType().name());
        }
    }

    /**
     * Format a material name (convert SOME_MATERIAL to Some Material)
     *
     * @param name The material name
     * @return The formatted name
     */
    private String formatMaterialName(String name) {
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append(" ");
            }

            result.append(parts[i].substring(0, 1).toUpperCase());
            result.append(parts[i].substring(1).toLowerCase());
        }

        return result.toString();
    }

    /**
     * Handle the entity pickup item event
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();

        // Check for pouches in inventory
        List<Map.Entry<String, Integer>> pouches = findPouchesInInventory(player);

        if (pouches.isEmpty()) {
            return;
        }

        // Try to add the item to a pouch
        for (Map.Entry<String, Integer> entry : pouches) {
            String pouchId = entry.getKey();
            Pouch pouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

            if (pouch != null) {
                if (pouch.hasUsePermission(player) && pouch.canPickup(plugin, itemStack) && pouch.meetsPickupConditions(player)) {
                    event.setCancelled(true);

                    int originalAmount = itemStack.getAmount();

                    // Try to add the item to the pouch
                    if (pouch.addItem(itemStack.clone())) {
                        // Update the item stack amount
                        itemStack.setAmount(0);
                        item.remove();

                        // Play pickup sound
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, 1.0f);

                        // Update stats
                        plugin.getStatsManager().trackItemsCollected(pouchId, player.getUniqueId(), originalAmount);

                        // Save the pouch data
                        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

                        // Notify the player
                        if (plugin.getConfigManager().getMainConfig().getBoolean("general.pickup-messages", true)) {
                            player.sendMessage(ChatColor.GRAY + "[" + ChatColor.translateAlternateColorCodes('&', pouch.getDisplayName()) +
                                    ChatColor.GRAY + "] " + ChatColor.GREEN + "Picked up " + originalAmount + "x " +
                                    getItemDisplayName(itemStack));
                        }

                        break;
                    } else {
                        // Handle overflow
                        handleOverflow(player, pouch, itemStack);

                        // Update stats
                        plugin.getStatsManager().trackItemsCollected(pouchId, player.getUniqueId(), originalAmount);

                        // Save the pouch data
                        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

                        break;
                    }
                }
            }
        }
    }

    /**
     * Monitor item spawn events to check for auto-pickup
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!autoPickup) {
            return;
        }

        // Find nearby players with pouches
        Item item = event.getEntity();
        ItemStack itemStack = item.getItemStack();

        for (Player player : item.getWorld().getPlayers()) {
            // Skip players that are too far away
            if (player.getLocation().distance(item.getLocation()) > pickupRange * 2) {
                continue;
            }

            // Skip players in spectator mode
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            // Find pouches in inventory
            List<Map.Entry<String, Integer>> pouches = findPouchesInInventory(player);

            if (pouches.isEmpty()) {
                continue;
            }

            // Schedule a delayed task to try to pick up the item
            // This gives time for other plugins to process the item
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!item.isValid() || item.isDead()) {
                        return;
                    }

                    tryPickupItem(player, item, pouches);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /**
     * Handle overflow when a pouch is full
     *
     * @param player The player
     * @param pouch The pouch
     * @param itemStack The item stack that caused overflow
     */
    private void handleOverflow(Player player, Pouch pouch, ItemStack itemStack) {
        String overflowMode = pouch.getOverflowMode();
        String sound = plugin.getConfigManager().getMainConfig().getString("overflow.sound", "BLOCK_NOTE_BLOCK_BELL");
        boolean showParticles = plugin.getConfigManager().getMainConfig().getBoolean("overflow.show-particles", true);
        String particleType = plugin.getConfigManager().getMainConfig().getString("overflow.particle-type", "VILLAGER_ANGRY");

        // Play sound
        try {
            player.playSound(player.getLocation(), Sound.valueOf(sound), 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getDebug().log("Invalid sound: " + sound);
        }

        // Show particles
        if (showParticles) {
            try {
                player.getWorld().spawnParticle(Particle.valueOf(particleType), player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            } catch (IllegalArgumentException e) {
                plugin.getDebug().log("Invalid particle type: " + particleType);
            }
        }

        switch (overflowMode) {
            case "inventory":
                player.getInventory().addItem(itemStack);
                plugin.getMessageUtils().sendMessage(player, "pouches.full-pouch", "pouch", pouch.getDisplayName());
                break;
            case "drop":
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                plugin.getMessageUtils().sendMessage(player, "pouches.full-pouch-drop", "pouch", pouch.getDisplayName());
                break;
            case "sell":
                if (plugin.getVaultHook().isEnabled()) {
                    double amount = plugin.getVaultHook().sellItem(player, itemStack);
                    plugin.getMessageUtils().sendMessage(player, "pouches.full-pouch-sell", "pouch", pouch.getDisplayName(), "amount", plugin.getVaultHook().formatMoney(amount));
                } else {
                    // Fall back to inventory
                    player.getInventory().addItem(itemStack);
                    plugin.getMessageUtils().sendMessage(player, "pouches.full-pouch", "pouch", pouch.getDisplayName());
                }
                break;
            default:
                player.getInventory().addItem(itemStack);
                plugin.getMessageUtils().sendMessage(player, "pouches.full-pouch", "pouch", pouch.getDisplayName());
                break;
        }
    }
}