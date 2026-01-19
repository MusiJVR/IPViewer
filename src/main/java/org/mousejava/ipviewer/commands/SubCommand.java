package org.mousejava.ipviewer.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    String getName();
    void sendDescription(CommandSender sender);
    void sendUsage(CommandSender sender);
    void execute(CommandSender sender, String[] args);
    default List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
