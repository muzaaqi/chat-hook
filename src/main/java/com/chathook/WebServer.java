package com.chathook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class WebServer {

    private final ChatHookPlugin plugin;
    private final int port;
    private HttpServer server;

    public WebServer(ChatHookPlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/chat", new ChatHandler());
            server.setExecutor(null); // default executor
            server.start();
            plugin.getLogger().info("Web server started on port " + port);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start web server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            if (!plugin.isReceiveEnabled()) {
                sendResponse(exchange, 503, "Service Unavailable: Chat receiving is disabled");
                return;
            }

            // Check IP Whitelist
            String remoteIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            boolean enableWhitelist = plugin.getConfig().getBoolean("enable-ip-whitelist", false);
            if (enableWhitelist) {
                Set<String> whitelist = plugin.getResolvedWhitelist();
                if (!whitelist.contains(remoteIp) && !whitelist.contains("0.0.0.0")) {
                    plugin.getLogger().warning("Blocked unauthorized web chat request from IP: " + remoteIp);
                    sendResponse(exchange, 403, "Forbidden: IP not whitelisted");
                    return;
                }
            }

            // Check Secret Key
            boolean enableSecret = plugin.getConfig().getBoolean("enable-secret-key", true);
            if (enableSecret) {
                String secretHeader = exchange.getRequestHeaders().getFirst("Authorization");
                String configSecret = plugin.getConfig().getString("secret-key");
                if (configSecret != null && !configSecret.isEmpty()) {
                    if (secretHeader == null || !secretHeader.equals("Bearer " + configSecret)) {
                        plugin.getLogger().warning("Blocked request with invalid secret from IP: " + remoteIp);
                        sendResponse(exchange, 401, "Unauthorized");
                        return;
                    }
                }
            }

            try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                if (!json.has("uuid") || !json.has("username") || !json.has("message")) {
                    sendResponse(exchange, 400, "Missing uuid, username, or message");
                    return;
                }

                String uuidStr = json.get("uuid").getAsString();
                String username = json.get("username").getAsString();
                String message = json.get("message").getAsString();
                String source = json.has("source") ? json.get("source").getAsString().toLowerCase() : "web";
                String sourceTag = plugin.getConfig().getString("source_tags." + source, source.toUpperCase());
                String group = json.has("group") ? json.get("group").getAsString().toUpperCase() : "MEMBER";
                String displayName = json.has("realname") && !json.get("realname").isJsonNull() && !json.get("realname").getAsString().isEmpty()
                        ? json.get("realname").getAsString()
                        : username;

                boolean isDiscord = "discord".equalsIgnoreCase(source);

                // Broadcast message to Minecraft chat
                Bukkit.getScheduler().runTask(plugin, () -> {
                    NamedTextColor prefixColor = isDiscord
                            ? NamedTextColor.BLUE
                            : "web".equalsIgnoreCase(source)
                            ? NamedTextColor.AQUA
                            : NamedTextColor.GREEN;

                    Component chatMessage = Component.text("[" + sourceTag + "] ", prefixColor)
                            .append(Component.text("[" + group + "] - ", NamedTextColor.GRAY))
                            .append(Component.text(displayName, NamedTextColor.WHITE))
                            .append(Component.text(" » ", NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text(message, NamedTextColor.GRAY));
                            
                    Bukkit.broadcast(chatMessage);
                });

                sendResponse(exchange, 200, "OK");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing incoming webhook", e);
                sendResponse(exchange, 500, "Internal Server Error");
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
