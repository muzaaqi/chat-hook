# Chat Hook 🔌

A Minecraft Paper plugin that bridges your Minecraft server chat with a website in real-time. It seamlessly integrates with LuckPerms, PlaceholderAPI, AuthMe, and EssentialsX.

## Features
- **Real-time Webhooks**: Sends all in-game chats instantly to your web backend as JSON.
- **REST API Receiver**: A built-in lightweight web server receives chat messages from your website and broadcasts them in-game.
- **Security First**: Protect endpoints using an IP Whitelist. On first run, it auto-generates a secure 64-character Bearer Authorization Secret Key if one isn't set.
- **Domain Support**: The IP whitelist supports raw IP addresses or full domain names (e.g., `api.yoursite.com`). Domains are resolved asynchronously in the background.
- **Configuration Toggles**: Easily toggle the requirement for the IP Whitelist or Secret Key via configuration.
- **Auto-Migration**: Older configurations safely and seamlessly update to the latest format without losing your custom values!
- **Plugin Integrations**: Automatically captures permission groups (via LuckPerms or PlaceholderAPI), handles muted players natively (EssentialsX compatibility), and ensures web-chat users are registered (AuthMe check).
- **Startup Diagnostics**: Beautiful ASCII log that asynchronously tests your webhook connection on startup to catch offline backends early.
- **Dynamic In-game Management**: Use `/chathook` commands to modify URLs, ports, and whitelists on-the-fly without needing to touch files or restart the server!

---

## Prerequisites
- Minecraft Paper 1.21.1 or higher
- Java 21
- Optional (but recommended) plugins: `LuckPerms`, `PlaceholderAPI`, `AuthMe`, `EssentialsX`

---

## Setup & Installation

### 1. Building the Plugin
To compile the plugin from the source code, open your terminal in the project directory and run:
```bash
# On Windows
gradlew build

# On Linux/macOS
./gradlew build
```
The compiled JAR file will be located in the `build/libs/` directory.

### 2. Installation
1. Move the compiled `ChatHook-1.1.0.jar` into your Minecraft server's `plugins/` folder.
2. Start the server. The plugin will generate a default configuration folder.

### 3. Configuration
Edit the `plugins/ChatHook/config.yml` file to link your website:

```yaml
# Configuration version (Do not change)
config-version: 2

# Webhook URL to send Minecraft chat to
webhook-url: "http://your-website.com/api/minecraft-chat"

# Web server settings for receiving chat from website
web-server:
  port: 8081
  
# Secret key to authenticate requests between server and website
# If left as default or empty, a secure key will be automatically generated on startup!
enable-secret-key: true
secret-key: "YOUR_SECRET_KEY"

# IP whitelist for incoming webhooks from website
# Supports IPs and Domain Names
enable-ip-whitelist: false
ip-whitelist:
  - "127.0.0.1"
  - "0.0.0.0"
  - "api.yoursite.com"
```
After editing, you can restart your Minecraft server or simply type `/chathook reload` in-game for the changes to take effect.

---

## Commands & Permissions

Permission required for all commands: `chathook.admin` (Default: OP)

- `/chathook check` - Runs an asynchronous diagnostic check verifying webhook reachability (via `HEAD` requests) and IP whitelist resolution.
- `/chathook reload` - Reloads `config.yml` from disk, resolves domains, and gracefully restarts the web server if needed.
- `/chathook send <on|off>` - Temporarily toggles sending chat webhooks to your website.
- `/chathook receive <on|off>` - Temporarily toggles receiving incoming chat requests from your website.
- `/chathook seturl <url>` - Updates the webhook URL on the fly and saves it.
- `/chathook setport <port>` - Updates the web server port, saves it, and instantly restarts the listener.
- `/chathook addip <ip/domain>` - Dynamically adds an IP or Domain to the `ip-whitelist`.
- `/chathook removeip <ip/domain>` - Dynamically removes an IP or Domain from the `ip-whitelist`.

---

## API Integration Guide

### 1. Web to Minecraft (Sending Chat)
To send a message from your website to the Minecraft server, make a `POST` request to the plugin's built-in web server. 

Incoming messages appear beautifully formatted in-game as:
`[WEB] Username » Message` (with the `»` colored in pink!)

**Endpoint:** `http://<minecraft-server-ip>:8081/api/chat`  
**Method:** `POST`  
**Headers Required:**
- `Content-Type: application/json`
- `Authorization: Bearer YOUR_SECRET_KEY` *(if `enable-secret-key` is true)*

**Payload You Must Send:**
```json
{
  "uuid": "player-uuid-here",
  "username": "PlayerName",
  "message": "Hello from the website!",
  "realname": "Optional Custom Display Name"
}
```

*Notes:*
- *If `realname` is provided, it will visually replace the `username` in the Minecraft chat.*
- *The plugin will verify if the `username` is registered in AuthMe. If they are not registered, the request will be rejected with a `403` status code.*

---

### 2. Minecraft to Web (Receiving Webhooks)
When a player chats in-game, the plugin will instantly send a `POST` request to your configured `webhook-url`.

**Headers You Will Receive:**
- `Content-Type: application/json`
- `Authorization: Bearer YOUR_SECRET_KEY` *(if `enable-secret-key` is true)*

**Example 'chat' Payload:**
```json
{
  "type": "chat",
  "uuid": "5b22b7a8-6f65-4f45-9a84-075e7a93c786",
  "name": "Notch",
  "group": "admin",
  "message": "Hello everyone on the website!"
}
```

**Example 'join' / 'leave' Payload:**
```json
{
  "type": "join",
  "uuid": "5b22b7a8-6f65-4f45-9a84-075e7a93c786",
  "name": "Notch",
  "group": "admin"
}
```

*Note: The `type` field indicates the event (`chat`, `join`, or `leave`). For `join` and `leave` events, the `message` field is intentionally omitted. The `group` is retrieved automatically via LuckPerms. If LuckPerms isn't found, it falls back to `%vault_rank%` via PlaceholderAPI.*

### 3. Diagnostic Webhook Handling (HEAD Requests)
When the plugin runs `/chathook check` or starts up, it sends a `HEAD` request to your `webhook-url` to verify connectivity. 

To prevent errors in your server console, it is recommended that your backend returns a `200 OK` (with no body) for `HEAD` requests to your webhook route.
