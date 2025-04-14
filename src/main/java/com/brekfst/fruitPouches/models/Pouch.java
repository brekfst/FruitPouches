package com.brekfst.fruitPouches.models;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a pouch that can store items
 */
public class Pouch {

    // Constants for NBT tags
    private static final String NBT_POUCH_ID = "pouch_id";
    private static final String NBT_POUCH_LEVEL = "pouch_level";
    private static final String NBT_POUCH_SKIN = "pouch_skin";
    private static final String NBT_POUCH_ITEMS = "pouch_items";

    private final String id;
    private final String displayName;
    private final Material material;
    private final String hdbId;
    private int slots;
    private int enchantmentSlots;
    private final Set<String> pickupItems;
    private final Set<String> excludeItems;
    private final List<Map<String, String>> pickupConditions;
    private final String guiLayout;
    private final List<Map<String, Object>> guiCategories;
    private final List<Map<String, Object>> guiButtons;
    private final String overflowMode;
    private final List<PouchAction> actions;
    private final boolean trackStats;
    private final List<PouchUpgrade> upgrades;
    private final Map<String, String> permissions;

    // Runtime data (not saved to config)
    private int currentLevel;
    private String currentSkin;
    private List<PouchEnchantment> enchantments;
    private Map<String, ItemStack> contents;
    private PouchStats stats;

    /**
     * Create a new pouch from configuration
     *
     * @param id The unique identifier
     * @param config The configuration section
     */
    public Pouch(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = ChatColor.translateAlternateColorCodes('&', config.getString("display", id));
        this.material = Material.valueOf(config.getString("material", "SHULKER_BOX"));
        this.hdbId = config.getString("hdb_id", "");
        this.slots = config.getInt("slots", 27);
        this.enchantmentSlots = config.getInt("enchantment_slots", 0);

        // Pickup settings
        this.pickupItems = new HashSet<>(config.getStringList("pickup.items"));
        this.excludeItems = new HashSet<>(config.getStringList("pickup.exclude"));

        // Pickup conditions
        this.pickupConditions = new ArrayList<>();
        List<Map<?, ?>> conditions = config.getMapList("pickup.conditions");
        for (Map<?, ?> condition : conditions) {
            Map<String, String> conditionMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : condition.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    conditionMap.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            pickupConditions.add(conditionMap);
        }

        // GUI settings
        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        if (guiSection != null) {
            this.guiLayout = guiSection.getString("layout", "simple");

            // Parse categories
            this.guiCategories = new ArrayList<>();
            List<Map<?, ?>> categories = guiSection.getMapList("categories");
            for (Map<?, ?> category : categories) {
                Map<String, Object> categoryMap = new HashMap<>();
                for (Map.Entry<?, ?> entry : category.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        categoryMap.put(entry.getKey().toString(), entry.getValue());
                    }
                }
                guiCategories.add(categoryMap);
            }

            // Parse buttons
            this.guiButtons = new ArrayList<>();
            List<Map<?, ?>> buttons = guiSection.getMapList("buttons");
            for (Map<?, ?> button : buttons) {
                Map<String, Object> buttonMap = new HashMap<>();
                for (Map.Entry<?, ?> entry : button.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        buttonMap.put(entry.getKey().toString(), entry.getValue());
                    }
                }
                guiButtons.add(buttonMap);
            }
        } else {
            this.guiLayout = "simple";
            this.guiCategories = new ArrayList<>();
            this.guiButtons = new ArrayList<>();
        }

        // Overflow handling
        this.overflowMode = config.getString("overflow", "inventory");

        // Actions
        this.actions = new ArrayList<>();
        List<Map<?, ?>> actionsList = config.getMapList("actions");
        for (Map<?, ?> actionMap : actionsList) {
            for (Map.Entry<?, ?> entry : actionMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() instanceof Map) {
                    String actionType = entry.getKey().toString();
                    Map<?, ?> actionConfig = (Map<?, ?>) entry.getValue();

                    Map<String, Object> configMap = new HashMap<>();
                    for (Map.Entry<?, ?> configEntry : actionConfig.entrySet()) {
                        if (configEntry.getKey() != null && configEntry.getValue() != null) {
                            configMap.put(configEntry.getKey().toString(), configEntry.getValue());
                        }
                    }

                    actions.add(new PouchAction(actionType, configMap));
                }
            }
        }

        // Stats tracking
        this.trackStats = config.getBoolean("stats", false);

        // Upgrades
        this.upgrades = new ArrayList<>();
        List<Map<?, ?>> upgradesList = config.getMapList("upgrades");
        for (Map<?, ?> upgradeMap : upgradesList) {
            for (Map.Entry<?, ?> entry : upgradeMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    int level;
                    try {
                        level = Integer.parseInt(entry.getKey().toString().replace("level", ""));
                    } catch (NumberFormatException e) {
                        // If the key doesn't follow the "levelX" format, use the map's level property
                        Map<?, ?> upgradeConfig = (Map<?, ?>) entry.getValue();
                        level = upgradeConfig.containsKey("level") ?
                                Integer.parseInt(upgradeConfig.get("level").toString()) : upgrades.size() + 1;
                    }

                    Map<?, ?> upgradeConfig = (Map<?, ?>) entry.getValue();
                    int upgradeSlots = upgradeConfig.containsKey("slots") ?
                            Integer.parseInt(upgradeConfig.get("slots").toString()) : slots;
                    int upgradeEnchantmentSlots = upgradeConfig.containsKey("enchantment_slots") ?
                            Integer.parseInt(upgradeConfig.get("enchantment_slots").toString()) : enchantmentSlots;

                    Map<String, Object> costMap = upgradeConfig.containsKey("cost") ?
                            convertToStringObjectMap((Map<?, ?>) upgradeConfig.get("cost")) : new HashMap<>();

                    upgrades.add(new PouchUpgrade(level, upgradeSlots, upgradeEnchantmentSlots, costMap));
                }
            }
        }

        // Sort upgrades by level
        upgrades.sort(Comparator.comparingInt(PouchUpgrade::getLevel));

        // Permissions
        this.permissions = new HashMap<>();
        ConfigurationSection permissionSection = config.getConfigurationSection("permissions");
        if (permissionSection != null) {
            for (String key : permissionSection.getKeys(false)) {
                permissions.put(key, permissionSection.getString(key));
            }
        }

        // Initialize runtime data
        this.currentLevel = 0; // Level 0 means no upgrades applied
        this.currentSkin = "";
        this.enchantments = new ArrayList<>();
        this.contents = new HashMap<>();
        this.stats = new PouchStats(id);

        // Apply level 0 defaults
        applyLevelDefaults();
    }

    /**
     * Apply the current level's defaults (slots, enchantment slots)
     */
    private void applyLevelDefaults() {
        if (currentLevel > 0 && currentLevel <= upgrades.size()) {
            PouchUpgrade upgrade = upgrades.get(currentLevel - 1);
            this.slots = upgrade.getSlots();
            this.enchantmentSlots = upgrade.getEnchantmentSlots();
        }
    }

    /**
     * Convert a Map<?, ?> to Map<String, Object>
     *
     * @param map The map to convert
     * @return Converted map
     */
    private Map<String, Object> convertToStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Create an ItemStack representing this pouch
     *
     * @param plugin The plugin instance
     * @return ItemStack representing this pouch
     */
    public ItemStack toItemStack(FruitPouches plugin) {
        ItemStack item;

        // Check if HeadDatabase ID is available and valid
        if (!hdbId.isEmpty() && plugin.getHeadDatabaseHook().isEnabled()) {
            item = plugin.getHeadDatabaseHook().getHeadFromID(hdbId);
            if (item == null) {
                item = new ItemStack(material);
                plugin.getDebug().log("Invalid HeadDatabase ID: " + hdbId + " for pouch: " + id);
            }
        } else {
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set display name
            meta.setDisplayName(displayName);

            // Set lore
            List<String> loreList = new ArrayList<>();
            loreList.add(ChatColor.GRAY + "Type: " + ChatColor.GOLD + id);

            // Add level info
            if (currentLevel > 0) {
                loreList.add(ChatColor.GRAY + "Level: " + ChatColor.GREEN + currentLevel);
            }

            // Add slots info
            loreList.add(ChatColor.GRAY + "Slots: " + ChatColor.AQUA + slots);

            // Add enchantments info
            if (!enchantments.isEmpty()) {
                loreList.add(ChatColor.GRAY + "Enchantments:");
                for (PouchEnchantment enchantment : enchantments) {
                    loreList.add(ChatColor.GRAY + " - " + ChatColor.LIGHT_PURPLE +
                            enchantment.getType().getDisplay() + " " +
                            toRomanNumeral(enchantment.getLevel()));
                }
            }

            // Add stats info if tracking is enabled
            if (trackStats) {
                loreList.add("");
                loreList.add(ChatColor.GRAY + "Items Collected: " + ChatColor.YELLOW + stats.getItemsCollected());
                loreList.add(ChatColor.GRAY + "Last Used: " + ChatColor.YELLOW + stats.getLastUsedFormatted());
            }

            // Add usage instructions
            loreList.add("");
            loreList.add(ChatColor.YELLOW + "Right-click" + ChatColor.GRAY + " to open");

            meta.setLore(loreList);

            // Add NBT data
            NamespacedKey pouchIdKey = new NamespacedKey(plugin, NBT_POUCH_ID);
            NamespacedKey pouchLevelKey = new NamespacedKey(plugin, NBT_POUCH_LEVEL);
            NamespacedKey pouchSkinKey = new NamespacedKey(plugin, NBT_POUCH_SKIN);

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(pouchIdKey, PersistentDataType.STRING, id);
            container.set(pouchLevelKey, PersistentDataType.INTEGER, currentLevel);
            container.set(pouchSkinKey, PersistentDataType.STRING, currentSkin);

            // We don't store the contents in NBT because it could be too large
            // Contents are stored in the player data files

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Check if an ItemStack is a pouch of this type
     *
     * @param plugin The plugin instance
     * @param item The ItemStack to check
     * @return true if the ItemStack is a pouch of this type
     */
    public static boolean isPouchOfType(FruitPouches plugin, ItemStack item, String pouchId) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey pouchIdKey = new NamespacedKey(plugin, NBT_POUCH_ID);

        if (container.has(pouchIdKey, PersistentDataType.STRING)) {
            String storedId = container.get(pouchIdKey, PersistentDataType.STRING);
            return pouchId.equals(storedId);
        }

        return false;
    }

    /**
     * Load a pouch from an ItemStack
     *
     * @param plugin The plugin instance
     * @param item The ItemStack to load from
     * @return The pouch ID or null if the item is not a pouch
     */
    public static String getPouchIdFromItem(FruitPouches plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey pouchIdKey = new NamespacedKey(plugin, NBT_POUCH_ID);

        if (container.has(pouchIdKey, PersistentDataType.STRING)) {
            return container.get(pouchIdKey, PersistentDataType.STRING);
        }

        return null;
    }

    /**
     * Get the level from an ItemStack
     *
     * @param plugin The plugin instance
     * @param item The ItemStack to check
     * @return The pouch level or 0 if the item is not a pouch
     */
    public static int getLevelFromItem(FruitPouches plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey pouchLevelKey = new NamespacedKey(plugin, NBT_POUCH_LEVEL);

        if (container.has(pouchLevelKey, PersistentDataType.INTEGER)) {
            return container.get(pouchLevelKey, PersistentDataType.INTEGER);
        }

        return 0;
    }

    /**
     * Get the skin from an ItemStack
     *
     * @param plugin The plugin instance
     * @param item The ItemStack to check
     * @return The pouch skin or empty string if the item is not a pouch
     */
    public static String getSkinFromItem(FruitPouches plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey pouchSkinKey = new NamespacedKey(plugin, NBT_POUCH_SKIN);

        if (container.has(pouchSkinKey, PersistentDataType.STRING)) {
            return container.get(pouchSkinKey, PersistentDataType.STRING);
        }

        return "";
    }

    /**
     * Check if a player has permission to use this pouch
     *
     * @param player The player to check
     * @return true if the player has permission
     */
    public boolean hasUsePermission(Player player) {
        String permission = permissions.getOrDefault("use", "fruitpouch." + id + ".use");
        return player.hasPermission(permission) || player.hasPermission("fruitpouch.admin");
    }

    /**
     * Check if a player has permission to upgrade this pouch
     *
     * @param player The player to check
     * @return true if the player has permission
     */
    public boolean hasUpgradePermission(Player player) {
        String permission = permissions.getOrDefault("upgrade", "fruitpouch." + id + ".upgrade");
        return player.hasPermission(permission) || player.hasPermission("fruitpouch.admin");
    }

    /**
     * Check if an item can be picked up by this pouch
     *
     * @param plugin The plugin instance
     * @param itemStack The item to check
     * @return true if the item can be picked up
     */
    public boolean canPickup(FruitPouches plugin, ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        String materialName = itemStack.getType().name();

        // Debug logging to troubleshoot item pickup issues
        plugin.getDebug().log("Checking if pouch " + id + " can pick up item: " + materialName);
        plugin.getDebug().log("Exclude list: " + String.join(", ", excludeItems));
        plugin.getDebug().log("Pickup items list: " + String.join(", ", pickupItems));

        // Check exclusions first
        if (excludeItems.contains(materialName)) {
            plugin.getDebug().log("Item is excluded: " + materialName);
            return false;
        }

        // Check if this is a vanilla item in the pickup list
        if (pickupItems.contains(materialName)) {
            plugin.getDebug().log("Item is in pickup list: " + materialName);
            return true;
        }

        // Check if this is a custom item in the pickup list
        for (String pickupItem : pickupItems) {
            if (pickupItem.startsWith("custom:")) {
                String customItemId = pickupItem.substring(7);
                CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customItemId);

                if (customItem != null && customItem.matches(itemStack)) {
                    plugin.getDebug().log("Item matches custom item: " + customItemId);
                    return true;
                }
            }
        }

        // Check for wildcard patterns (like *_WOOL or CONCRETE*)
        for (String pickupItem : pickupItems) {
            if (pickupItem.contains("*")) {
                String pattern = pickupItem.replace("*", "");
                if ((pattern.startsWith("_") && materialName.endsWith(pattern)) ||
                        (pattern.endsWith("_") && materialName.startsWith(pattern))) {
                    plugin.getDebug().log("Item matches wildcard pattern: " + pickupItem);
                    return true;
                }
            }
        }

        plugin.getDebug().log("Item does not match any pickup criteria: " + materialName);
        return false;
    }

    /**
     * Check if pickup conditions are met for a player
     *
     * @param player The player to check
     * @return true if the conditions are met
     */
    public boolean meetsPickupConditions(Player player) {
        for (Map<String, String> condition : pickupConditions) {
            // Check world condition
            if (condition.containsKey("world") && !player.getWorld().getName().equals(condition.get("world"))) {
                return false;
            }

            // Check permission condition
            if (condition.containsKey("permission") && !player.hasPermission(condition.get("permission"))) {
                return false;
            }

            // Check time condition
            if (condition.containsKey("time")) {
                String timeCondition = condition.get("time").toLowerCase();
                long time = player.getWorld().getTime();

                if (timeCondition.equals("day") && (time < 0 || time > 12000)) {
                    return false;
                } else if (timeCondition.equals("night") && (time >= 0 && time <= 12000)) {
                    return false;
                }
            }
        }

        return true;
    }

    // In the Pouch.java class
    /**
     * Add an item to this pouch
     *
     * @param itemStack The item to add
     * @return true if the item was added, false if the pouch is full
     */
    public boolean addItem(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        // Create a copy to avoid modifying the original
        ItemStack itemToAdd = itemStack.clone();
        int initialAmount = itemToAdd.getAmount();
        int remainingAmount = initialAmount;

        // First pass: try to stack with existing similar items
        for (Map.Entry<String, ItemStack> entry : contents.entrySet()) {
            String key = entry.getKey();
            ItemStack existingItem = entry.getValue();

            if (existingItem.isSimilar(itemToAdd)) {
                int maxStackSize = existingItem.getMaxStackSize();
                int currentAmount = existingItem.getAmount();
                int spaceAvailable = maxStackSize - currentAmount;

                if (spaceAvailable > 0) {
                    int amountToAdd = Math.min(remainingAmount, spaceAvailable);
                    existingItem.setAmount(currentAmount + amountToAdd);
                    remainingAmount -= amountToAdd;

                    if (remainingAmount <= 0) {
                        // Successfully added the entire stack
                        stats.incrementItemsCollected(initialAmount);
                        stats.updateLastUsed();
                        return true;
                    }
                }
            }
        }

        // If we still have items to add, check if we have space for a new stack
        if (remainingAmount > 0) {
            // Check available slots
            if (contents.size() >= slots) {
                // No space for a new stack - return false if we couldn't add anything
                if (remainingAmount == initialAmount) {
                    return false;
                }

                // Otherwise, return true because we added at least some items
                stats.incrementItemsCollected(initialAmount - remainingAmount);
                stats.updateLastUsed();
                return true;
            }

            // Create a new stack with the remaining items
            itemToAdd.setAmount(remainingAmount);
            String newKey = UUID.randomUUID().toString();
            contents.put(newKey, itemToAdd);
        }

        stats.incrementItemsCollected(initialAmount);
        stats.updateLastUsed();
        return true;
    }

    /**
     * Generate a unique key for an item
     *
     * @param itemStack The item to generate a key for
     * @return A unique key
     */
    private String generateItemKey(ItemStack itemStack) {
        return UUID.randomUUID().toString();
    }

    /**
     * Convert an integer to a Roman numeral
     *
     * @param num The integer to convert
     * @return Roman numeral representation
     */
    private String toRomanNumeral(int num) {
        if (num <= 0 || num > 10) {
            return String.valueOf(num);
        }

        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[num - 1];
    }

    /**
     * Save this pouch to a YAML configuration
     *
     * @param config The YAML configuration to save to
     */
    public void saveToConfig(ConfigurationSection config) {
        // Convert enchantments to a list of maps
        List<Map<String, Object>> enchantmentsList = new ArrayList<>();
        for (PouchEnchantment enchantment : enchantments) {
            Map<String, Object> enchantmentMap = new HashMap<>();
            enchantmentMap.put("type", enchantment.getType().getId());
            enchantmentMap.put("level", enchantment.getLevel());
            enchantmentsList.add(enchantmentMap);
        }

        // Convert contents to a list of serialized items
        Map<String, Object> serializedContents = new HashMap<>();
        for (Map.Entry<String, ItemStack> entry : contents.entrySet()) {
            serializedContents.put(entry.getKey(), entry.getValue().serialize());
        }

        // Save the basic data
        config.set("level", currentLevel);
        config.set("skin", currentSkin);
        config.set("enchantments", enchantmentsList);
        config.set("contents", serializedContents);
        config.set("stats", stats.serialize());
    }

    /**
     * Load this pouch from a YAML configuration
     *
     * @param plugin The plugin instance
     * @param config The YAML configuration to load from
     */
    public void loadFromConfig(FruitPouches plugin, ConfigurationSection config) {
        this.currentLevel = config.getInt("level", 0);
        this.currentSkin = config.getString("skin", "");

        // Apply level defaults
        applyLevelDefaults();

        // Load enchantments
        List<Map<?, ?>> enchantmentsList = config.getMapList("enchantments");
        this.enchantments = new ArrayList<>();
        for (Map<?, ?> enchantmentMap : enchantmentsList) {
            String typeId = enchantmentMap.get("type").toString();
            int level = Integer.parseInt(enchantmentMap.get("level").toString());

            PouchEnchantment.Type enchantmentType = plugin.getEnchantmentManager().getEnchantmentType(typeId);
            if (enchantmentType != null) {
                this.enchantments.add(new PouchEnchantment(enchantmentType, level));
            }
        }

        // Load contents
        this.contents = new HashMap<>();
        ConfigurationSection contentsSection = config.getConfigurationSection("contents");
        if (contentsSection != null) {
            for (String key : contentsSection.getKeys(false)) {
                Map<String, Object> serialized = contentsSection.getConfigurationSection(key).getValues(true);
                ItemStack item = ItemStack.deserialize(serialized);
                this.contents.put(key, item);
            }
        }

        // Load stats
        ConfigurationSection statsSection = config.getConfigurationSection("stats");
        if (statsSection != null) {
            this.stats = PouchStats.deserialize(id, statsSection.getValues(true));
        } else {
            this.stats = new PouchStats(id);
        }
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getHdbId() {
        return hdbId;
    }

    public int getSlots() {
        return slots;
    }

    public int getEnchantmentSlots() {
        return enchantmentSlots;
    }

    public Set<String> getPickupItems() {
        return new HashSet<>(pickupItems);
    }

    public Set<String> getExcludeItems() {
        return new HashSet<>(excludeItems);
    }

    public List<Map<String, String>> getPickupConditions() {
        return new ArrayList<>(pickupConditions);
    }

    public String getGuiLayout() {
        return guiLayout;
    }

    public List<Map<String, Object>> getGuiCategories() {
        return new ArrayList<>(guiCategories);
    }

    public List<Map<String, Object>> getGuiButtons() {
        return new ArrayList<>(guiButtons);
    }

    public String getOverflowMode() {
        return overflowMode;
    }

    public List<PouchAction> getActions() {
        return new ArrayList<>(actions);
    }

    public boolean isTrackStats() {
        return trackStats;
    }

    public List<PouchUpgrade> getUpgrades() {
        return new ArrayList<>(upgrades);
    }

    public Map<String, String> getPermissions() {
        return new HashMap<>(permissions);
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
        applyLevelDefaults();
    }

    public String getCurrentSkin() {
        return currentSkin;
    }

    public void setCurrentSkin(String currentSkin) {
        this.currentSkin = currentSkin;
    }

    public List<PouchEnchantment> getEnchantments() {
        return new ArrayList<>(enchantments);
    }

    public void setEnchantments(List<PouchEnchantment> enchantments) {
        this.enchantments = new ArrayList<>(enchantments);
    }

    public Map<String, ItemStack> getContents() {
        return new HashMap<>(this.contents);
    }

    public void setContents(Map<String, ItemStack> contents) {
        this.contents = new HashMap<>(contents);
    }

    public PouchStats getStats() {
        return stats;
    }

    public void setStats(PouchStats stats) {
        this.stats = stats;
    }
}