package com.brekfst.fruitPouches.utils;

import com.brekfst.fruitPouches.FruitPouches;
import com.brekfst.fruitPouches.models.CustomItem;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;

/**
 * Hook for Vault economy
 */
public class VaultHook {

    private final FruitPouches plugin;
    private boolean enabled;
    private Economy economy;

    /**
     * Create a new Vault hook
     *
     * @param plugin The plugin instance
     */
    public VaultHook(FruitPouches plugin) {
        this.plugin = plugin;
        this.enabled = false;
        this.economy = null;

        // Try to hook into Vault
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

            if (rsp != null) {
                this.economy = rsp.getProvider();
                this.enabled = (this.economy != null);

                if (this.enabled) {
                    plugin.getDebug().log("Successfully hooked into Vault economy");
                } else {
                    plugin.getDebug().log("Failed to hook into Vault economy");
                }
            } else {
                plugin.getDebug().log("Vault plugin found, but no economy provider is registered");
            }
        } else {
            plugin.getDebug().log("Vault plugin not found, economy features will be disabled");
        }
    }

    /**
     * Check if Vault is enabled
     *
     * @return true if Vault is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Deposit money to a player
     *
     * @param player The player
     * @param amount The amount
     * @return true if the transaction was successful
     */
    public boolean depositMoney(Player player, double amount) {
        if (!enabled || economy == null) {
            return false;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Withdraw money from a player
     *
     * @param player The player
     * @param amount The amount
     * @return true if the transaction was successful
     */
    public boolean withdrawMoney(Player player, double amount) {
        if (!enabled || economy == null) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Check if a player has enough money
     *
     * @param player The player
     * @param amount The amount
     * @return true if the player has enough money
     */
    public boolean hasMoney(Player player, double amount) {
        if (!enabled || economy == null) {
            return false;
        }

        return economy.has(player, amount);
    }

    /**
     * Get a player's balance
     *
     * @param player The player
     * @return The player's balance
     */
    public double getBalance(Player player) {
        if (!enabled || economy == null) {
            return 0.0;
        }

        return economy.getBalance(player);
    }

    /**
     * Format money
     *
     * @param amount The amount
     * @return The formatted money string
     */
    public String formatMoney(double amount) {
        if (!enabled || economy == null) {
            DecimalFormat df = new DecimalFormat("#,##0.00");
            return "$" + df.format(amount);
        }

        return economy.format(amount);
    }

    /**
     * Sell an item
     *
     * @param player The player
     * @param item The item
     * @return The amount of money received
     */
    public double sellItem(Player player, ItemStack item) {
        if (!enabled || economy == null) {
            return 0.0;
        }

        double sellValue = getSellValue(item);
        double totalValue = sellValue * item.getAmount();

        if (totalValue > 0) {
            depositMoney(player, totalValue);
        }

        return totalValue;
    }

    /**
     * Get the sell value of an item
     *
     * @param item The item
     * @return The sell value
     */
    public double getSellValue(ItemStack item) {
        // Check if it's a custom item
        String customItemId = plugin.getCustomItemManager().matchCustomItem(item);

        if (customItemId != null) {
            CustomItem customItem = plugin.getCustomItemManager().getCustomItem(customItemId);

            if (customItem != null) {
                return customItem.getSellValue();
            }
        }

        // Return default sell value for vanilla items
        return plugin.getConfigManager().getMainConfig().getDouble("economy.default-sell-value", 1.0);
    }
}