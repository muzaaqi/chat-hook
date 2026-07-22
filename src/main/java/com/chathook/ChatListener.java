package com.chathook;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final ChatHookPlugin plugin;
    private final WebhookSender webhookSender;

    public ChatListener(ChatHookPlugin plugin, WebhookSender webhookSender) {
        this.plugin = plugin;
        this.webhookSender = webhookSender;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        
        String group = getPlayerGroup(player);
        
        webhookSender.sendChat(player.getUniqueId(), player.getName(), group, message);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String group = getPlayerGroup(player);
        webhookSender.sendEvent("join", player.getUniqueId(), player.getName(), group);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String group = getPlayerGroup(player);
        webhookSender.sendEvent("leave", player.getUniqueId(), player.getName(), group);
    }

    private String getPlayerGroup(Player player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                LuckPerms api = LuckPermsProvider.get();
                User user = api.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    return user.getPrimaryGroup();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get LuckPerms group: " + e.getMessage());
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
}
