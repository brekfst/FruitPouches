package com.brekfst.fruitPouches.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents statistics for a pouch
 */
public class PouchStats {

    private final String pouchId;
    private int itemsCollected;
    private int actionsPerformed;
    private long lastUsed;

    /**
     * Create new pouch stats
     *
     * @param pouchId The pouch ID
     */
    public PouchStats(String pouchId) {
        this.pouchId = pouchId;
        this.itemsCollected = 0;
        this.actionsPerformed = 0;
        this.lastUsed = System.currentTimeMillis();
    }

    /**
     * Create pouch stats from serialized data
     *
     * @param pouchId The pouch ID
     * @param data The serialized data
     */
    private PouchStats(String pouchId, Map<String, Object> data) {
        this.pouchId = pouchId;
        this.itemsCollected = (int) data.getOrDefault("items_collected", 0);
        this.actionsPerformed = (int) data.getOrDefault("actions_performed", 0);
        this.lastUsed = Long.parseLong(data.getOrDefault("last_used", System.currentTimeMillis()).toString());
    }

    /**
     * Get the pouch ID
     *
     * @return The pouch ID
     */
    public String getPouchId() {
        return pouchId;
    }

    /**
     * Get the number of items collected
     *
     * @return The number of items collected
     */
    public int getItemsCollected() {
        return itemsCollected;
    }

    /**
     * Increment the number of items collected
     *
     * @param amount The amount to increment by
     */
    public void incrementItemsCollected(int amount) {
        this.itemsCollected += amount;
    }

    /**
     * Get the number of actions performed
     *
     * @return The number of actions performed
     */
    public int getActionsPerformed() {
        return actionsPerformed;
    }

    /**
     * Increment the number of actions performed
     */
    public void incrementActionsPerformed() {
        this.actionsPerformed++;
    }

    /**
     * Get the last used timestamp
     *
     * @return The last used timestamp
     */
    public long getLastUsed() {
        return lastUsed;
    }

    /**
     * Get the last used timestamp as a formatted string
     *
     * @return The last used timestamp as a formatted string
     */
    public String getLastUsedFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(lastUsed));
    }

    /**
     * Update the last used timestamp to now
     */
    public void updateLastUsed() {
        this.lastUsed = System.currentTimeMillis();
    }

    /**
     * Convert to a map representation
     *
     * @return Map representation
     */
    public Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("items_collected", itemsCollected);
        result.put("actions_performed", actionsPerformed);
        result.put("last_used", lastUsed);
        return result;
    }

    /**
     * Create pouch stats from serialized data
     *
     * @param pouchId The pouch ID
     * @param data The serialized data
     * @return The pouch stats
     */
    public static PouchStats deserialize(String pouchId, Map<String, Object> data) {
        return new PouchStats(pouchId, data);
    }
}