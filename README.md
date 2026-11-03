<div align="center">

![Logo](docs/icon.png)

[![GitHub Release](https://img.shields.io/github/v/release/MusiJVR/IPViewer?style=for-the-badge)](https://github.com/MusiJVR/IPViewer/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/4E5j5m04?style=for-the-badge&label=Modrinth)](https://modrinth.com/plugin/IPViewer)
[![GitHub License](https://img.shields.io/github/license/MusiJVR/IPViewer?style=for-the-badge)](https://github.com/MusiJVR/IPViewer/blob/main/LICENSE)

[![Bukkit](https://badges.penpow.dev/badges/supported/bukkit/compact-minimal.svg)](https://bukkit.org/)
[![Spigot](https://badges.penpow.dev/badges/supported/spigot/compact-minimal.svg)](https://spigotmc.org/)
[![Paper](https://badges.penpow.dev/badges/supported/paper/compact-minimal.svg)](https://papermc.io/software/paper)
[![Folia](https://badges.penpow.dev/badges/supported/folia/compact-minimal.svg)](https://papermc.io/software/folia)
[![Purpur](https://badges.penpow.dev/badges/supported/purpur/compact-minimal.svg)](https://purpurmc.org/)

</div>

## 🌐 Overview

**IPViewer** is a plugin that provides a simple way to view players' IP addresses, helping with server management and detecting ban evasion or multi-accounts.

> **Note:** All technical settings and features described here are intended for the latest version of the plugin and may not be fully compatible with older versions.

## 🔍 Features

* View all players who have joined the server since the plugin was installed, along with their IP addresses and last seen time
* Filter and search player IPs easily
* Detect shared IPs across multiple accounts
* Helps track down ban evasion and suspicious activity

## 📜 Commands

Here are all the commands that can be used in the plugin:
* `/ipv help` - This command allows you to display all possible plugin commands
* `/ipv reload` - This command allows you to reload the plugin config
* `/ipv list [filters]` - This command allows you to browse the list of players with optional filters

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

You can also use spaces in filter values:
* Replace spaces with `+` - `country:United+States`
* Wrap the value in quotes - `state:"New York"`

When quotes are used, `+` characters are preserved as written.

### 📌 Example

```
/ipv list player:stev% ip:192.168.% time:7d page:3
```

This command shows page `3` of players whose names start with `stev`, IP starts with `192.168.`, and who have been seen in the last `7 days`.

## 📁 Configuration

The config file is generated on first launch and modify it to adjust output formats or other plugin settings. View default config [here](https://github.com/MusiJVR/IPViewer/blob/main/src/main/resources/config.yml).

All messages in the config use the [MiniMessage](https://docs.advntr.dev/minimessage/) format, which allows rich text formatting (colors, gradients, bold, etc.). You can preview and create formatted messages using the [MiniMessage Viewer](https://webui.advntr.dev/).

## ⚙️ Settings for developers or administrators

The plugin has permissions:

| **Permissions**            | **Meaning**                                  | **Default access** |
|----------------------------|----------------------------------------------|--------------------|
| `ipviewer.update_listener` | Permission to receive update `notifications` | `op`               |
| `ipviewer.use`             | Permission to use commands                   | `op`               |
| `ipviewer.help`            | Permission to use command `help`             | `op`               |
| `ipviewer.reload`          | Permission to use command `reload`           | `op`               |
| `ipviewer.list`            | Permission to use command `list`             | `op`               |

## ❗ Issues

Please leave messages about any errors you find [here](https://github.com/MusiJVR/IPViewer/issues) or on the [Discord](https://discord.gg/xY8WJt7VGr)

## 💬 Social Media

- Page on [Modrinth](https://modrinth.com/plugin/ipviewer)
- Page on [GitHub](https://github.com/MusiJVR/IPViewer)
- Page on [Discord](https://discord.gg/xY8WJt7VGr)

## 📊 Metrics

<div align="center">

[![Metrics](https://api.mousejava.com/api/plugin/metrics/35113)](https://bstats.org/plugin/bukkit/IPViewer/35113)

</div>
