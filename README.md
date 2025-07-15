<p align="center">
   <a href="https://github.com/MusiJVR/IPViewer" target="_blank">
      <img width="180" height="180" src="https://github.com/MusiJVR/IPViewer/blob/main/docs/icon.png" alt="IPViewer">
   </a>
</p>

<h1 align="center">IPViewer</h1>

<p align="center">
    <a href="https://papermc.io/" target="_blank">
        <img src="https://img.shields.io/badge/Paper-1.20%2B-blue?style=flat-square&logo=spigotmc" alt="PaperMC">
    </a>
    <a href="https://modrinth.com/plugin/ipviewer" target="_blank">
        <img src="https://img.shields.io/badge/Modrinth-IPViewer-1bd96a?style=flat-square&logo=modrinth" alt="Modrinth">
    </a>
    <a href="https://docs.advntr.dev/minimessage/" target="_blank">
        <img src="https://img.shields.io/badge/MiniMessage-Adventure-blueviolet?style=flat-square" alt="MiniMessage">
    </a>
    <a href="https://www.java.com/" target="_blank">
        <img src="https://img.shields.io/badge/Java-21+-red?style=flat-square&logo=java" alt="Java">
    </a>
</p>

## 🌐 Overview

**IPViewer** is a plugin for [Paper](https://papermc.io/) Minecraft servers version _1.20_ and above, designed for server administrators and moderators. It provides a simple way to view players' IP addresses, helping with server management and detecting ban evasion or alt accounts.

<blockquote>All technical settings and features presented here are for the latest version of the plugin and will not always work on older versions.</blockquote>

## 🔍 Features

* View all players who have joined the server since the plugin was installed, along with their IP addresses and last seen time
* Filter and search player IPs easily
* Detect shared IPs across multiple accounts
* Helps track down ban evasion and suspicious activity

## 📜 Commands

Here are all the commands that can be used in the plugin:
* `/ipv help` - Displays all available plugin commands
* `/ipv reload` - Reloads the plugin configuration
* `/ipv list <filters (optional)>` - Shows a list of players with optional filters (e.g., by IP or join time)

## 🪪 Filters for `/ipv list`

The `/ipv list` command supports flexible filters to help you search and organize player IP data. All filters can be used together in a single command.

| **Filter**                          | **Description**                                                                              |
|-------------------------------------|----------------------------------------------------------------------------------------------|
| `page:<number>` or just `<number>`  | Displays a specific results page (e.g., `/ipv list 2` or `/ipv list page:2`)                 |
| `player:<text>`                     | Filters results by player name (supports partial matches and wildcards)                      |
| `ip:<text>`                         | Filters by IP address (supports partial matches and wildcards)                               |
| `country:<text>`                    | Filters by country name or code (e.g., `country:United States`)                              |
| `state:<text>`                      | Filters by region or state (useful for more granular IP geolocation, e.g., `state:New York`) |
| `time:<duration>`                   | Shows only players seen within a time range (e.g., `time:24h`, `time:30m`)                   |

### 🧠 Wildcards

Filters that use text (`player`, `ip`, `country`, `state`) support SQL-style wildcards:
* `%` - Matches any sequence of characters
* `_` - Matches a single character

### 📌 Example

```
/ipv list player:stev% ip:192.168.% time:7d page:3
```

This command shows page `3` of players whose names start with `stev`, IP starts with `192.168.`, and who have been seen in the last `7 days`.

## 📁 Configuration

The config file is generated on first launch and modify it to adjust output formats or other plugin settings. View default config [here](https://github.com/MusiJVR/IPViewer/blob/main/src/main/resources/config.yml).

All messages in the config use the [MiniMessage](https://docs.advntr.dev/minimessage/) format, which allows rich text formatting (colors, gradients, bold, etc.). You can preview and create formatted messages using the [MiniMessage Viewer](https://webui.advntr.dev/).

## ⚙️ Settings for Developers and Administrators

The plugin has permissions:

| **Permissions**             | **Meaning**                           |
|-----------------------------|---------------------------------------|
| `ipviewer.ipviewer`         | Grants access to the `/ipv` command   |

## ❗ Issues

Please leave messages about any errors you find [here](https://github.com/MusiJVR/IPViewer/issues) or on the [Discord](https://discord.gg/xY8WJt7VGr)

## 💬 Social Media

- Page on [Modrinth](https://modrinth.com/plugin/ipviewer)
- Page on [GitHub](https://github.com/MusiJVR/IPViewer)
- Page on [Discord](https://discord.gg/xY8WJt7VGr)
