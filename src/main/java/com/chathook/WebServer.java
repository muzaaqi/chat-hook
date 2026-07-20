package com.chathook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.xephi.authme.api.v3.AuthMeApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
            List<String> whitelist = plugin.getConfig().getStringList("ip-whitelist");
            if (!whitelist.isEmpty() && !whitelist.contains(remoteIp) && !whitelist.contains("0.0.0.0")) {
                plugin.getLogger().warning("Blocked request from non-whitelisted IP: " + remoteIp);
                sendResponse(exchange, 403, "Forbidden IP");
                return;
            }

            // Check Secret Key
            String secretHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String configSecret = plugin.getConfig().getString("secret-key");
            if (configSecret != null && !configSecret.isEmpty()) {
                if (secretHeader == null || !secretHeader.equals("Bearer " + configSecret)) {
                    plugin.getLogger().warning("Blocked request with invalid secret from IP: " + remoteIp);
                    sendResponse(exchange, 401, "Unauthorized");
                    return;
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

                // Check if the user is registered in AuthMe (optional check since we don't need password)
                if (Bukkit.getPluginManager().getPlugin("AuthMe") != null) {
                    if (!AuthMeApi.getInstance().isRegistered(username)) {
                        sendResponse(exchange, 403, "User is not registered in AuthMe");
                        return;
                    }
                }

                // Broadcast message to Minecraft chat
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Component chatMessage = Component.text("[Web] ", NamedTextColor.AQUA)
                            .append(Component.text(username + ": ", NamedTextColor.WHITE))
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
