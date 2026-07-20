package com.chathook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

public class ChatHookPlugin extends JavaPlugin {
    
    private WebServer webServer;
    private WebhookSender webhookSender;
    
    private boolean sendEnabled = true;
    private boolean receiveEnabled = true;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        Logger logger = getLogger();
        
        logger.info(" ");
        logger.info("  §b____ _           _     §3_   _             _    ");
        logger.info(" §b/ ___| |__   __ _| |_  §3| | | | ___   ___ | | __");
        logger.info("§b| |   | '_ \\ / _` | __| §3| |_| |/ _ \\ / _ \\| |/ /");
        logger.info("§b| |___| | | | (_| | |_  §3|  _  | (_) | (_) |   < ");
        logger.info(" §b\\____|_| |_|\\__,_|\\__| §3|_| |_|\\___/ \\___/|_|\\_\\");
        logger.info(" ");
        logger.info("    §fVersion: §a" + getDescription().getVersion() + " §f| Author: §a" + getDescription().getAuthors().get(0));
        logger.info(" ");
        
        String secret = getConfig().getString("secret-key");
        if (secret == null || secret.isEmpty() || secret.equals("YOUR_SECRET_KEY")) {
            String generatedSecret = java.util.UUID.randomUUID().toString().replace("-", "") 
                                   + java.util.UUID.randomUUID().toString().replace("-", "");
            getConfig().set("secret-key", generatedSecret);
            saveConfig();
            logger.info("Generated a new random secret-key. Please check your config.yml!");
        }
        
        webhookSender = new WebhookSender(this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, webhookSender), this);
        
        ChatHookCommand cmd = new ChatHookCommand(this);
        getCommand("chathook").setExecutor(cmd);
        getCommand("chathook").setTabCompleter(cmd);
        
        
        startWebServer();
        checkWebhookStatus();
        
        logger.info("§aChatHook has been successfully enabled!");
    }
    
    @Override
    public void onDisable() {
        stopWebServer();
        getLogger().info("ChatHook has been disabled!");
    }
    
    private void startWebServer() {
        int port = getConfig().getInt("web-server.port", 8081);
        webServer = new WebServer(this, port);
        webServer.start();
    }
    
    private void stopWebServer() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
    }
    
    public void restartWebServer() {
        stopWebServer();
        startWebServer();
    }
    
    public boolean isSendEnabled() { return sendEnabled; }
    public void setSendEnabled(boolean enabled) { this.sendEnabled = enabled; }
    
    public boolean isReceiveEnabled() { return receiveEnabled; }
    public void setReceiveEnabled(boolean enabled) { this.receiveEnabled = enabled; }
    
    private void checkWebhookStatus() {
        String url = getConfig().getString("webhook-url");
        if (url == null || url.isEmpty()) {
            getLogger().warning("Webhook URL is not configured. Outgoing messages will not be sent.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody()) // Sending HEAD request just to check reachability
                        .build();

                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                
                getLogger().info("§a[Webhook Check] Successfully connected to webhook! (Status: " + response.statusCode() + ")");
            } catch (Exception e) {
                getLogger().warning("§c[Webhook Check] Failed to connect to webhook URL: " + e.getMessage());
                getLogger().warning("§cPlease check your config.yml and ensure your backend server is running.");
            }
        });
    }
}
