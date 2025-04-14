package com.brekfst.fruitPouches.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an upgrade for a pouch
 */
public class PouchUpgrade {

    private final int level;
    private final int slots;
    private final int enchantmentSlots;
    private final Map<String, Object> cost;

    /**
     * Create a new pouch upgrade
     *
     * @param level The upgrade level
     * @param slots The number of slots
     * @param enchantmentSlots The number of enchantment slots
     * @param cost The cost of the upgrade
     */
    public PouchUpgrade(int level, int slots, int enchantmentSlots, Map<String, Object> cost) {
        this.level = level;
        this.slots = slots;
        this.enchantmentSlots = enchantmentSlots;
        this.cost = new HashMap<>(cost);
    }

    /**
     * Get the upgrade level
     *
     * @return The upgrade level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get the number of slots
     *
     * @return The number of slots
     */
    public int getSlots() {
        return slots;
    }

    /**
     * Get the number of enchantment slots
     *
     * @return The number of enchantment slots
     */
    public int getEnchantmentSlots() {
        return enchantmentSlots;
    }

    /**
     * Get the cost
     *
     * @return The cost map
     */
    public Map<String, Object> getCost() {
        return new HashMap<>(cost);
    }

    /**
     * Get the cost as a formatted string
     *
     * @return The formatted cost string
     */
    public String getFormattedCost() {
        if (cost.containsKey("item") && cost.containsKey("amount")) {
            String item = cost.get("item").toString();
            int amount = Integer.parseInt(cost.get("amount").toString());

            return amount + "x " + (item.startsWith("custom:") ? item.substring(7) : item);
        }

        return "Free";
    }

    /**
     * Convert to a map representation
     *
     * @return Map representation
     */
    public Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("level", level);
        result.put("slots", slots);
        result.put("enchantment_slots", enchantmentSlots);
        result.put("cost", cost);
        return result;
    }
}