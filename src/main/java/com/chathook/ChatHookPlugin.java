package com.chathook;

import org.bukkit.plugin.java.JavaPlugin;
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
        
        logger.info("ChatHook has been enabled!");
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
}
