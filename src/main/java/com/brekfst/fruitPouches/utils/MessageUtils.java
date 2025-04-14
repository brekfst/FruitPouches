package com.brekfst.fruitPouches.utils;

import com.brekfst.fruitPouches.FruitPouches;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling messages
 */
public class MessageUtils {

    private final FruitPouches plugin;
    private final Map<String, String> messages;

    /**
     * Create a new message utils instance
     *
     * @param plugin The plugin instance
     */
    public MessageUtils(FruitPouches plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();

        // Load messages
        loadMessages();
    }

    /**
     * Load messages from the configuration
     */
    public void loadMessages() {
        messages.clear();

        FileConfiguration config = plugin.getConfigManager().getMessagesConfig();
        String prefix = config.getString("prefix", "&8[&6Fruit&ePouches&8] ");

        // Load general messages
        loadMessagesFromSection(config, "general", prefix);

        // Load items messages
        loadMessagesFromSection(config, "items", prefix);

        // Load pouches messages
        loadMessagesFromSection(config, "pouches", prefix);

        // Load enchantments messages
        loadMessagesFromSection(config, "enchantments", prefix);

        // Load upgrades messages
        loadMessagesFromSection(config, "upgrades", prefix);

        // Load skins messages
        loadMessagesFromSection(config, "skins", prefix);

        // Load stats messages
        loadMessagesFromSection(config, "stats", prefix);

        // Load actions messages
        loadMessagesFromSection(config, "actions", prefix);

        plugin.getDebug().log("Loaded " + messages.size() + " messages");
    }

    /**
     * Load messages from a configuration section
     *
     * @param config The configuration
     * @param section The section
     * @param prefix The message prefix
     */
    private void loadMessagesFromSection(FileConfiguration config, String section, String prefix) {
        if (!config.isConfigurationSection(section)) {
            return;
        }

        for (String key : config.getConfigurationSection(section).getKeys(false)) {
            String path = section + "." + key;
            String message = config.getString(path);

            if (message != null) {
                messages.put(path, prefix + message);
            }
        }
    }

    /**
     * Send a message to a sender
     *
     * @param sender The sender
     * @param key The message key
     * @param replacements The replacements
     */
    public void sendMessage(CommandSender sender, String key, Object... replacements) {
        String message = getMessage(key, replacements);

        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    /**
     * Get a message
     *
     * @param key The message key
     * @param replacements The replacements
     * @return The message
     */
    public String getMessage(String key, Object... replacements) {
        String message = messages.get(key);

        if (message == null) {
            plugin.getDebug().log("Missing message: " + key);
            return "";
        }

        // Apply replacements
        if (replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    String placeholder = "{" + replacements[i] + "}";
                    String replacement = String.valueOf(replacements[i + 1]);
                    message = message.replace(placeholder, replacement);
                }
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}