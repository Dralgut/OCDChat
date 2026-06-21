package com.dralgut.ocd_chat.chat;

import com.dralgut.ocd_chat.OCDChat;
import com.dralgut.ocd_chat.chat.type.ChatTypeManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatCore implements Listener {
    private final static int UNLIMITED = -2;
    private final static int UNLIMITED_FOR_WORLD = -1;

    // ping
    public static boolean isPingEnable;
    private static Pattern pingPrefixPattern;
    private static String pingPrefix;
    private static String pingFormat;
    private static String pingActiveFormat;
    private static String pingSound;

    //errors
    private static String nobodySawMessage;
    private static String noAccessToChatMessage;

    public static void load() {
        FileConfiguration config = OCDChat.config;

        isPingEnable = config.getBoolean("ping.enable", false);
        if (isPingEnable) {
            pingPrefix = config.getString("ping.prefix", "@");
            pingPrefixPattern = Pattern.compile(Pattern.quote(pingPrefix) + "(\\w+)");
            pingFormat = config.getString("ping.format", "§f§l§{player}§r");
            pingActiveFormat = config.getString("ping.active_format", "§6§l§n{player}§r");
            pingSound = config.getString("ping.sound", "minecraft:block.note_block.pling");
        }

        nobodySawMessage = config.getString("messages.nobody_saw", "");
        noAccessToChatMessage = config.getString("messages.no_access_to_chat", "");
    }

    @EventHandler
    public void onChatEvent(AsyncPlayerChatEvent e) {
        if (e.isCancelled()) return;
        e.setCancelled(true);

        Player sender = e.getPlayer();
        String message = e.getMessage();

        ChatTypeManager.ChatType type = ChatTypeManager.getType(message);
        if (type == null) {
            return;
        }

        String permission = type.permission();
        if (permission.isEmpty() || sender.hasPermission(permission)) {
            int distance = type.distance();
            Set<Player> recipients = getRecipients(sender, distance);

            for (Player recipient : recipients) {
                String processedMessage = processMessage(message, recipient, sender, type);
                recipient.sendMessage(processedMessage);
            }

            if (recipients.size() < 2) {
                printError(sender, nobodySawMessage);
            }
        } else {
            printError(sender, noAccessToChatMessage);
        }
    }

    private String processMessage(String message, Player recipient, Player sender, ChatTypeManager.ChatType type) {
        String processedMessage = message;
        if (isPingEnable && message.contains(pingPrefix)) {
            processedMessage = processMentions(processedMessage, recipient);
        }
        return processFormat(type, sender, processedMessage);
    }


    private String processMentions(String message, Player recipient) {
        if (!isPingEnable || !message.contains(pingPrefix)) return message;

        Matcher matcher = pingPrefixPattern.matcher(message);
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            String playerName = matcher.group(1);
            Player target = Bukkit.getPlayerExact(playerName);

            if (target != null) {
                String replacement;
                if (target.getName().equals(recipient.getName())) {
                    replacement = pingActiveFormat.replace("{player}", playerName);
                    recipient.playSound(recipient.getLocation(), pingSound, 1.0f, 1.0f);
                } else {
                    replacement = pingFormat.replace("{player}", playerName);
                }
                matcher.appendReplacement(builder, replacement);
            } else {
                matcher.appendReplacement(builder, matcher.group(0));
            }
        }
        matcher.appendTail(builder);

        return builder.toString();
    }

    private void printError(Player player, String errorMessage) {
        if (!errorMessage.isEmpty()) {
            player.sendMessage(errorMessage);
        }
    }

    private Set<Player> getRecipients(Player sender, int distance) {

        if (distance == UNLIMITED) {
            return new HashSet<>(Bukkit.getOnlinePlayers());
        }

        List<Player> worldPlayers = sender.getWorld().getPlayers();
        if (distance == UNLIMITED_FOR_WORLD) {
            return new HashSet<>(worldPlayers);
        }

        final int finalDistance = Math.max(distance, 0);
        final int squaredDistance = finalDistance * finalDistance;
        final Location senderLocation = sender.getLocation();

        return worldPlayers.stream()
                .filter(player -> {
                    Location playerLocation = player.getLocation();
                    if (!Objects.equals(senderLocation.getWorld(), playerLocation.getWorld())) return false;
                    return senderLocation.distanceSquared(playerLocation) <= squaredDistance;
                })
                .collect(Collectors.toSet());
    }

    private String processFormat(ChatTypeManager.ChatType type, Player sender, String message) {
        String format = ChatColor.translateAlternateColorCodes('&', type.format());

        if (OCDChat.PAPI) format = PlaceholderAPI.setPlaceholders(sender, format);

        String prefix = type.prefix();
        String actualMessage = message;
        if (!prefix.isEmpty() && message.startsWith(prefix)) {
            actualMessage = message.substring(prefix.length()).trim();
        }

        return format
                .replace("{player}", sender.getName())
                .replace("{message}", actualMessage);
    }
}
