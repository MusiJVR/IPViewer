package org.mousejava.ipviewer.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.mousejava.ipviewer.utils.DatabaseDriver;
import org.mousejava.ipviewer.utils.MessageUtils;

import java.util.*;

public class IPViewerCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final DatabaseDriver dbDriver;
    public static final Map<String, SubCommand> subCommands = new HashMap<>();

    public IPViewerCommand(JavaPlugin plugin, DatabaseDriver dbDriver) {
        this.plugin = plugin;
        this.dbDriver = dbDriver;

        registerSubCommand(new HelpSubCommand(this.plugin));
        registerSubCommand(new ReloadSubCommand(this.plugin));
        registerSubCommand(new ListSubCommand(this.dbDriver));
    }

    private void registerSubCommand(SubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            subCommands.get(CommandName.HELP.get()).execute(sender, args);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            MessageUtils.sendMiniMessageIfPresent(sender, "messages.unknown_subcommand");
            subCommands.get(CommandName.HELP.get()).execute(sender, args);
            return true;
        }

        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        subCommand.execute(sender, subArgs);
        return true;
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
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);
            return subCommand.onTabComplete(sender, subArgs);
        }

        return List.of();
    }

    public static Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }
}
