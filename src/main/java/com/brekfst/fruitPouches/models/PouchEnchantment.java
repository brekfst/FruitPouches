package com.brekfst.fruitPouches.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an enchantment that can be applied to a pouch
 */
public class PouchEnchantment {

    private final Type type;
    private final int level;

    /**
     * Create a new pouch enchantment
     *
     * @param type The enchantment type
     * @param level The enchantment level
     */
    public PouchEnchantment(Type type, int level) {
        this.type = type;
        this.level = level;
    }

    /**
     * Get the enchantment type
     *
     * @return The enchantment type
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the enchantment level
     *
     * @return The enchantment level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Convert to a map representation
     *
     * @return Map representation
     */
    public Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type.getId());
        result.put("level", level);
        return result;
    }

    /**
     * Enchantment type
     */
    public static class Type {
        private final String id;
        private final String display;
        private final String description;
        private final int maxLevel;
        private final Map<String, Object> cost;
        private final Map<String, Object> effect;

        /**
         * Create a new enchantment type
         *
         * @param id The unique identifier
         * @param config The configuration section
         */
        public Type(String id, ConfigurationSection config) {
            this.id = id;
            this.display = config.getString("display", id);
            this.description = config.getString("description", "");
            this.maxLevel = config.getInt("max_level", 1);

            // Parse cost
            ConfigurationSection costSection = config.getConfigurationSection("cost");
            this.cost = new HashMap<>();
            if (costSection != null) {
                for (String key : costSection.getKeys(false)) {
                    cost.put(key, costSection.get(key));
                }
            }

            // Parse effect
            ConfigurationSection effectSection = config.getConfigurationSection("effect");
            this.effect = new HashMap<>();
            if (effectSection != null) {
                for (String key : effectSection.getKeys(false)) {
                    effect.put(key, effectSection.get(key));
                }
            }
        }

        /**
         * Get the unique identifier
         *
         * @return The unique identifier
         */
        public String getId() {
            return id;
        }

        /**
         * Get the display name
         *
         * @return The display name
         */
        public String getDisplay() {
            return display;
        }

        /**
         * Get the description
         *
         * @return The description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Get the maximum level
         *
         * @return The maximum level
         */
        public int getMaxLevel() {
            return maxLevel;
        }

        /**
         * Get the cost for a specific level
         *
         * @param level The level
         * @return The cost map
         */
        public Map<String, Object> getCostForLevel(int level) {
            Map<String, Object> result = new HashMap<>(cost);

            // Apply multiplier
            if (result.containsKey("amount") && result.containsKey("multiplier")) {
                double amount = Double.parseDouble(result.get("amount").toString());
                double multiplier = Double.parseDouble(result.get("multiplier").toString());

                double finalAmount = amount;
                for (int i = 1; i < level; i++) {
                    finalAmount *= multiplier;
                }

                result.put("amount", (int) Math.ceil(finalAmount));
            }

            return result;
        }

        /**
         * Get the effect
         *
         * @return The effect map
         */
        public Map<String, Object> getEffect() {
            return new HashMap<>(effect);
        }

        /**
         * Get the effect for a specific level
         *
         * @param level The level
         * @return The effect value for the level
         */
        public double getEffectValueForLevel(String key, int level) {
            if (!effect.containsKey(key)) {
                return 0.0;
            }

            double baseValue = Double.parseDouble(effect.get(key).toString());
            return baseValue * level;
        }
    }
}