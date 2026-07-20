package com.chathook;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatHookCommand implements CommandExecutor, TabCompleter {

    private final ChatHookPlugin plugin;

    public ChatHookCommand(ChatHookPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chathook.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§bChatHook Commands:");
            sender.sendMessage("§f/chathook reload §7- Reloads config");
            sender.sendMessage("§f/chathook send <on|off> §7- Toggle sending messages");
            sender.sendMessage("§f/chathook receive <on|off> §7- Toggle receiving messages");
            sender.sendMessage("§f/chathook seturl <url> §7- Set webhook URL");
            sender.sendMessage("§f/chathook setport <port> §7- Set web server port");
            sender.sendMessage("§f/chathook addip <ip> §7- Add IP to whitelist");
            sender.sendMessage("§f/chathook removeip <ip> §7- Remove IP from whitelist");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                plugin.reloadConfig();
                plugin.updateResolvedWhitelist();
                plugin.restartWebServer();
                sender.sendMessage("§aChatHook configuration reloaded!");
                break;
                
            case "send":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /chathook send <on|off>");
                    return true;
                }
                boolean enableSend = args[1].equalsIgnoreCase("on");
                plugin.setSendEnabled(enableSend);
                sender.sendMessage(enableSend ? "§aMessage sending enabled." : "§cMessage sending disabled.");
                break;
                
            case "receive":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /chathook receive <on|off>");
                    return true;
                }
                boolean enableReceive = args[1].equalsIgnoreCase("on");
                plugin.setReceiveEnabled(enableReceive);
                sender.sendMessage(enableReceive ? "§aMessage receiving enabled." : "§cMessage receiving disabled.");
                break;
                
            case "seturl":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /chathook seturl <url>");
                    return true;
                }
                String url = args[1];
                plugin.getConfig().set("webhook-url", url);
                plugin.saveConfig();
                sender.sendMessage("§aWebhook URL set to: §f" + url);
                break;
                
            case "setport":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /chathook setport <port>");
                    return true;
                }
                try {
                    int port = Integer.parseInt(args[1]);
                    plugin.getConfig().set("web-server.port", port);
                    plugin.saveConfig();
                    plugin.restartWebServer();
                    sender.sendMessage("§aWeb server port set to §f" + port + "§a and restarted.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid port number.");
                }
                break;
                
            case "addip":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /chathook addip <ip>");
                    return true;
                }
                String ipToAdd = args[1];
                List<String> ips = plugin.getConfig().getStringList("ip-whitelist");
                if (!ips.contains(ipToAdd)) {
                    ips.add(ipToAdd);
                    plugin.getConfig().set("ip-whitelist", ips);
                    plugin.saveConfig();
                    plugin.updateResolvedWhitelist();
                    sender.sendMessage("§aIP/Domain §f" + ipToAdd + " §aadded to whitelist.");
                } else {
                    sender.sendMessage("§cIP/Domain is already in the whitelist.");
                }
                break;
                
            case "removeip":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /chathook removeip <ip>");
                    return true;
                }
                String ipToRemove = args[1];
                List<String> listToRemoveFrom = plugin.getConfig().getStringList("ip-whitelist");
                if (listToRemoveFrom.remove(ipToRemove)) {
                    plugin.getConfig().set("ip-whitelist", listToRemoveFrom);
                    plugin.saveConfig();
                    plugin.updateResolvedWhitelist();
                    sender.sendMessage("§aIP/Domain §f" + ipToRemove + " §aremoved from whitelist.");
                } else {
                    sender.sendMessage("§cIP/Domain not found in whitelist.");
                }
                break;
                
            default:
                sender.sendMessage("§cUnknown subcommand.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = Arrays.asList("reload", "send", "receive", "seturl", "setport", "addip", "removeip");
            return commands.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("receive"))) {
            return Arrays.asList("on", "off").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("removeip")) {
            List<String> ips = plugin.getConfig().getStringList("ip-whitelist");
            return ips.stream().filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
