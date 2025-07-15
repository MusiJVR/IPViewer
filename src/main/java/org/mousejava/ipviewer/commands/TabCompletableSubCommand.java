package org.mousejava.ipviewer.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface TabCompletableSubCommand extends SubCommand {
    List<String> onTabComplete(CommandSender sender, String[] args);
}
