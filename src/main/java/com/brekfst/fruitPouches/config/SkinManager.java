package com.brekfst.fruitPouches.config;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.CustomItem;
import com.brekfst.fruitPouches.models.Pouch;
import com.fruitster.fruitpouches.models.PouchSkin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages skins for pouches
 */
public class SkinManager {

    private final FruitPouches plugin;
    private final Map<String, Map<String, PouchSkin>> skins;
    private final Map<UUID, Map<String, Set<String>>> playerSkins;

    /**
     * Create a new skin manager
     *
     * @param plugin The plugin instance
     */
    public SkinManager(FruitPouches plugin) {
        this.plugin = plugin;
        this.skins = new HashMap<>();
        this.playerSkins = new HashMap<>();

        // Load all skins
        loadSkins();
    }

    /**
     * Load all skins from the configuration
     */
    public void loadSkins() {
        skins.clear();

        FileConfiguration config = plugin.getConfigManager().getSkinsConfig();
        ConfigurationSection skinShopSection = config.getConfigurationSection("skin_shop");

        if (skinShopSection != null) {
            for (String pouchId : skinShopSection.getKeys(false)) {
                ConfigurationSection pouchSkinsSection = skinShopSection.getConfigurationSection(pouchId);

                if (pouchSkinsSection != null) {
                    Map<String, PouchSkin> pouchSkins = new HashMap<>();

                    for (String skinId : pouchSkinsSection.getKeys(false)) {
                        ConfigurationSection skinSection = pouchSkinsSection.getConfigurationSection(skinId);

                        if (skinSection != null) {
                            PouchSkin skin = new PouchSkin(skinId, pouchId, skinSection);
                            pouchSkins.put(skinId, skin);
                            plugin.getDebug().log("Loaded skin: " + skinId + " for pouch: " + pouchId);
                        }
                    }

                    skins.put(pouchId, pouchSkins);
                }
            }
        }

        plugin.getDebug().log("Loaded skins for " + skins.size() + " pouch types");
    }

    /**
     * Get a skin by ID
     *
     * @param pouchId The pouch ID
     * @param skinId The skin ID
     * @return The skin, or null if not found
     */
    public PouchSkin getSkin(String pouchId, String skinId) {
        Map<String, PouchSkin> pouchSkins = skins.getOrDefault(pouchId, Collections.emptyMap());
        return pouchSkins.get(skinId);
    }

    /**
     * Get all skins for a pouch
     *
     * @param pouchId The pouch ID
     * @return All skins for the pouch
     */
    public Map<String, PouchSkin> getPouchSkins(String pouchId) {
        return Collections.unmodifiableMap(skins.getOrDefault(pouchId, Collections.emptyMap()));
    }

    /**
     * Check if a skin exists
     *
     * @param pouchId The pouch ID
     * @param skinId The skin ID
     * @return true if the skin exists
     */
    public boolean skinExists(String pouchId, String skinId) {
        Map<String, PouchSkin> pouchSkins = skins.getOrDefault(pouchId, Collections.emptyMap());
        return pouchSkins.containsKey(skinId);
    }

    public boolean applySkin(Player player, String pouchId, String skinId) {
        // Check if the skin exists
        if (!skinExists(pouchId, skinId)) {
            plugin.getMessageUtils().sendMessage(player, "skins.invalid-skin", "skin", skinId);
            return false;
        }

        // Find the pouch in the player's inventory
        ItemStack pouchItem = null;
        int pouchSlot = -1;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && Pouch.isPouchOfType(plugin, item, pouchId)) {
                pouchItem = item;
                pouchSlot = i;
                break;
            }
        }

        if (pouchItem == null) {
            plugin.getMessageUtils().sendMessage(player, "pouches.invalid-pouch", "pouch", pouchId);
            return false;
        }

        // Check if the player owns the skin
        if (!playerOwnsSkin(player.getUniqueId(), pouchId, skinId)) {
            PouchSkin skin = getSkin(pouchId, skinId);

            if (skin == null) {
                plugin.getMessageUtils().sendMessage(player, "skins.invalid-skin", "skin", skinId);
                return false;
            }

            // Check if the player can afford the skin
            Map<String, Object> cost = skin.getCost();
            if (cost.containsKey("item") && cost.containsKey("amount")) {
                String itemId = cost.get("item").toString();
                int amount = Integer.parseInt(cost.get("amount").toString());

                if (!hasEnoughItems(player, itemId, amount)) {
                    plugin.getMessageUtils().sendMessage(player, "skins.cannot-afford", "cost", skin.getFormattedCost());
                    return false;
                }

                // Remove the items from the player's inventory
                removeItems(player, itemId, amount);
            }

            // Add the skin to the player's owned skins
            addPlayerSkin(player.getUniqueId(), pouchId, skinId);

            // Send message to the player
            plugin.getMessageUtils().sendMessage(player, "skins.purchased", "skin", skin.getName(), "pouch", plugin.getPouchManager().getPouch(pouchId).getDisplayName());
        }

        // Get the player's pouch data
        Pouch pouch = plugin.getPlayerDataManager().getPlayerPouch(player.getUniqueId(), pouchId);

        if (pouch == null) {
            plugin.getDebug().log("Failed to get player pouch data for " + player.getName() + ", pouch: " + pouchId);
            return false;
        }

        // Apply the skin
        pouch.setCurrentSkin(skinId);

        // Save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Update the item in the player's inventory
        player.getInventory().setItem(pouchSlot, pouch.toItemStack(plugin));

        // Send message to the player
        plugin.getMessageUtils().sendMessage(player, "skins.applied", "skin", getSkin(pouchId, skinId).getName(), "pouch", pouch.getDisplayName());

        return true;
    }

    /**
     * Check if a player owns a skin
     *
     * @param playerId The player UUID
     * @param pouchId The pouch ID
     * @param skinId The skin ID
     * @return true if the player owns the skin
     */
    public boolean playerOwnsSkin(UUID playerId, String pouchId, String skinId) {
        Map<String, Set<String>> playerPouchSkins = playerSkins.getOrDefault(playerId, Collections.emptyMap());
        Set<String> ownedSkins = playerPouchSkins.getOrDefault(pouchId, Collections.emptySet());
        return ownedSkins.contains(skinId);
    }

    /**
     * Add a skin to a player's owned skins
     *
     * @param playerId The player UUID
     * @param pouchId The pouch ID
     * @param skinId The skin ID
     */
    public void addPlayerSkin(UUID playerId, String pouchId, String skinId) {
        Map<String, Set<String>> playerPouchSkins = playerSkins.computeIfAbsent(playerId, k -> new HashMap<>());
        Set<String> ownedSkins = playerPouchSkins.computeIfAbsent(pouchId, k -> new HashSet<>());
        ownedSkins.add(skinId);

        // Save to player data
        plugin.getPlayerDataManager().savePlayerSkins(playerId, playerPouchSkins);
    }

    /**
     * Get all skins owned by a player
     *
     * @param playerId The player UUID
     * @return All skins owned by the player
     */
    public Map<String, Set<String>> getPlayerSkins(UUID playerId) {
        return Collections.unmodifiableMap(playerSkins.getOrDefault(playerId, Collections.emptyMap()));
    }

    /**
     * Set all skins owned by a player
     *
     * @param playerId The player UUID
     * @param skins The skins owned by the player
     */
    public void setPlayerSkins(UUID playerId, Map<String, Set<String>> skins) {
        playerSkins.put(playerId, new HashMap<>(skins));
    }

    /**
     * Check if a player has enough items
     *
     * @param player The player
     * @param itemId The item ID
     * @param amount The amount
     * @return true if the player has enough items
     */
    private boolean hasEnoughItems(Player player, String itemId, int amount) {
        if (itemId.startsWith("custom:")) {
            String customId = itemId.substring(7);
            CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customId);

            if (customItem == null) {
                return false;
            }

            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && customItem.matches(item)) {
                    count += item.getAmount();
                }
            }

            return count >= amount;
        } else {
            // For vanilla items
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(itemId);
                int count = 0;

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == material) {
                        count += item.getAmount();
                    }
                }

                return count >= amount;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    /**
     * Remove items from a player's inventory
     *
     * @param player The player
     * @param itemId The item ID
     * @param amount The amount
     */
    private void removeItems(Player player, String itemId, int amount) {
        int remaining = amount;

        if (itemId.startsWith("custom:")) {
            String customId = itemId.substring(7);
            CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customId);

            if (customItem == null) {
                return;
            }

            for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
                ItemStack item = player.getInventory().getItem(i);

                if (item != null && customItem.matches(item)) {
                    int toRemove = Math.min(item.getAmount(), remaining);
                    remaining -= toRemove;

                    if (item.getAmount() - toRemove <= 0) {
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - toRemove);
                    }
                }
            }
        } else {
            // For vanilla items
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(itemId);

                for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
                    ItemStack item = player.getInventory().getItem(i);

                    if (item != null && item.getType() == material) {
                        int toRemove = Math.min(item.getAmount(), remaining);
                        remaining -= toRemove;

                        if (item.getAmount() - toRemove <= 0) {
                            player.getInventory().setItem(i, null);
                        } else {
                            item.setAmount(item.getAmount() - toRemove);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
    }
}