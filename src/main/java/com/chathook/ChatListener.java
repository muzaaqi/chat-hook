package com.chathook;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Comparator;

public class ChatListener implements Listener {

    private final ChatHookPlugin plugin;
    private final WebhookSender webhookSender;

    public ChatListener(ChatHookPlugin plugin, WebhookSender webhookSender) {
        this.plugin = plugin;
        this.webhookSender = webhookSender;
    }

    /**
     * Strips all Minecraft color/formatting codes from a string.
     * Removes both § (section sign) and & (ampersand) format codes.
     * Also strips hex color codes and MiniMessage tags like <red>, <bold>.
     * Called before any string is sent to the API.
     */
    public static String stripColorCodes(String input) {
        if (input == null) return "";
        // Strip §x and &x patterns (x = any formatting char 0-9a-fk-orA-FK-OR)
        String stripped = input.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "");
        // Strip §x§x§x§x§x§x / &x&x&x&x&x&x hex color format
        stripped = stripped.replaceAll("[§&]x([§&][0-9a-fA-F]){6}", "");
        // Strip MiniMessage tags
        stripped = stripped.replaceAll("<[^>]*>", "");
        return stripped.trim();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        
        String cleanMessage = stripColorCodes(message);
        String rawDisplayName = PlainTextComponentSerializer.plainText().serialize(player.displayName());
        String cleanRealname = stripColorCodes(rawDisplayName);
        if (cleanRealname.isEmpty()) {
            cleanRealname = stripColorCodes(player.getName());
        }
        String group = stripColorCodes(getEffectiveGroup(player));
        
        webhookSender.sendChat(player.getUniqueId(), stripColorCodes(player.getName()), cleanRealname, group, cleanMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String rawDisplayName = PlainTextComponentSerializer.plainText().serialize(event.joinMessage() != null ? player.displayName() : player.name());
        String cleanRealname = stripColorCodes(rawDisplayName);
        if (cleanRealname.isEmpty()) {
            cleanRealname = stripColorCodes(player.getName());
        }
        String group = stripColorCodes(getEffectiveGroup(player));
        webhookSender.sendEvent("join", player.getUniqueId(), stripColorCodes(player.getName()), cleanRealname, group);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String rawDisplayName = PlainTextComponentSerializer.plainText().serialize(player.displayName());
        String cleanRealname = stripColorCodes(rawDisplayName);
        if (cleanRealname.isEmpty()) {
            cleanRealname = stripColorCodes(player.getName());
        }
        String group = stripColorCodes(getEffectiveGroup(player));
        webhookSender.sendEvent("leave", player.getUniqueId(), stripColorCodes(player.getName()), cleanRealname, group);
    }

    /**
     * Gets the effective group for this player using LuckPerms API.
     * Uses highest-weight inherited group from CachedPermissionData / InheritedGroups.
     * This mirrors the LuckPerms weight-based resolution.
     */
    private String getEffectiveGroup(Player player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                LuckPerms lp = LuckPermsProvider.get();
                User user = lp.getPlayerAdapter(Player.class).getUser(player);
                if (user != null) {
                    return user.getInheritedGroups(user.getQueryOptions())
                            .stream()
                            .filter(g -> !g.getName().equalsIgnoreCase("default"))
                            .max(Comparator.comparingInt(g -> g.getWeight().orElse(0)))
                            .map(Group::getName)
                            .orElse(user.getPrimaryGroup());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to resolve LuckPerms effective group: " + e.getMessage());
            }
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            String vaultGroup = PlaceholderAPI.setPlaceholders(player, "%vault_rank%");
            if (vaultGroup != null && !vaultGroup.equals("%vault_rank%") && !vaultGroup.isEmpty()) {
                return vaultGroup;
            }
        }
        return "default";
    }

    /**
     * Gets the LuckPerms prefix for a player using CachedMetaData.
     * For online players: uses PlayerAdapter (fast, cached).
     * Returns empty string if LuckPerms is not available or player has no prefix. Never returns null.
     */
    private String getPlayerPrefix(Player player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                LuckPerms lp = LuckPermsProvider.get();
                CachedMetaData meta = lp.getPlayerAdapter(Player.class).getMetaData(player);
                String prefix = meta.getPrefix();
                return prefix != null ? prefix : "";
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }
}
