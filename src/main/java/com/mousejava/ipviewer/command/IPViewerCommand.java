package com.mousejava.ipviewer.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mousejava.ipviewer.command.api.Cmd;
import com.mousejava.ipviewer.command.api.ICommand;
import com.mousejava.ipviewer.data.ListFilter;
import com.mousejava.ipviewer.database.repository.PlayersRepository;
import com.mousejava.ipviewer.database.api.DatabaseException;
import com.mousejava.ipviewer.data.PlayerProfile;
import com.mousejava.ipviewer.util.Scheduler;
import com.mousejava.ipviewer.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class IPViewerCommand implements ICommand {
    private static final int PAGE_SIZE = 10;
    private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+):(\"[^\"]+\"|\\S+)");
    private static final List<String> FILTER_NAMES = List.of("player", "ip", "country", "state", "time", "page");
    private static final DateTimeFormatter LAST_JOIN_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss - dd-MM-yyyy");

    private final JavaPlugin plugin;
    private final PlayersRepository playersRepository;

    public IPViewerCommand(JavaPlugin plugin, PlayersRepository playersRepository) {
        this.plugin = plugin;
        this.playersRepository = playersRepository;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> create() {
        return Cmd.senderCommand("ipviewer", "ipviewer.use", "messages.ipviewer.usage")
                .then(buildHelp())
                .then(buildReload())
                .then(buildList())
                .build();
    }

    @Override
    public String description() {
        return "This command allows you to use the plugin";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("ipv");
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildHelp() {
        return Cmd.senderCommand("help", "ipviewer.help", this::executeHelp);
    }

    private int executeHelp(CommandContext<CommandSourceStack> ctx, CommandSender sender) {
        if (sender instanceof Player) {
            MessageUtil.sendMiniMessageIfPresent(sender, "messages.ipviewer.help");
        } else {
            String prefix = plugin.getPluginMeta().getLoggerPrefix();
            MessageUtil.sendMiniMessageTransformed(sender, "messages.ipviewer.help",
                    msg -> Arrays.stream(msg.split("\n"))
                            .map(line -> "[%s] %s".formatted(prefix, line))
                            .collect(Collectors.joining("\n"))
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildReload() {
        return Cmd.senderCommand("reload", "ipviewer.reload", this::executeReload);
    }

    private int executeReload(CommandContext<CommandSourceStack> ctx, CommandSender sender) {
        plugin.reloadConfig();
        MessageUtil.sendMiniMessageIfPresent(sender, "messages.ipviewer.reload.reload_successfully");
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestListFilters(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        boolean quoted = false;
        int start = 0;
        for (int i = 0; i < remaining.length(); i++) {
            char character = remaining.charAt(i);
            if (character == '"') quoted = !quoted;
            if (Character.isWhitespace(character) && !quoted) start = i + 1;
        }

        if (quoted)
            return builder.buildFuture();

        SuggestionsBuilder current = builder.createOffset(builder.getStart() + start);
        String token = current.getRemainingLowerCase();
        for (String name : FILTER_NAMES) {
            String suggestion = name + ":";
            if (suggestion.startsWith(token))
                current.suggest(suggestion);
        }

        if (token.startsWith("page:")) {
            for (int page = 1; page <= 9; page++) {
                String suggestion = "page:" + page;
                if (suggestion.startsWith(token))
                    current.suggest(suggestion);
            }
        }

        return current.buildFuture();
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildList() {
        return Cmd.senderCommand("list", "ipviewer.list", this::executeListWithEmptyFilters)
                .then(Cmd.executesSender(Cmd.argument("filters", StringArgumentType.greedyString(), this::suggestListFilters),
                        this::executeListWithFilters));
    }

    private int executeListWithEmptyFilters(CommandContext<CommandSourceStack> ctx, CommandSender sender) {
        return executeList(sender, "");
    }

    private int executeListWithFilters(CommandContext<CommandSourceStack> ctx, CommandSender sender) {
        return executeList(sender, StringArgumentType.getString(ctx, "filters"));
    }

    private int executeList(CommandSender sender, String input) {
        Map<String, String> filters = new LinkedHashMap<>();
        Map<String, String> filterArguments = new LinkedHashMap<>();
        Integer filterPage = null;
        Matcher matcher = FILTER_PATTERN.matcher(input);
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            String value = matcher.group(2);
            if (value.startsWith("\"") && value.endsWith("\""))
                value = value.substring(1, value.length() - 1);
            else
                value = value.replace('+', ' ');

            if (key.equals("page")) {
                try {
                    filterPage = Integer.parseInt(value);
                    if (filterPage < 1)
                        throw new NumberFormatException();
                } catch (NumberFormatException exception) {
                    MessageUtil.sendMiniMessageIfPresent(sender, "messages.ipviewer.list.correct_page_number");
                    return Command.SINGLE_SUCCESS;
                }
            } else if (FILTER_NAMES.contains(key)) {
                filters.put(key, value);
                filterArguments.put(key, matcher.group());
            }
        }

        int page = filterPage == null ? 1 : filterPage;
        String remaining = matcher.replaceAll("").trim();
        if (!remaining.isEmpty()) {
            try {
                page = Integer.parseInt(remaining);
                if (page < 1) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                MessageUtil.sendMiniMessageIfPresent(sender, "messages.ipviewer.list.correct_page_number");
                return Command.SINGLE_SUCCESS;
            }
        }

        Timestamp joinedSince = null;
        if (filters.containsKey("time")) {
            String time = filters.get("time").toLowerCase(Locale.ROOT).trim();
            try {
                if (!time.matches("[1-9]\\d*[smhdw]"))
                    throw new IllegalArgumentException();

                long multiplier = switch (time.charAt(time.length() - 1)) {
                    case 's' -> 1_000L;
                    case 'm' -> 60_000L;
                    case 'h' -> 3_600_000L;
                    case 'd' -> 86_400_000L;
                    case 'w' -> 604_800_000L;
                    default -> throw new IllegalArgumentException();
                };

                long duration = Math.multiplyExact(Long.parseLong(time.substring(0, time.length() - 1)), multiplier);
                joinedSince = new Timestamp(Math.subtractExact(System.currentTimeMillis(), duration));
            } catch (IllegalArgumentException | ArithmeticException exception) {
                MessageUtil.sendMiniMessageIfPresent(sender, "messages.ipviewer.list.invalid_time_format");
                return Command.SINGLE_SUCCESS;
            }
        }

        ListFilter filter = new ListFilter(
                filters.get("player"),
                filters.get("ip"),
                filters.get("country"),
                filters.get("state"),
                joinedSince
        );
        int requestedPage = page;
        String arguments = String.join(" ", filterArguments.values());
        Scheduler.runAsync(() -> {
            try {
                List<PlayerProfile> players = playersRepository.findPage(filter, requestedPage, PAGE_SIZE);
                long total = players.isEmpty() ? 0 : playersRepository.count(filter);
                runForSender(sender, () -> sendList(sender, players, requestedPage, total, arguments));
            } catch (DatabaseException exception) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load the player list", exception);
                runForSender(sender, () -> MessageUtil.sendMiniMessageIfPresent(sender, "messages.ipviewer.list.error_loading_message"));
            }
        });
        return Command.SINGLE_SUCCESS;
    }

    private void runForSender(CommandSender sender, Runnable task) {
        if (sender instanceof Player player)
            Scheduler.runForEntity(player, task);
        else
            Scheduler.run(task);
    }

    private void sendList(CommandSender sender, List<PlayerProfile> players, int page, long total, String filterArguments) {
        if (players.isEmpty()) {
            MessageUtil.sendMiniMessageIfPresent(sender, "messages.ipviewer.list.no_entries_on_page");
            return;
        }

        Optional<String> playerTemplate = MessageUtil.optionalPlain("messages.ipviewer.list.player_ip_template");
        Optional<String> listTemplate = MessageUtil.optionalPlain("messages.ipviewer.list.list_player_template");
        if (playerTemplate.isEmpty() || listTemplate.isEmpty()) {
            MessageUtil.sendMiniMessageIfPresent(sender, "messages.ipviewer.list.error_loading_message");
            return;
        }

        MiniMessage mini = MiniMessage.miniMessage();
        Component playerLines = Component.empty();
        long number = ((long) page - 1) * PAGE_SIZE + 1;
        for (PlayerProfile player : players) {
            String time = LAST_JOIN_FORMAT.format(player.lastJoin().toInstant().atZone(ZoneId.systemDefault()));
            Component line = mini.deserialize(playerTemplate.get().replace("<time>", time),
                    Placeholder.component("page", Component.text(number++)),
                    Placeholder.component("player", copyable(player.nickname(), "Unknown")),
                    Placeholder.component("ip", copyable(player.ip(), "-")),
                    Placeholder.component("country", copyable(player.country(), "-")),
                    Placeholder.component("state", copyable(player.state(), "-")));
            playerLines = playerLines.append(line);
        }

        long totalPages = Math.max(1, total / PAGE_SIZE + (total % PAGE_SIZE == 0 ? 0 : 1));
        String prefix = filterArguments.isEmpty() ? "" : filterArguments + " ";
        String previous = prefix + Math.max(1, page - 1);
        String next = prefix + Math.min(totalPages, (long) page + 1);
        MessageUtil.sendMiniMessageTransformed(sender, "messages.ipviewer.list.list_player_template",
                template -> replacePagePlaceholder(replacePagePlaceholder(template, "<prev_page>", previous), "<next_page>", next),
                TagResolver.resolver(
                        Placeholder.component("players", playerLines),
                        Placeholder.component("current", Component.text(page)),
                        Placeholder.component("total", Component.text(totalPages))
                )
        );
    }

    private Component copyable(String value, String fallback) {
        String text = Objects.toString(value, fallback);
        return Component.text(text).clickEvent(ClickEvent.copyToClipboard(text));
    }

    private String replacePagePlaceholder(String template, String placeholder, String value) {
        StringBuilder result = new StringBuilder();
        boolean inTag = false;
        char quote = 0;
        for (int i = 0; i < template.length(); i++) {
            if (template.startsWith(placeholder, i)) {
                String replacement = quote == 0 ? MiniMessage.miniMessage().escapeTags(value)
                        : value.replace("\\", "\\\\").replace(String.valueOf(quote), "\\" + quote);
                result.append(replacement);
                i += placeholder.length() - 1;
                continue;
            }

            char character = template.charAt(i);
            result.append(character);
            if (inTag && character == '\\' && i + 1 < template.length())
                result.append(template.charAt(++i));
            else if (quote != 0)
                if (character == quote) quote = 0;
            else if (inTag && (character == '\'' || character == '"'))
                quote = character;
            else if (character == '<')
                inTag = true;
            else if (character == '>')
                inTag = false;
        }
        return result.toString();
    }
}
