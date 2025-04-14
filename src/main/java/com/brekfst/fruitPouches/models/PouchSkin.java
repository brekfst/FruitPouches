package com.fruitster.fruitpouches.models;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a skin that can be applied to a pouch
 */
public class PouchSkin {

    private final String id;
    private final String pouchId;
    private final String name;
    private final List<String> lore;
    private final String hdbId;
    private final Map<String, Object> cost;

    /**
     * Create a new pouch skin
     *
     * @param id The unique identifier
     * @param pouchId The pouch type this skin is for
     * @param config The configuration section
     */
    public PouchSkin(String id, String pouchId, ConfigurationSection config) {
        this.id = id;
        this.pouchId = pouchId;
        this.name = ChatColor.translateAlternateColorCodes('&', config.getString("name", id));

        // Parse lore
        List<String> configLore = config.getStringList("lore");
        this.lore = new ArrayList<>();
        for (String line : configLore) {
            this.lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        this.hdbId = config.getString("hdb_id", "");

        // Parse cost
        ConfigurationSection costSection = config.getConfigurationSection("cost");
        this.cost = new HashMap<>();
        if (costSection != null) {
            for (String key : costSection.getKeys(false)) {
                cost.put(key, costSection.get(key));
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
     * Get the pouch type
     *
     * @return The pouch type
     */
    public String getPouchId() {
        return pouchId;
    }

    /**
     * Get the display name
     *
     * @return The display name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the lore
     *
     * @return The lore
     */
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    /**
     * Get the HeadDatabase ID
     *
     * @return The HeadDatabase ID
     */
    public String getHdbId() {
        return hdbId;
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
        result.put("id", id);
        result.put("pouch_id", pouchId);
        result.put("name", name);
        result.put("lore", lore);
        result.put("hdb_id", hdbId);
        result.put("cost", cost);
        return result;
    }
}