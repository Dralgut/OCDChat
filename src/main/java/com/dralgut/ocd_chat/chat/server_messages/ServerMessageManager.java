package com.dralgut.ocd_chat.chat.server_messages;

import com.dralgut.ocd_chat.OCDChat;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerMessageManager {
    private static final List<ServerMessage> MESSAGES = new ArrayList<>();

    public static boolean isServerMessageEnable;

    private static BukkitTask task;
    private static int pos;

    public record ServerMessage(String name, String[] lines) {
    }

    public static void load() {
        stop();
        MESSAGES.clear();

        FileConfiguration config = OCDChat.config;

        isServerMessageEnable = config.getBoolean("server_messages.enable", false);
        if (!isServerMessageEnable) return;

        ConfigurationSection component = config.getConfigurationSection("server_messages.messages");
        if (component == null) return;

        for (String name : component.getKeys(false)) {
            List<String> rawLines = component.getStringList(name);

            if (rawLines.isEmpty()) {
                OCDChat.logger.warning(String.format("Invalid parameters for server message '%s' :(", name));
                continue;
            }

            String[] lines = rawLines.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toArray(String[]::new);

            ServerMessage message = new ServerMessage(name, lines);
            MESSAGES.add(message);
        }

        start();
    }

    private static void broadcastMessage() {
        if (MESSAGES.isEmpty()) return;
        ServerMessage message = MESSAGES.get(pos);

        if (!OCDChat.PAPI) {
            String[] lines = message.lines();
            for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(lines);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String[] personalizedLines = Arrays.stream(message.lines())
                        .map(line -> PlaceholderAPI.setPlaceholders(player, line))
                        .toArray(String[]::new);
                player.sendMessage(personalizedLines);
            }
        }

        pos = (pos + 1) % MESSAGES.size();
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        pos = 0;
    }

    public static void start() {
        if (!MESSAGES.isEmpty() && task == null) {
            task = Bukkit.getScheduler().runTaskTimer(OCDChat.getInstance(),
                    ServerMessageManager::broadcastMessage,
                    0L,
                    OCDChat.config.getLong("server_messages.period", 260));
        }
    }
}
