package com.brekfst.fruitPouches.gui;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.CustomItem;
import com.brekfst.fruitPouches.models.Pouch;
import com.brekfst.fruitPouches.models.PouchAction;
import com.brekfst.fruitPouches.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced GUI for pouches with organized categories and visual improvements
 */
public class PouchGUI implements Listener {

    private final FruitPouches plugin;
    private final Player player;
    private final Pouch pouch;
    private final Inventory inventory;
    private final Map<Integer, String> itemSlots = new HashMap<>();
    private final Map<Integer, String> buttonActions = new HashMap<>();
    private String currentCategory = "all";
    private int currentPage = 0;

    // Constants for GUI layout
    private static final int INVENTORY_SIZE = 54; // 6 rows of slots
    private static final int CATEGORY_ROW = 0;    // First row for categories
    private static final int CONTENT_START_ROW = 1;  // Content starts from second row
    private static final int CONTENT_END_ROW = 4;    // Content ends at fifth row
    private static final int NAVIGATION_ROW = 5;     // Bottom row for navigation/actions

    // Colors for GUI elements
    private static final ChatColor PRIMARY_COLOR = ChatColor.GOLD;
    private static final ChatColor SECONDARY_COLOR = ChatColor.YELLOW;
    private static final ChatColor TEXT_COLOR = ChatColor.GRAY;

    /**
     * Create a new pouch GUI
     *
     * @param plugin The plugin instance
     * @param player The player
     * @param pouch  The pouch
     */
    public PouchGUI(FruitPouches plugin, Player player, Pouch pouch) {
        this.plugin = plugin;
        this.player = player;
        this.pouch = pouch;

        // Create a consistent 6-row inventory for better organization
        String title = PRIMARY_COLOR + "✦ " +
                ChatColor.translateAlternateColorCodes('&', pouch.getDisplayName()) +
                PRIMARY_COLOR + " ✦";
        this.inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        // Register this listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Initialize the GUI
        initializeGUI();
    }

    /**
     * Initialize the GUI with proper organization
     */
    private void initializeGUI() {
        // Clear the inventory
        inventory.clear();
        itemSlots.clear();
        buttonActions.clear();

        // Get the pouch contents
        Map<String, ItemStack> contents = pouch.getContents();

        // Add decorative border
        addBorder();

        // Get the pouch layout
        String layout = pouch.getGuiLayout();

        switch (layout) {
            case "categorized":
                initializeCategorizedLayout(contents);
                break;
            case "paged":
                initializePagedLayout(contents);
                break;
            case "simple":
            default:
                initializeSimpleLayout(contents);
                break;
        }

        // Add pouch info
        addPouchInfo();

        // Add action buttons
        addActionButtons();
    }

    /**
     * Add a decorative border to the GUI
     */
    private void addBorder() {
        ItemStack borderItem = ItemUtils.createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                " ",
                Collections.emptyList()
        );

        // Add top and bottom borders (skipping category and action buttons)
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 8) {
                inventory.setItem(i, borderItem); // Top corners only
            }

            // Bottom row corners
            if (i == 45 || i == 53) {
                inventory.setItem(i, borderItem);
            }
        }

        // Add side borders
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, borderItem);      // Left border
            inventory.setItem(row * 9 + 8, borderItem);  // Right border
        }
    }

    private void initializeSimpleLayout(Map<String, ItemStack> contents) {
        // Create a map of all possible items this pouch can collect
        Map<String, ItemStack> possibleItems = new HashMap<>();

        // Add all possible items with zero count initially
        for (String pickupItem : pouch.getPickupItems()) {
            if (pickupItem.startsWith("custom:")) {
                String customId = pickupItem.substring(7);
                CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customId);
                if (customItem != null) {
                    ItemStack item = customItem.toItemStack();
                    item.setAmount(0);
                    possibleItems.put(getItemIdentifier(item), item);
                }
            } else {
                try {
                    Material material = Material.valueOf(pickupItem);
                    ItemStack item = new ItemStack(material);
                    item.setAmount(0);
                    possibleItems.put(getItemIdentifier(item), item);
                } catch (IllegalArgumentException e) {
                    // Handle wildcards (like *_ORE) by finding all matching materials
                    if (pickupItem.contains("*")) {
                        String pattern = pickupItem.replace("*", "");
                        for (Material material : Material.values()) {
                            String materialName = material.name();
                            if ((pickupItem.startsWith("*") && materialName.endsWith(pattern)) ||
                                    (pickupItem.endsWith("*") && materialName.startsWith(pattern)) ||
                                    (pickupItem.startsWith("*") && pickupItem.endsWith("*") && materialName.contains(pattern))) {
                                ItemStack item = new ItemStack(material);
                                item.setAmount(0);
                                possibleItems.put(getItemIdentifier(item), item);
                            }
                        }
                    }
                }
            }
        }

        // Update quantities from actual contents
        for (Map.Entry<String, ItemStack> entry : contents.entrySet()) {
            ItemStack item = entry.getValue();
            String itemType = getItemIdentifier(item);

            if (possibleItems.containsKey(itemType)) {
                // Add to existing item
                ItemStack existingItem = possibleItems.get(itemType);
                existingItem.setAmount(existingItem.getAmount() + item.getAmount());
            } else {
                // This shouldn't happen often but handle it just in case
                possibleItems.put(itemType, item.clone());
            }
        }

        // Now display all possible items
        if (possibleItems.isEmpty()) {
            // Show empty message
            ItemStack emptyItem = ItemUtils.createItem(
                    Material.BARRIER,
                    SECONDARY_COLOR + "Empty Pouch",
                    Arrays.asList(
                            TEXT_COLOR + "This pouch is empty.",
                            TEXT_COLOR + "Collect items to fill it up!"
                    )
            );
            inventory.setItem(22, emptyItem); // Center of the inventory
            return;
        }

        // Add items to GUI
        int slot = 10; // Start after border, in the second row
        for (Map.Entry<String, ItemStack> entry : possibleItems.entrySet()) {
            ItemStack item = entry.getValue();

            // Generate a unique key for this display item
            String key = generateDisplayKey(item);

            // Add to GUI
            addItemToGUI(slot, item, key);

            // Move to next slot, skipping borders
            slot++;
            if (slot % 9 == 8) { // Right border
                slot += 2;
            }

            if (slot >= 45) { // Bottom row
                break;
            }
        }
    }

    // Helper method to generate a display key
    private String generateDisplayKey(ItemStack item) {
        return item.getType().name() + "_display";
    }

    /**
     * Initialize a categorized layout with tabs
     *
     * @param contents The pouch contents
     */
    private void initializeCategorizedLayout(Map<String, ItemStack> contents) {
        // Set up category buttons in the top row
        List<Map<String, Object>> categories = pouch.getGuiCategories();

        if (categories.isEmpty()) {
            // Fall back to simple layout if no categories are defined
            initializeSimpleLayout(contents);
            return;
        }

        // Add "All Items" category
        ItemStack allItemsButton = ItemUtils.createItem(
                Material.CHEST,
                SECONDARY_COLOR + "All Items",
                Collections.singletonList(TEXT_COLOR + "View all items")
        );
        inventory.setItem(1, allItemsButton);
        buttonActions.put(1, "category:all");

        // Add custom category buttons
        for (int i = 0; i < Math.min(categories.size(), 6); i++) {
            Map<String, Object> category = categories.get(i);
            String name = (String) category.get("name");
            String filter = (String) category.get("filter");
            String icon = (String) category.getOrDefault("icon", "WHITE_WOOL");

            if (name == null || filter == null) {
                continue;
            }

            // Create the category button
            Material iconMaterial;
            try {
                iconMaterial = Material.valueOf(icon);
            } catch (IllegalArgumentException e) {
                iconMaterial = Material.WHITE_WOOL;
            }

            // Highlight currently selected category
            boolean isSelected = filter.equals(currentCategory);
            String displayName = (isSelected ? PRIMARY_COLOR + "► " : SECONDARY_COLOR) +
                    ChatColor.translateAlternateColorCodes('&', name);

            ItemStack button = ItemUtils.createItem(
                    iconMaterial,
                    displayName,
                    Collections.singletonList(TEXT_COLOR + "Click to view category")
            );

            inventory.setItem(i + 2, button); // Start at slot 2 to center categories
            buttonActions.put(i + 2, "category:" + filter);
        }

        // Filter items by current category
        Map<String, ItemStack> filteredContents;
        if (currentCategory.equals("all")) {
            filteredContents = contents;
        } else {
            filteredContents = filterItemsByCategory(contents, currentCategory);
        }

        // Show the filtered items
        if (filteredContents.isEmpty()) {
            // Show empty category message
            ItemStack emptyItem = ItemUtils.createItem(
                    Material.BARRIER,
                    SECONDARY_COLOR + "Empty Category",
                    Arrays.asList(
                            TEXT_COLOR + "No items in this category.",
                            TEXT_COLOR + "Collect more items!"
                    )
            );
            inventory.setItem(31, emptyItem); // Center of content area
        } else {
            // Group similar items
            Map<String, Integer> itemCounts = new HashMap<>();
            Map<String, ItemStack> uniqueItems = new HashMap<>();
            Map<String, String> itemKeys = new HashMap<>(); // Store the original key

            for (Map.Entry<String, ItemStack> entry : filteredContents.entrySet()) {
                ItemStack item = entry.getValue();
                String key = entry.getKey();
                String itemType = getItemIdentifier(item);

                if (!itemCounts.containsKey(itemType)) {
                    itemCounts.put(itemType, item.getAmount());
                    uniqueItems.put(itemType, item.clone());
                    itemKeys.put(itemType, key); // Remember the first key for this type
                } else {
                    itemCounts.put(itemType, itemCounts.get(itemType) + item.getAmount());
                }
            }

            // Add items to GUI
            int slot = 10; // Start after border, in the second row
            for (String itemType : uniqueItems.keySet()) {
                ItemStack item = uniqueItems.get(itemType);
                String key = itemKeys.get(itemType);
                int count = itemCounts.get(itemType);

                // Update item amount
                item.setAmount(count);

                // Add to GUI with unified display
                addItemToGUI(slot, item, key);

                // Move to next slot, skipping borders
                slot++;
                if (slot % 9 == 8) { // Right border
                    slot += 2;
                }

                if (slot >= 45) { // Bottom row
                    break;
                }
            }
        }
    }

    /**
     * Filter items by category
     *
     * @param contents The full contents
     * @param category The category to filter by
     * @return Filtered contents
     */
    private Map<String, ItemStack> filterItemsByCategory(Map<String, ItemStack> contents, String category) {
        Map<String, ItemStack> filtered = new HashMap<>();

        if (category.equals("all")) {
            return contents;
        }

        for (Map.Entry<String, ItemStack> entry : contents.entrySet()) {
            ItemStack item = entry.getValue();
            String materialName = item.getType().name();

            if (category.startsWith("custom:")) {
                String customId = category.substring(7);
                CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customId);

                if (customItem != null && customItem.matches(item)) {
                    filtered.put(entry.getKey(), item);
                }
            } else if (category.contains("*")) {
                // Wildcard matching for categories like *_WOOL or CONCRETE*
                String pattern = category.replace("*", "");

                if (category.startsWith("*_") && materialName.endsWith(pattern)) {
                    filtered.put(entry.getKey(), item);
                } else if (category.endsWith("_*") && materialName.startsWith(pattern)) {
                    filtered.put(entry.getKey(), item);
                } else if (materialName.contains(pattern)) {
                    filtered.put(entry.getKey(), item);
                }
            } else if (category.equals(materialName)) {
                filtered.put(entry.getKey(), item);
            }
        }

        return filtered;
    }

    /**
     * Initialize a paged layout for large collections
     *
     * @param contents The pouch contents
     */
    private void initializePagedLayout(Map<String, ItemStack> contents) {
        // Add title for this layout
        ItemStack titleItem = ItemUtils.createItem(
                Material.BOOK,
                PRIMARY_COLOR + "Page " + (currentPage + 1),
                Collections.singletonList(TEXT_COLOR + "Item Collection")
        );
        inventory.setItem(4, titleItem);

        if (contents.isEmpty()) {
            // Show empty message
            ItemStack emptyItem = ItemUtils.createItem(
                    Material.BARRIER,
                    SECONDARY_COLOR + "Empty Pouch",
                    Arrays.asList(
                            TEXT_COLOR + "This pouch is empty.",
                            TEXT_COLOR + "Collect items to fill it up!"
                    )
            );
            inventory.setItem(31, emptyItem); // Center of content area
            return;
        }

        // Group similar items
        Map<String, Integer> itemCounts = new HashMap<>();
        Map<String, ItemStack> uniqueItems = new HashMap<>();
        Map<String, String> itemKeys = new HashMap<>(); // Store the original key

        for (Map.Entry<String, ItemStack> entry : contents.entrySet()) {
            ItemStack item = entry.getValue();
            String key = entry.getKey();
            String itemType = getItemIdentifier(item);

            if (!itemCounts.containsKey(itemType)) {
                itemCounts.put(itemType, item.getAmount());
                uniqueItems.put(itemType, item.clone());
                itemKeys.put(itemType, key); // Remember the first key for this type
            } else {
                itemCounts.put(itemType, itemCounts.get(itemType) + item.getAmount());
            }
        }

        // Create list of items
        List<Map.Entry<String, ItemStack>> itemList = new ArrayList<>();
        for (String itemType : uniqueItems.keySet()) {
            ItemStack item = uniqueItems.get(itemType);
            String key = itemKeys.get(itemType);
            int count = itemCounts.get(itemType);

            // Update item amount
            item.setAmount(count);

            itemList.add(Map.entry(key, item));
        }

        // Calculate pages
        int itemsPerPage = 21; // 3 rows of 7 slots
        int totalPages = (int) Math.ceil(itemList.size() / (double) itemsPerPage);

        // Ensure current page is valid
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        // Add page navigation
        if (totalPages > 1) {
            // Previous page button
            if (currentPage > 0) {
                ItemStack prevButton = ItemUtils.createItem(
                        Material.ARROW,
                        SECONDARY_COLOR + "Previous Page",
                        Collections.singletonList(TEXT_COLOR + "Page " + currentPage + " of " + totalPages)
                );
                inventory.setItem(45, prevButton);
                buttonActions.put(45, "page:prev");
            }

            // Next page button
            if (currentPage < totalPages - 1) {
                ItemStack nextButton = ItemUtils.createItem(
                        Material.ARROW,
                        SECONDARY_COLOR + "Next Page",
                        Collections.singletonList(TEXT_COLOR + "Page " + (currentPage + 2) + " of " + totalPages)
                );
                inventory.setItem(53, nextButton);
                buttonActions.put(53, "page:next");
            }
        }

        // Add items for the current page
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, itemList.size());

        // Fill the content area with items
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, ItemStack> entry = itemList.get(i);
            ItemStack item = entry.getValue();
            String key = entry.getKey();

            // Calculate slot in content area (middle 3 rows, skipping borders)
            int relativeIndex = i - startIndex;
            int row = (relativeIndex / 7) + 1; // Start from row 1
            int col = (relativeIndex % 7) + 1; // Start from column 1, skip border
            int slot = (row * 9) + col;

            // Add the item to the GUI
            addItemToGUI(slot, item, key);
        }
    }

    /**
     * Add pouch information to the GUI
     */
    private void addPouchInfo() {
        // Pouch stats and info
        List<String> infoLore = new ArrayList<>();
        infoLore.add(TEXT_COLOR + "Storage: " + SECONDARY_COLOR + pouch.getContents().size() + "/" + pouch.getSlots() + " items");

        if (pouch.isTrackStats()) {
            infoLore.add(TEXT_COLOR + "Items Collected: " + SECONDARY_COLOR + pouch.getStats().getItemsCollected());
            infoLore.add(TEXT_COLOR + "Last Used: " + SECONDARY_COLOR + pouch.getStats().getLastUsedFormatted());
        }

        // Add enchantment info
        if (!pouch.getEnchantments().isEmpty()) {
            infoLore.add("");
            infoLore.add(PRIMARY_COLOR + "Enchantments:");
            for (var enchant : pouch.getEnchantments()) {
                infoLore.add(SECONDARY_COLOR + "• " + TEXT_COLOR + enchant.getType().getDisplay() + " " +
                        getRomanNumeral(enchant.getLevel()));
            }
        }

        // Create info item
        Material infoMaterial = Material.ENCHANTED_BOOK;
        if (!pouch.getHdbId().isEmpty() && plugin.getHeadDatabaseHook().isEnabled()) {
            ItemStack headItem = plugin.getHeadDatabaseHook().getHeadFromID(pouch.getHdbId());
            if (headItem != null) {
                infoMaterial = headItem.getType();
            }
        }

        ItemStack infoItem = ItemUtils.createItem(
                infoMaterial,
                PRIMARY_COLOR + "Pouch Information",
                infoLore
        );

        // Place at bottom center
        inventory.setItem(49, infoItem);
    }

    /**
     * Add action buttons to the GUI
     */
    private void addActionButtons() {
        // Add custom buttons from config
        List<Map<String, Object>> buttons = pouch.getGuiButtons();

        for (Map<String, Object> button : buttons) {
            int configSlot = (int) button.getOrDefault("slot", 0);
            String action = (String) button.get("action");
            String icon = (String) button.getOrDefault("icon", "STONE");
            String name = (String) button.getOrDefault("name", "Button");
            List<String> lore = (List<String>) button.getOrDefault("lore", new ArrayList<>());

            if (action == null) {
                continue;
            }

            // Create the button
            Material iconMaterial;
            try {
                iconMaterial = Material.valueOf(icon);
            } catch (IllegalArgumentException e) {
                iconMaterial = Material.STONE;
            }

            ItemStack buttonItem = ItemUtils.createItem(
                    iconMaterial,
                    ChatColor.translateAlternateColorCodes('&', name),
                    lore
            );

            // Bottom row buttons (navigation row)
            int slot = configSlot;
            if (configSlot < 45) {
                // Move to navigation row if not already there
                slot = 46 + (configSlot % 8); // Avoid corners
            }

            inventory.setItem(slot, buttonItem);
            buttonActions.put(slot, "action:" + action);
        }

        // Add default buttons if no custom buttons exist
        if (buttons.isEmpty()) {
            // Sort button
            ItemStack sortButton = ItemUtils.createItem(
                    Material.HOPPER,
                    SECONDARY_COLOR + "Sort Items",
                    Arrays.asList(
                            TEXT_COLOR + "Left-click: " + SECONDARY_COLOR + "Sort by type",
                            TEXT_COLOR + "Right-click: " + SECONDARY_COLOR + "Sort by amount"
                    )
            );
            inventory.setItem(47, sortButton);
            buttonActions.put(47, "action:sort_type");

            // Withdraw all button
            ItemStack withdrawButton = ItemUtils.createItem(
                    Material.CHEST,
                    SECONDARY_COLOR + "Withdraw All",
                    Collections.singletonList(TEXT_COLOR + "Click to withdraw all items")
            );
            inventory.setItem(51, withdrawButton);
            buttonActions.put(51, "action:withdraw_all");
        }
    }

    /**
     * Get a unique identifier for an item to group similar items
     */
    private String getItemIdentifier(ItemStack item) {
        StringBuilder id = new StringBuilder(item.getType().name());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                id.append(":").append(meta.getDisplayName());
            }
            // Could also check enchantments, damage, etc. for more precise matching
        }

        return id.toString();
    }

    private void addItemToGUI(int slot, ItemStack item, String key) {
        // Create a COPY of the item for display purposes only
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();

        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            // Add blank line if lore exists and doesn't end with one
            if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
                lore.add("");
            }

            // Add action information
            if (item.getAmount() > 0) {
                lore.add(ChatColor.YELLOW + "Left-Click: " + ChatColor.GRAY + "Withdraw 64");
                lore.add(ChatColor.YELLOW + "Shift+Left-Click: " + ChatColor.GRAY + "Withdraw All");

                // Add right-click action based on item type
                if (isConsumable(item)) {
                    lore.add(ChatColor.YELLOW + "Right-Click: " + ChatColor.GRAY + "Consume");
                } else {
                    lore.add(ChatColor.YELLOW + "Right-Click: " + ChatColor.GRAY + "Sell All");
                }
            } else {
                // For items with zero count
                lore.add(ChatColor.GRAY + "This pouch can collect this item");
                lore.add(ChatColor.GRAY + "None collected yet");
            }

            // Show total count if more than max stack size
            if (item.getAmount() > item.getMaxStackSize()) {
                lore.add("");
                lore.add(ChatColor.WHITE + "Total: " + ChatColor.GOLD + item.getAmount() + " items");
            }

            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }

        // Set a reasonable display amount (64 max for items with count, 1 for zero count)
        if (item.getAmount() > 0) {
            displayItem.setAmount(Math.min(item.getAmount(), 64));
        } else {
            displayItem.setAmount(1); // Show 1 for display purposes
        }

        // Add to GUI inventory and track the key
        inventory.setItem(slot, displayItem);
        itemSlots.put(slot, key);

        plugin.getDebug().log("Added item to GUI: " + item.getType() + " x" + item.getAmount() + " with key: " + key);
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
     * Get the inventory
     *
     * @return The inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Handle the inventory click event
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
        boolean isRightClick = event.isRightClick();

        // Check if this is a button
        if (buttonActions.containsKey(slot)) {
            String action = buttonActions.get(slot);
            handleButtonClick(action, isRightClick);
            return;
        }

        // Check if this is an item
        if (itemSlots.containsKey(slot)) {
            String itemKey = itemSlots.get(slot);
            handleItemClick(itemKey, slot, event.isShiftClick(), isRightClick);
        }
    }

    /**
     * Handle a button click
     *
     * @param action       The button action
     * @param isRightClick Whether the click was a right click
     */
    private void handleButtonClick(String action, boolean isRightClick) {
        if (action.startsWith("category:")) {
            // Change category
            String category = action.substring(9);
            currentCategory = category;
            initializeGUI();

        } else if (action.startsWith("page:")) {
            // Change page
            String page = action.substring(5);

            if (page.equals("prev")) {
                currentPage = Math.max(0, currentPage - 1);
            } else if (page.equals("next")) {
                currentPage++;
            }

            initializeGUI();

        } else if (action.startsWith("action:")) {
            // Execute a pouch action
            String actionType = action.substring(7);

            switch (actionType) {
                case "sort_type":
                    if (isRightClick) {
                        sortByAmount();
                    } else {
                        sortByType();
                    }
                    break;

                case "sort_rarity":
                    sortByRarity();
                    break;

                case "sort_amount":
                    sortByAmount();
                    break;

                case "merge":
                    mergeItems();
                    break;

                case "transfer":
                    transferItems();
                    break;

                case "convert":
                    convertItems();
                    break;

                case "withdraw_all":
                    withdrawAllItems();
                    break;
            }
        }
    }

    /**
     * Handle an item click
     *
     * @param itemKey      The item key
     * @param slot         The inventory slot
     * @param isShiftClick Whether shift was held
     * @param isRightClick Whether right mouse button was used
     */
    private void handleItemClick(String itemKey, int slot, boolean isShiftClick, boolean isRightClick) {
        plugin.getDebug().log("Item click: " + itemKey + " in slot " + slot);

        // Get the item from the pouch DIRECTLY
        Map<String, ItemStack> currentContents = pouch.getContents();
        if (!currentContents.containsKey(itemKey)) {
            plugin.getDebug().log("CRITICAL ERROR: Item " + itemKey + " not found in pouch contents!");
            player.sendMessage(ChatColor.RED + "Error: Item not found in pouch!");
            initializeGUI(); // Refresh the GUI
            return;
        }

        // Get a reference to the actual item in the pouch
        ItemStack pouchItem = currentContents.get(itemKey);
        if (pouchItem == null) {
            plugin.getDebug().log("CRITICAL ERROR: Item " + itemKey + " is null in pouch contents!");
            player.sendMessage(ChatColor.RED + "Error: Item is invalid!");
            initializeGUI(); // Refresh the GUI
            return;
        }

        // Log current state for debugging
        plugin.getDebug().log("Before action - Item: " + pouchItem.getType() + ", Amount: " + pouchItem.getAmount());
        plugin.getDebug().log("Before action - Pouch contents size: " + currentContents.size());

        // Handle right-click actions
        if (isRightClick) {
            if (isConsumable(pouchItem)) {
                // Make a copy for display purposes
                ItemStack displayCopy = pouchItem.clone();

                // Consume one item
                if (pouchItem.getAmount() <= 1) {
                    // Remove the last item
                    currentContents.remove(itemKey);
                    plugin.getDebug().log("Removed last consumable item");
                } else {
                    // Reduce stack by 1
                    pouchItem.setAmount(pouchItem.getAmount() - 1);
                    plugin.getDebug().log("Reduced consumable stack to " + pouchItem.getAmount());
                }

                // Apply consume effects
                if (displayCopy.getType().name().contains("POTION")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.GREEN + "Consumed " + getItemDisplayName(displayCopy));
                    player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                } else if (displayCopy.getType().isEdible()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.GREEN + "Consumed " + getItemDisplayName(displayCopy));
                    player.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                }
            } else {
                // Selling the item
                if (!plugin.getVaultHook().isEnabled()) {
                    player.sendMessage(ChatColor.RED + "Cannot sell items - economy is not enabled!");
                    return;
                }

                // Calculate value
                int amount = pouchItem.getAmount();
                double totalValue = plugin.getVaultHook().getSellValue(pouchItem) * amount;

                if (totalValue <= 0) {
                    player.sendMessage(ChatColor.RED + "This item has no value!");
                    return;
                }

                // Make a copy for display purposes
                ItemStack displayCopy = pouchItem.clone();

                // Remove the item from the pouch
                currentContents.remove(itemKey);
                plugin.getDebug().log("Removed sold item from pouch");

                // Add money to player
                plugin.getVaultHook().depositMoney(player, totalValue);

                // Notify player
                player.sendMessage(ChatColor.GREEN + "Sold " + amount + "x " + getItemDisplayName(displayCopy) +
                        " for " + plugin.getVaultHook().formatMoney(totalValue));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            }
        } else {
            // Handle left-click (withdraw)
            int withdrawAmount = isShiftClick ? pouchItem.getAmount() : Math.min(pouchItem.getAmount(), 64);
            plugin.getDebug().log("Withdrawing " + withdrawAmount + " items");

            // Create a clone of the item with the correct amount for withdrawal
            ItemStack withdrawItem = pouchItem.clone();
            withdrawItem.setAmount(withdrawAmount);

            // Try to add the item to player inventory
            HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(withdrawItem);

            if (notAdded.isEmpty()) {
                // Successfully added all to inventory
                if (withdrawAmount >= pouchItem.getAmount()) {
                    // Remove the entire stack
                    currentContents.remove(itemKey);
                    plugin.getDebug().log("Removed entire stack from pouch");
                } else {
                    // Reduce the stack
                    pouchItem.setAmount(pouchItem.getAmount() - withdrawAmount);
                    plugin.getDebug().log("Reduced stack to " + pouchItem.getAmount());
                }

                // Play success sound
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);

                // Send feedback
                player.sendMessage(ChatColor.GREEN + "Withdrew " + withdrawAmount + "x " + getItemDisplayName(withdrawItem));
            } else {
                // Couldn't add all items
                int addedAmount = withdrawAmount - notAdded.values().stream().mapToInt(ItemStack::getAmount).sum();

                if (addedAmount > 0) {
                    // Reduce the amount in the pouch by what was actually added
                    if (pouchItem.getAmount() <= addedAmount) {
                        currentContents.remove(itemKey);
                        plugin.getDebug().log("Removed entire stack after partial withdrawal");
                    } else {
                        pouchItem.setAmount(pouchItem.getAmount() - addedAmount);
                        plugin.getDebug().log("Reduced stack to " + pouchItem.getAmount() + " after partial withdrawal");
                    }

                    // Send partial success message
                    player.sendMessage(ChatColor.YELLOW + "Withdrew " + addedAmount + "x " +
                            getItemDisplayName(withdrawItem) + ". Your inventory is full!");
                } else {
                    // Couldn't add any items
                    player.sendMessage(ChatColor.RED + "Your inventory is full!");
                }
            }
        }

        // Log final state
        plugin.getDebug().log("After action - Pouch contents size: " + currentContents.size());

        // Save the pouch data IMMEDIATELY
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Refresh the GUI
        initializeGUI();
    }

    /**
     * Check if an item is consumable (potion, food, etc.)
     */
    private boolean isConsumable(ItemStack item) {
        Material type = item.getType();
        return type.isEdible() ||
                type.name().contains("POTION") ||
                type.name().equals("MILK_BUCKET");
    }

    /**
     * Consume an item from the pouch
     */
    private void consumeItem(ItemStack item, String itemKey) {
        Material type = item.getType();
        Map<String, ItemStack> contents = pouch.getContents();

        // Verify the item still exists in the pouch
        if (!contents.containsKey(itemKey)) {
            player.sendMessage(ChatColor.RED + "This item no longer exists in the pouch!");
            initializeGUI(); // Refresh the GUI
            return;
        }

        // Remove one item FIRST
        if (item.getAmount() <= 1) {
            contents.remove(itemKey);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        // IMMEDIATELY save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Apply appropriate effects
        if (type.name().contains("POTION")) {
            // Apply potion effect if applicable
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "Consumed " + getItemDisplayName(item));

            // Visual effect
            player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        } else if (type.isEdible()) {
            // Apply food effect
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "Consumed " + getItemDisplayName(item));

            // Visual effect
            player.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1, item);
        }

        // Refresh the GUI
        initializeGUI();
    }

    /**
     * Sell an item from the pouch
     */
    private void sellItem(ItemStack item, String itemKey) {
        // Check if Vault is available
        if (!plugin.getVaultHook().isEnabled()) {
            player.sendMessage(ChatColor.RED + "Cannot sell items - economy is not enabled!");
            return;
        }

        Map<String, ItemStack> contents = pouch.getContents();

        // Verify the item still exists in the pouch
        if (!contents.containsKey(itemKey)) {
            player.sendMessage(ChatColor.RED + "This item no longer exists in the pouch!");
            initializeGUI(); // Refresh the GUI
            return;
        }

        int amount = item.getAmount();
        double totalValue = plugin.getVaultHook().getSellValue(item) * amount;

        if (totalValue <= 0) {
            player.sendMessage(ChatColor.RED + "This item has no value!");
            return;
        }

        // Remove the item FIRST
        contents.remove(itemKey);

        // THEN add money to player
        plugin.getVaultHook().depositMoney(player, totalValue);

        // IMMEDIATELY save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Notify player
        player.sendMessage(ChatColor.GREEN + "Sold " + amount + "x " + getItemDisplayName(item) +
                " for " + plugin.getVaultHook().formatMoney(totalValue));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);

        // Refresh the GUI
        initializeGUI();
    }

    /**
     * Get the display name of an item
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
     * Convert an integer to a Roman numeral
     */
    private String getRomanNumeral(int num) {
        if (num <= 0 || num > 10) {
            return String.valueOf(num);
        }

        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return romans[num - 1];
    }

    /**
     * Handle the inventory drag event
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory() == inventory) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle the inventory close event
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

        // Remove this GUI from the manager
        plugin.getGuiManager().removePlayerGUI(player);

        // Unregister this listener
        HandlerList.unregisterAll(this);
    }

    /**
     * Withdraw all items from the pouch
     */
    private void withdrawAllItems() {
        // Create a copy of the contents to avoid concurrent modification
        Map<String, ItemStack> contents = new HashMap<>(pouch.getContents());

        if (contents.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "The pouch is empty!");
            return;
        }

        int totalItems = 0;
        int successfullyWithdrawn = 0;

        // Keep track of keys to remove and amounts to adjust
        Map<String, Integer> itemsToRemove = new HashMap<>();

        // Try to add all items to player's inventory
        for (Map.Entry<String, ItemStack> entry : contents.entrySet()) {
            ItemStack item = entry.getValue().clone();
            String key = entry.getKey();
            int originalAmount = item.getAmount();
            totalItems += originalAmount;

            // Try to add to inventory
            HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(item);

            if (notAdded.isEmpty()) {
                // All added, mark for removal from pouch
                itemsToRemove.put(key, originalAmount);
                successfullyWithdrawn += originalAmount;
            } else {
                // Partially added, update amounts
                int addedAmount = originalAmount - notAdded.values().stream()
                        .mapToInt(ItemStack::getAmount)
                        .sum();

                if (addedAmount > 0) {
                    itemsToRemove.put(key, addedAmount);
                    successfullyWithdrawn += addedAmount;
                }
            }
        }

        // Now actually remove the items from the pouch
        Map<String, ItemStack> pouchContents = pouch.getContents();
        for (Map.Entry<String, Integer> entry : itemsToRemove.entrySet()) {
            String key = entry.getKey();
            int amountToRemove = entry.getValue();

            ItemStack pouchItem = pouchContents.get(key);
            if (pouchItem != null) {
                if (pouchItem.getAmount() <= amountToRemove) {
                    pouchContents.remove(key);
                } else {
                    pouchItem.setAmount(pouchItem.getAmount() - amountToRemove);
                }
            }
        }

        // Save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Send feedback to player
        if (successfullyWithdrawn == totalItems) {
            player.sendMessage(ChatColor.GREEN + "Withdrew all items from the pouch (" + totalItems + " items)");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        } else if (successfullyWithdrawn > 0) {
            player.sendMessage(ChatColor.YELLOW + "Withdrew " + successfullyWithdrawn + "/" +
                    totalItems + " items. Your inventory is full!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f);
        } else {
            player.sendMessage(ChatColor.RED + "Your inventory is full! Couldn't withdraw any items.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }

        // Refresh the GUI
        initializeGUI();
    }

    /**
     * Sort items by type
     */
    private void sortByType() {
        // Get the pouch contents
        Map<String, ItemStack> contents = pouch.getContents();

        if (contents.isEmpty()) {
            return;
        }

        // Convert to a list
        List<Map.Entry<String, ItemStack>> contentsList = new ArrayList<>(contents.entrySet());

        // Sort by material name
        contentsList.sort(Comparator.comparing(entry -> entry.getValue().getType().name()));

        // Create a new sorted contents map
        Map<String, ItemStack> sortedContents = new LinkedHashMap<>();

        // Add items to the new map in order
        for (Map.Entry<String, ItemStack> entry : contentsList) {
            sortedContents.put(entry.getKey(), entry.getValue());
        }

        // Update the pouch contents
        pouch.setContents(sortedContents);

        // Save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Send message to the player
        plugin.getMessageUtils().sendMessage(player, "actions.sort", "type", "type");

        // Refresh the GUI
        initializeGUI();
    }

    /**
     * Sort items by rarity
     */
    private void sortByRarity() {
        // Get the pouch contents
        Map<String, ItemStack> contents = pouch.getContents();

        if (contents.isEmpty()) {
            return;
        }

        // Group items by rarity
        Map<String, List<Map.Entry<String, ItemStack>>> rarityGroups = new HashMap<>();

        for (Map.Entry<String, ItemStack> entry : contents.entrySet()) {
            ItemStack item = entry.getValue();
            String rarity = getRarity(item);

            if (!rarityGroups.containsKey(rarity)) {
                rarityGroups.put(rarity, new ArrayList<>());
            }

            rarityGroups.get(rarity).addAll(Collections.singletonList(entry));
        }

        // Sort rarities (common, uncommon, rare, epic, legendary)
        List<String> rarityOrder = Arrays.asList("common", "uncommon", "rare", "epic", "legendary");

        // Create a new sorted contents map
        Map<String, ItemStack> sortedContents = new LinkedHashMap<>();

        // Add items to the new map in order of rarity
        for (String rarity : rarityOrder) {
            if (rarityGroups.containsKey(rarity)) {
                for (Map.Entry<String, ItemStack> entry : rarityGroups.get(rarity)) {
                    sortedContents.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Add items with unknown rarity at the end
        for (Map.Entry<String, List<Map.Entry<String, ItemStack>>> entry : rarityGroups.entrySet()) {
            if (!rarityOrder.contains(entry.getKey())) {
                for (Map.Entry<String, ItemStack> itemEntry : entry.getValue()) {
                    sortedContents.put(itemEntry.getKey(), itemEntry.getValue());
                }
            }
        }

        // Update the pouch contents
        pouch.setContents(sortedContents);

        // Save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Send message to the player
        plugin.getMessageUtils().sendMessage(player, "actions.sort", "type", "rarity");

        // Refresh the GUI
        initializeGUI();
    }

    /**
     * Get the rarity of an item
     */
    private String getRarity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "unknown";
        }

        ItemMeta meta = item.getItemMeta();

        if (meta.hasLore()) {
            List<String> lore = meta.getLore();

            for (String line : lore) {
                String strippedLine = ChatColor.stripColor(line).toLowerCase();

                if (strippedLine.contains("rarity:")) {
                    if (strippedLine.contains("common")) {
                        return "common";
                    } else if (strippedLine.contains("uncommon")) {
                        return "uncommon";
                    } else if (strippedLine.contains("rare")) {
                        return "rare";
                    } else if (strippedLine.contains("epic")) {
                        return "epic";
                    } else if (strippedLine.contains("legendary")) {
                        return "legendary";
                    }
                }
            }
        }

        // Try to determine rarity from the item's display name color
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            ChatColor color = null;

            // Find the first color in the display name
            for (int i = 0; i < displayName.length() - 1; i++) {
                if (displayName.charAt(i) == ChatColor.COLOR_CHAR) {
                    char colorChar = displayName.charAt(i + 1);
                    color = ChatColor.getByChar(colorChar);
                    break;
                }
            }

            if (color != null) {
                if (color == ChatColor.WHITE) {
                    return "common";
                } else if (color == ChatColor.GREEN) {
                    return "uncommon";
                } else if (color == ChatColor.BLUE) {
                    return "rare";
                } else if (color == ChatColor.DARK_PURPLE) {
                    return "epic";
                } else if (color == ChatColor.GOLD) {
                    return "legendary";
                }
            }
        }

        return "unknown";
    }

    /**
     * Sort items by amount
     */
    private void sortByAmount() {
        // Get the pouch contents
        Map<String, ItemStack> contents = pouch.getContents();

        if (contents.isEmpty()) {
            return;
        }

        // Convert to a list
        List<Map.Entry<String, ItemStack>> contentsList = new ArrayList<>(contents.entrySet());

        // Sort by amount (descending)
        contentsList.sort((a, b) -> Integer.compare(b.getValue().getAmount(), a.getValue().getAmount()));

        // Create a new sorted contents map
        Map<String, ItemStack> sortedContents = new LinkedHashMap<>();

        // Add items to the new map in order of amount
        for (Map.Entry<String, ItemStack> entry : contentsList) {
            sortedContents.put(entry.getKey(), entry.getValue());
        }

        // Update the pouch contents
        pouch.setContents(sortedContents);

        // Save the pouch data
        plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

        // Send message to the player
        plugin.getMessageUtils().sendMessage(player, "actions.sort", "type", "amount");

        // Refresh the GUI
        initializeGUI();
    }

    /**
     * Merge items
     */
    private void mergeItems() {
        // Execute the merge action
        for (PouchAction action : pouch.getActions()) {
            if (action.getType().equals("merge")) {
                if (action.execute(plugin, player, pouch)) {
                    // Save the pouch data
                    plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

                    // Refresh the GUI
                    initializeGUI();
                    break;
                }
            }
        }
    }

    /**
     * Transfer items
     */
    private void transferItems() {
        // Execute the transfer action
        for (PouchAction action : pouch.getActions()) {
            if (action.getType().equals("transfer")) {
                if (action.execute(plugin, player, pouch)) {
                    // Save the pouch data
                    plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

                    // Refresh the GUI
                    initializeGUI();
                    break;
                }
            }
        }
    }

    /**
     * Convert items
     */
    private void convertItems() {
        // Execute the convert action
        for (PouchAction action : pouch.getActions()) {
            if (action.getType().equals("convert")) {
                if (action.execute(plugin, player, pouch)) {
                    // Save the pouch data
                    plugin.getPlayerDataManager().savePlayerPouch(player.getUniqueId(), pouch);

                    // Refresh the GUI
                    initializeGUI();
                    break;
                }
            }
        }
    }
}