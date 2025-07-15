package org.mousejava.ipviewer.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.mousejava.ipviewer.utils.MessageUtils;

import java.util.List;

public class HelpSubCommand implements TabCompletableSubCommand {
    private final JavaPlugin plugin;

    public HelpSubCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return CommandName.HELP.get();
    }

    @Override
    public void sendDescription(CommandSender sender) {
        MessageUtils.sendMiniMessageIfPresent(sender, "messages.description.help");
    }

    @Override
    public void sendUsage(CommandSender sender) {
        MessageUtils.sendMiniMessageIfPresent(sender, "messages.usage.help");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 0) {
            MessageUtils.sendMiniMessageIfPresent(sender, "messages.error");
            sendUsage(sender);
            return;
        }

        MessageUtils.sendMiniMessageComponent(sender, "messages.help_message",
                component -> component
                        .replaceText(builder -> builder.matchLiteral("%version%")
                                .replacement(Component.text(plugin.getDescription().getVersion())
                                        .clickEvent(ClickEvent.copyToClipboard(plugin.getDescription().getVersion())))));

        for (SubCommand cmd : IPViewerCommand.getSubCommands().values()) {
            MessageUtils.sendMiniMessageComponent(sender, "messages.command_template",
                    component -> component
                            .replaceText(builder -> builder.matchLiteral("%command%")
                                    .replacement(Component.text(cmd.getName()))));
            cmd.sendDescription(sender);
            cmd.sendUsage(sender);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
