package com.chathook;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;

public class WebhookSender {

    private final ChatHookPlugin plugin;
    private final HttpClient httpClient;

    public WebhookSender(ChatHookPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void sendChat(UUID uuid, String username, String realname, String group, String message) {
        if (!plugin.isSendEnabled()) {
            return;
        }

        String url = plugin.getConfig().getString("webhook-url");
        String secret = plugin.getConfig().getString("secret-key");
        boolean enableSecret = plugin.getConfig().getBoolean("enable-secret-key", true);
        
        if (url == null || url.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("uuid", uuid.toString());
                json.addProperty("username", username);
                json.addProperty("realname", realname);
                json.addProperty("group", group != null ? group.toUpperCase() : "MEMBER");
                json.addProperty("current_group", group != null ? group.toUpperCase() : "MEMBER");
                json.addProperty("type", "chat");
                json.addProperty("source", "minecraft");
                json.addProperty("message", message);
                
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8));
                        
                if (enableSecret && secret != null && !secret.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + secret);
                }
                
                HttpRequest request = requestBuilder.build();
                        
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 400) {
                    plugin.getLogger().warning("Failed to send chat webhook: HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending chat webhook: " + e.getMessage());
            }
        });
    }

    public void sendEvent(String eventType, UUID uuid, String username, String realname, String group) {
        if (!plugin.isSendEnabled()) {
            return;
        }

        String url = plugin.getConfig().getString("webhook-url");
        String secret = plugin.getConfig().getString("secret-key");
        boolean enableSecret = plugin.getConfig().getBoolean("enable-secret-key", true);
        
        if (url == null || url.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("uuid", uuid.toString());
                json.addProperty("username", username);
                json.addProperty("realname", realname);
                json.addProperty("group", group != null ? group.toUpperCase() : "MEMBER");
                json.addProperty("current_group", group != null ? group.toUpperCase() : "MEMBER");
                json.addProperty("type", eventType);
                json.addProperty("source", "minecraft");
                json.addProperty("message", "");
                
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8));
                        
                if (enableSecret && secret != null && !secret.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + secret);
                }
                
                HttpRequest request = requestBuilder.build();
                        
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 400) {
                    plugin.getLogger().warning("Failed to send " + eventType + " webhook: HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending " + eventType + " webhook: " + e.getMessage());
            }
        });
    }
}
