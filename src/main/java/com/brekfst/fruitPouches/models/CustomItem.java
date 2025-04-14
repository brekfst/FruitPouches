package com.brekfst.fruitPouches.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a custom item that can be stored in pouches
 */
public class CustomItem {

    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final List<ItemFlag> itemFlags;
    private final Map<String, String> nbtTags;
    private final double sellValue;

    /**
     * Create a new custom item from configuration
     *
     * @param id The unique identifier
     * @param config The configuration section
     */
    public CustomItem(String id, ConfigurationSection config) {
        this.id = id;
        this.material = Material.valueOf(config.getString("material", "STONE"));
        this.displayName = config.getString("display", id);
        this.lore = config.getStringList("lore");
        this.sellValue = config.getDouble("sell-value", 0.0);

        // Parse enchantments
        this.enchantments = new HashMap<>();
        for (String enchStr : config.getStringList("enchantments")) {
            String[] parts = enchStr.split(":");
            if (parts.length == 2) {
                try {
                    Enchantment enchantment = Enchantment.getByName(parts[0]);
                    int level = Integer.parseInt(parts[1]);
                    if (enchantment != null) {
                        this.enchantments.put(enchantment, level);
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid enchantment
                }
            }
        }

        // Parse item flags
        this.itemFlags = config.getStringList("flags").stream()
                .map(flag -> {
                    try {
                        return ItemFlag.valueOf(flag);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(flag -> flag != null)
                .collect(Collectors.toList());

        // Parse NBT data
        this.nbtTags = new HashMap<>();
        ConfigurationSection nbtSection = config.getConfigurationSection("nbt");
        if (nbtSection != null) {
            for (String key : nbtSection.getKeys(false)) {
                nbtTags.put(key, nbtSection.getString(key));
            }
        }
    }

    /**
     * Create a custom item from an existing ItemStack
     *
     * @param id The unique identifier
     * @param item The ItemStack to convert
     */
    public CustomItem(String id, ItemStack item) {
        this.id = id;
        this.material = item.getType();
        this.enchantments = new HashMap<>(item.getEnchantments());
        this.nbtTags = new HashMap<>();
        this.sellValue = 0.0;

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            this.displayName = meta.hasDisplayName() ? meta.getDisplayName() : id;
            this.lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            this.itemFlags = new ArrayList<>(meta.getItemFlags());

            // Try to extract NBT data
            PersistentDataContainer container = meta.getPersistentDataContainer();
            // This would require more complex implementation to extract NBT data
            // We'd need to use the NamespacedKey system to extract actual values
        } else {
            this.displayName = id;
            this.lore = new ArrayList<>();
            this.itemFlags = new ArrayList<>();
        }
    }

    /**
     * Convert this custom item to an ItemStack
     *
     * @return A new ItemStack with all the custom properties
     */
    public ItemStack toItemStack() {
        return toItemStack(1);
    }

    /**
     * Convert this custom item to an ItemStack with a specific amount
     *
     * @param amount The amount of items in the stack
     * @return A new ItemStack with all the custom properties
     */
    public ItemStack toItemStack(int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName(displayName);

            // Set lore
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }

            // Add item flags
            for (ItemFlag flag : itemFlags) {
                meta.addItemFlags(flag);
            }

            // Set NBT data
            // This would require more complex implementation to set NBT data
            // We'd need to use the NamespacedKey system to set actual values

            item.setItemMeta(meta);
        }

        // Add enchantments
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        return item;
    }

    /**
     * Check if this custom item matches an ItemStack
     *
     * @param item The ItemStack to check
     * @return true if the ItemStack matches this custom item
     */
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return !hasMetaData();
        }

        // Check display name
        if (meta.hasDisplayName() != (displayName != null && !displayName.equals(id))) {
            return false;
        }

        if (meta.hasDisplayName() && !meta.getDisplayName().equals(displayName)) {
            return false;
        }

        // Check lore
        if (!lore.isEmpty()) {
            List<String> itemLore = meta.getLore();
            if (itemLore == null || itemLore.size() != lore.size()) {
                return false;
            }

            for (int i = 0; i < lore.size(); i++) {
                if (!lore.get(i).equals(itemLore.get(i))) {
                    return false;
                }
            }
        }

        // Check enchantments (simplified check)
        if (!enchantments.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                if (item.getEnchantmentLevel(entry.getKey()) != entry.getValue()) {
                    return false;
                }
            }
        }

        // NBT checks would be more complex and would need to use the NamespacedKey system

        return true;
    }

    /**
     * Save this custom item to a configuration section
     *
     * @param config The configuration section to save to
     */
    public void saveToConfig(ConfigurationSection config) {
        config.set("material", material.name());
        config.set("display", displayName);
        config.set("lore", lore);
        config.set("sell-value", sellValue);

        // Save enchantments
        List<String> enchantStrings = enchantments.entrySet().stream()
                .map(entry -> entry.getKey().getName() + ":" + entry.getValue())
                .collect(Collectors.toList());
        config.set("enchantments", enchantStrings);

        // Save item flags
        List<String> flagStrings = itemFlags.stream()
                .map(ItemFlag::name)
                .collect(Collectors.toList());
        config.set("flags", flagStrings);

        // Save NBT data
        ConfigurationSection nbtSection = config.createSection("nbt");
        for (Map.Entry<String, String> entry : nbtTags.entrySet()) {
            nbtSection.set(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Check if this custom item has any metadata (display name, lore, enchantments)
     *
     * @return true if the item has metadata
     */
    private boolean hasMetaData() {
        return (displayName != null && !displayName.equals(id)) ||
                !lore.isEmpty() ||
                !enchantments.isEmpty() ||
                !itemFlags.isEmpty() ||
                !nbtTags.isEmpty();
    }

    /**
     * Get the unique ID of this custom item
     *
     * @return The unique ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the material of this custom item
     *
     * @return The material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Get the display name of this custom item
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the lore of this custom item
     *
     * @return The lore
     */
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    /**
     * Get the enchantments of this custom item
     *
     * @return The enchantments
     */
    public Map<Enchantment, Integer> getEnchantments() {
        return new HashMap<>(enchantments);
    }

    /**
     * Get the item flags of this custom item
     *
     * @return The item flags
     */
    public List<ItemFlag> getItemFlags() {
        return new ArrayList<>(itemFlags);
    }

    /**
     * Get the NBT tags of this custom item
     *
     * @return The NBT tags
     */
    public Map<String, String> getNbtTags() {
        return new HashMap<>(nbtTags);
    }

    /**
     * Get the sell value of this custom item
     *
     * @return The sell value
     */
    public double getSellValue() {
        return sellValue;
    }
}