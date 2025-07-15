package org.mousejava.ipviewer.commands;

import org.bukkit.command.CommandSender;

public interface SubCommand {
    String getName();
    void sendDescription(CommandSender sender);
    void sendUsage(CommandSender sender);
    void execute(CommandSender sender, String[] args);
}
