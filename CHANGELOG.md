# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-07-25

### 🚀 New Features
- **Player Join & Leave Events**: ChatHook now sends webhooks to your website whenever a player joins or leaves the server.
- **Event Types in Webhooks**: Outbound JSON payloads now include a `"type"` property (`"chat"`, `"join"`, `"leave"`) to help your backend differentiate between events.
- **Custom Display Names (`realname`)**: The plugin now supports an optional `"realname"` field in incoming webhooks. If provided, it will completely replace the standard username for the chat display.
- **Dynamic Diagnostics**: Added the `/chathook check` command! This command runs an asynchronous `HEAD` request to your webhook URL and verifies your IP whitelist configurations without freezing the server.
- **Domain Support for Whitelists**: You can now use full domain names (e.g., `api.yoursite.com`) in the `ip-whitelist` configuration. The plugin will asynchronously resolve them into raw IP addresses.

### 🎨 Styling & Design
- **New Web Prefix**: The default lowercase `[Web]` prefix has been upgraded to a sleeker uppercase `[WEB]`.
- **Pink Separator**: The chat separator between the username and the message has been changed from standard white `: ` to a gorgeous pink double arrow (` » `).

### ⚙️ Configuration & System
- **Security Toggles**: Introduced `enable-secret-key` (default: true) and `enable-ip-whitelist` (default: false) in the `config.yml` to give you more control over endpoint protection.
- **Auto-Migration (v2)**: Implemented seamless configuration auto-migration using Bukkit's `copyDefaults(true)`. Upgrading to version 1.1.0 will safely merge new features into your `config.yml` while preserving your old values and YAML comments!
