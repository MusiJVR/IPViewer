package org.mousejava.ipviewer.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IPViewerTabCompleter implements TabCompleter {
    private final Map<String, SubCommand> subCommands;

    public IPViewerTabCompleter(Map<String, SubCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String name : subCommands.keySet()) {
                if (name.startsWith(prefix)) {
                    suggestions.add(name);
                }
            }
            return suggestions;
        }

        if (args.length >= 2) {
            String subName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subName);
            if (subCommand instanceof TabCompletableSubCommand completable) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return completable.onTabComplete(sender, subArgs);
            }
        }

        return Collections.emptyList();
    }
}
