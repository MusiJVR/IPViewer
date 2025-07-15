package org.mousejava.ipviewer.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.mousejava.ipviewer.utils.MessageUtils;

import java.util.List;

public class ReloadSubCommand implements TabCompletableSubCommand {
    private final JavaPlugin plugin;

    public ReloadSubCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return CommandName.RELOAD.get();
    }

    @Override
    public void sendDescription(CommandSender sender) {
        MessageUtils.sendMiniMessageIfPresent(sender, "messages.description.reload");
    }

    @Override
    public void sendUsage(CommandSender sender) {
        MessageUtils.sendMiniMessageIfPresent(sender, "messages.usage.reload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 0) {
            MessageUtils.sendMiniMessageIfPresent(sender, "messages.error");
            sendUsage(sender);
            return;
        }

        plugin.reloadConfig();
        MessageUtils.sendMiniMessageIfPresent(sender, "messages.config_reload_successfully");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
