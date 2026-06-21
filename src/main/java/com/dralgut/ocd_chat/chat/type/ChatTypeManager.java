package com.dralgut.ocd_chat.chat.type;

import com.dralgut.ocd_chat.OCDChat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ChatTypeManager {
    private static final Set<ChatType> TYPES = new HashSet<>();
    private static final int MIN_VALID_DISTANCE = -2;

    public record ChatType(String prefix, String permission, String format, int distance) {
    }

    public static void load() {
        TYPES.clear();

        FileConfiguration config = OCDChat.config;

        ConfigurationSection component = config.getConfigurationSection("chat.types");
        if (component == null) return;

        for (String typeName : component.getKeys(false)) {
            final String path = "chat.types." + typeName;

            final String prefix = config.getString(path + ".prefix");
            final String permission = config.getString(path + ".permission");
            final String format = config.getString(path + ".format");
            final int distance = config.getInt(path + ".distance", MIN_VALID_DISTANCE - 1);

            if (!isValidChatType(prefix, permission, format, distance, typeName)) {
                continue;
            }

            ChatType type = new ChatType(prefix, permission, format, distance);
            TYPES.add(type);
        }
    }

    private static boolean isValidChatType(String prefix, String permission, String format, int distance, String name) {
        String errorMessage = "Invalid or missing '%s' value for chat type '" + name + "' :(";
        boolean isValid = true;

        if (prefix == null) {
            OCDChat.logger.warning(String.format(errorMessage, "prefix"));
            isValid = false;
        }
        if (permission == null) {
            OCDChat.logger.warning(String.format(errorMessage, "permission"));
            isValid = false;
        }
        if (format == null || format.isBlank()) {
            OCDChat.logger.warning(String.format(errorMessage, "format"));
            isValid = false;
        }
        if (distance < MIN_VALID_DISTANCE) {
            OCDChat.logger.warning(String.format(errorMessage, "distance"));
            isValid = false;
        }

        return isValid;
    }

    public static Set<ChatType> getTypes() {
        return TYPES;
    }

    public static ChatType getType(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        return TYPES.stream()
                .filter(e -> (message.startsWith(e.prefix))
                        && (!message.substring(e.prefix.length()).trim().isEmpty()))
                .max(Comparator.comparingInt(e -> e.prefix.length()))
                .orElse(null);
    }
}
