package com.chathook;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class ChatHookPlugin extends JavaPlugin {
    
    private WebServer webServer;
    private WebhookSender webhookSender;
    
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
        
        int port = getConfig().getInt("web-server.port", 8081);
        webServer = new WebServer(this, port);
        webServer.start();
        
        logger.info("ChatHook has been enabled!");
    }
    
    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
        getLogger().info("ChatHook has been disabled!");
    }
}
