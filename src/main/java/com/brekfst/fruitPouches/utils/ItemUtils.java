package com.brekfst.fruitPouches.utils;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.CustomItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for item operations
 */
public class ItemUtils {

    /**
     * Create an item stack
     *
     * @param material The material
     * @param amount The amount
     * @param name The name
     * @param lore The lore
     * @return The item stack
     */
    public static ItemStack createItem(Material material, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();

                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }

                meta.setLore(coloredLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create an item stack
     *
     * @param material The material
     * @param name The name
     * @param lore The lore
     * @return The item stack
     */
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, 1, name, lore);
    }

    /**
     * Create an item stack
     *
     * @param material The material
     * @param name The name
     * @return The item stack
     */
    public static ItemStack createItem(Material material, String name) {
        return createItem(material, 1, name, null);
    }

    /**
     * Get an item from a string
     *
     * @param plugin The plugin instance
     * @param itemString The item string
     * @param amount The amount
     * @return The item stack
     */
    public static ItemStack getItemFromString(FruitPouches plugin, String itemString, int amount) {
        if (itemString.startsWith("custom:")) {
            String customId = itemString.substring(7);
            CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customId);

            if (customItem != null) {
                return customItem.toItemStack(amount);
            }

            plugin.getDebug().log("Unknown custom item: " + customId);
            return null;
        } else {
            try {
                Material material = Material.valueOf(itemString);
                return new ItemStack(material, amount);
            } catch (IllegalArgumentException e) {
                plugin.getDebug().log("Invalid material: " + itemString);
                return null;
            }
        }
    }

    /**
     * Get an item from a string
     *
     * @param plugin The plugin instance
     * @param itemString The item string
     * @return The item stack
     */
    public static ItemStack getItemFromString(FruitPouches plugin, String itemString) {
        return getItemFromString(plugin, itemString, 1);
    }
}