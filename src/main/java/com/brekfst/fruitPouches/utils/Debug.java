package com.brekfst.fruitPouches.utils;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for debugging and logging
 */
public class Debug {

    private final FruitPouches plugin;
    private boolean debugEnabled;
    private File logFile;
    private SimpleDateFormat dateFormat;

    public Debug(FruitPouches plugin) {
        this.plugin = plugin;
        this.debugEnabled = plugin.getConfig().getBoolean("debug", false);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (debugEnabled) {
            setupLogFile();
        }
    }

    /**
     * Log a debug message if debug mode is enabled
     *
     * @param message The message to log
     */
    public void log(String message) {
        if (!debugEnabled) return;

        String formattedMessage = formatMessage(message);

        // Log to console
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&6FruitPouches Debug&8] &7" + message));

        // Log to file
        if (logFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(formattedMessage);
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Failed to write to debug log file: " + e.getMessage());
            }
        }
    }

    /**
     * Log a debug message to a specific sender
     *
     * @param sender The sender to receive the message
     * @param message The message to log
     */
    public void log(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&6FruitPouches Debug&8] &7" + message));
        }

        // Also log to normal debug channels
        log(message);
    }

    /**
     * Log an exception with stacktrace
     *
     * @param e The exception to log
     * @param message Additional context message
     */
    public void logException(Exception e, String message) {
        log("ERROR: " + message + " - " + e.getMessage());

        if (debugEnabled && logFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(formatMessage("ERROR: " + message + " - " + e.getMessage()));
                e.printStackTrace(writer);
            } catch (IOException ex) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Failed to write exception to debug log file: " + ex.getMessage());
            }
        }

        if (debugEnabled) {
            e.printStackTrace();
        }
    }

    /**
     * Setup the debug log file
     */
    private void setupLogFile() {
        File debugDir = new File(plugin.getDataFolder(), "debug");
        if (!debugDir.exists()) {
            debugDir.mkdirs();
        }

        String fileName = "debug-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        logFile = new File(debugDir, fileName);

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Failed to create debug log file: " + e.getMessage());
        }
    }

    /**
     * Format a message with timestamp
     *
     * @param message The message to format
     * @return Formatted message
     */
    private String formatMessage(String message) {
        return dateFormat.format(new Date()) + " | " + message;
    }

    /**
     * Check if debug mode is enabled
     *
     * @return true if debug mode is enabled
     */
    public boolean isEnabled() {
        return debugEnabled;
    }

    /**
     * Set the debug mode
     *
     * @param enabled true to enable debug mode
     */
    public void setEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        if (enabled && logFile == null) {
            setupLogFile();
        }
    }
}