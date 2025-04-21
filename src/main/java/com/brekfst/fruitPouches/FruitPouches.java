package com.brekfst.fruitPouches;

import com.brekfst.fruitPouches.commands.FruitPouchCommand;
import com.brekfst.fruitPouches.commands.TabCompleter;
import com.brekfst.fruitPouches.config.*;
import com.brekfst.fruitPouches.data.DataManager;
import com.brekfst.fruitPouches.data.PlayerDataManager;
import com.brekfst.fruitPouches.data.StatsManager;
import com.brekfst.fruitPouches.events.ItemPickupListener;
import com.brekfst.fruitPouches.events.PouchInteractListener;
import com.brekfst.fruitPouches.events.PouchTradeListener;
import com.brekfst.fruitPouches.gui.GuiManager;
import com.brekfst.fruitPouches.listeners.PlayerJoinQuitListener;
import com.brekfst.fruitPouches.utils.Debug;
import com.brekfst.fruitPouches.utils.HeadDatabaseHook;
import com.brekfst.fruitPouches.utils.MessageUtils;
import com.brekfst.fruitPouches.utils.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FruitPouches extends JavaPlugin {

    private static FruitPouches instance;
    private ConfigManager configManager;
    private CustomItemManager customItemManager;
    private PouchManager pouchManager;
    private EnchantmentManager enchantmentManager;
    private SkinManager skinManager;
    private PlayerDataManager playerDataManager;
    private StatsManager statsManager;
    private GuiManager guiManager;
    private DataManager dataManager;
    private HeadDatabaseHook headDatabaseHook;
    private VaultHook vaultHook;
    private MessageUtils messageUtils;
    private ExecutorService asyncExecutor;
    private Debug debug;
    private PriceManager priceManager; // New field for PriceManager

    @Override
    public void onEnable() {
        // Initialize the instance
        instance = this;

        // Create configuration files if they don't exist
        saveDefaultConfig();
        createConfigFile("custom_items.yml");
        createConfigFile("pouches.yml");
        createConfigFile("enchantments.yml");
        createConfigFile("messages.yml");
        createConfigFile("skins.yml");
        createConfigFile("prices.yml"); // Add the new prices.yml file

        // Initialize debug mode
        debug = new Debug(this);

        // Setup async executor for data operations
        asyncExecutor = Executors.newFixedThreadPool(2);

        // Initialize managers
        configManager = new ConfigManager(this);
        customItemManager = new CustomItemManager(this);
        pouchManager = new PouchManager(this);
        enchantmentManager = new EnchantmentManager(this);
        skinManager = new SkinManager(this);
        playerDataManager = new PlayerDataManager(this);
        statsManager = new StatsManager(this);
        dataManager = new DataManager(this);
        messageUtils = new MessageUtils(this);
        priceManager = new PriceManager(this); // Initialize the PriceManager

        // Check for dependencies
        if (!setupDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize GUI manager (after dependencies are setup)
        guiManager = new GuiManager(this);

        // Register commands
        getCommand("fruitpouch").setExecutor(new FruitPouchCommand(this));
        getCommand("fruitpouch").setTabCompleter(new TabCompleter(this));

        // Register event listeners
        registerEventListeners();

        // Schedule data saving task
        setupDataSavingTask();

        // Load all player data for online players (in case of reload)
        Bukkit.getOnlinePlayers().forEach(player -> playerDataManager.loadPlayerData(player.getUniqueId()));

        debug.log("Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save all data before shutting down
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }

        if (statsManager != null) {
            statsManager.saveAllStats();
        }

        // Shutdown executor service
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }

        debug.log("Plugin disabled successfully!");
    }

    private boolean setupDependencies() {
        // Check for HeadDatabase (required)
        headDatabaseHook = new HeadDatabaseHook(this);
        if (!headDatabaseHook.isEnabled()) {
            getLogger().severe("HeadDatabase is required but not found! Disabling plugin.");
            return false;
        }

        // Check for Vault (optional)
        vaultHook = new VaultHook(this);
        if (!vaultHook.isEnabled()) {
            getLogger().warning("Vault not found. Economy features will be disabled.");
        }

        return true;
    }

    private void registerEventListeners() {
        getServer().getPluginManager().registerEvents(new ItemPickupListener(this), this);
        getServer().getPluginManager().registerEvents(new PouchInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PouchTradeListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
    }

    private void setupDataSavingTask() {
        long saveInterval = getConfig().getLong("data.save-interval", 300); // Default: 5 minutes
        long backupInterval = getConfig().getLong("data.backup-interval", 3600); // Default: 1 hour

        // Regular saving task
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            debug.log("Running scheduled data save...");
            playerDataManager.saveAllPlayerData();
            statsManager.saveAllStats();
            debug.log("Scheduled data save completed.");
        }, saveInterval * 20L, saveInterval * 20L);

        // Less frequent backup task
        if (getConfig().getBoolean("data.enable-backups", true)) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                debug.log("Running scheduled data backup...");
                configManager.createAllBackups();
                debug.log("Scheduled data backup completed.");
            }, backupInterval * 20L, backupInterval * 20L);
        }
    }

    private void createConfigFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            saveResource(fileName, false);
        }
    }

    public static FruitPouches getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }

    public PouchManager getPouchManager() {
        return pouchManager;
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public HeadDatabaseHook getHeadDatabaseHook() {
        return headDatabaseHook;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }

    public Debug getDebug() {
        return debug;
    }

    // New getter for PriceManager
    public PriceManager getPriceManager() {
        return priceManager;
    }
}