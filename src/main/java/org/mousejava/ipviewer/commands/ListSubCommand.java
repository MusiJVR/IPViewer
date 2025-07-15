package org.mousejava.ipviewer.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.mousejava.ipviewer.utils.DatabaseDriver;
import org.mousejava.ipviewer.utils.MessageUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ListSubCommand implements TabCompletableSubCommand {
    private final DatabaseDriver dbDriver;

    private static final int PAGE_SIZE = 10;

    public ListSubCommand(DatabaseDriver dbDriver) {
        this.dbDriver = dbDriver;
    }

    @Override
    public String getName() {
        return CommandName.LIST.get();
    }

    @Override
    public void sendDescription(CommandSender sender) {
        MessageUtils.sendMiniMessageIfPresent(sender, "messages.description.list");
    }

    @Override
    public void sendUsage(CommandSender sender) {
        MessageUtils.sendMiniMessageIfPresent(sender, "messages.usage.list");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Map<String, String> filters = new HashMap<>();
        int page = 1;

        for (String arg : args) {
            if (arg.contains(":")) {
                String[] split = arg.split(":", 2);
                if (split.length == 2) {
                    filters.put(split[0].toLowerCase(), split[1]);
                }
            } else {
                try {
                    page = Integer.parseInt(arg);
                    if (page <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    MessageUtils.sendMiniMessageIfPresent(sender, "messages.correct_page_number");
                    return;
                }
            }
        }

        List<Object> filterParams = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (List.of("player", "ip", "country", "state", "time").contains(key)) {
                String column = switch (key) {
                    case "player" -> "nickname";
                    case "ip" -> "ip";
                    case "country" -> "country";
                    case "state" -> "state";
                    case "time" -> "last_join";
                    default -> null;
                };
                if (column != null) {
                    if (whereClause.length() > 0) whereClause.append(" AND ");

                    if (key.equals("time")) {
                        Map<String, Long> timeUnits = Map.of(
                                "s", 1000L,
                                "m", 60_000L,
                                "h", 3_600_000L,
                                "d", 86_400_000L,
                                "w", 604_800_000L
                        );

                        String raw = value.toLowerCase().trim();

                        if (raw.matches("^[1-9]\\d*[smhdw]$")) {
                            String numberPart = raw.substring(0, raw.length() - 1);
                            String unit = raw.substring(raw.length() - 1);

                            try {
                                Long multiplier = timeUnits.get(unit);
                                if (multiplier == null) {
                                    MessageUtils.sendMiniMessageIfPresent(sender, "messages.invalid_time_format");
                                    return;
                                }
                                long number = Long.parseLong(numberPart);
                                long now = System.currentTimeMillis();
                                long targetTimestamp = now - number * multiplier;

                                whereClause.append(column).append(" >= ?");
                                filterParams.add(targetTimestamp);
                            } catch (NumberFormatException e) {
                                MessageUtils.sendMiniMessageIfPresent(sender, "messages.invalid_time_format");
                                return;
                            }
                        } else {
                            MessageUtils.sendMiniMessageIfPresent(sender, "messages.invalid_time_format");
                            return;
                        }

                        continue;
                    }

                    whereClause.append(column).append(" LIKE ?");
                    filterParams.add(value);
                }
            }
        }

        String where = whereClause.length() > 0 ? "WHERE " + whereClause : "";
        int offset = (page - 1) * PAGE_SIZE;

        List<Object> queryParams = new ArrayList<>(filterParams);
        queryParams.add(PAGE_SIZE);
        queryParams.add(offset);
        List<Object> countParams = new ArrayList<>(filterParams);

        List<Map<String, Object>> players = dbDriver.selectData(
                "nickname, ip, country, state, last_join",
                "players",
                "%s ORDER BY nickname ASC LIMIT ? OFFSET ?".formatted(where),
                queryParams.toArray()
        );

        if (players.isEmpty()) {
            MessageUtils.sendMiniMessageIfPresent(sender, "messages.no_entries_on_page");
            return;
        }

        Optional<Component> playerTemplateOpt = MessageUtils.optionalMiniMessage("messages.player_ip_template");
        Optional<Component> listTemplateOpt = MessageUtils.optionalMiniMessage("messages.list_player_template");

        if (playerTemplateOpt.isEmpty() || listTemplateOpt.isEmpty()) {
            MessageUtils.sendMiniMessageIfPresent(sender, "messages.error_loading_message");
            return;
        }

        Component playerTemplate = playerTemplateOpt.get();
        String listTemplateRaw = MiniMessage.miniMessage().serialize(listTemplateOpt.get());
        Component playersBuilder = Component.empty();
        int number = offset + 1;

        for (Map<String, Object> player : players) {
            int playerNumber = number++;
            String nickname = Objects.toString(player.get("nickname"), "Unknown");
            String ip = Objects.toString(player.get("ip"), "-");
            String country = Objects.toString(player.get("country"), "-");
            String state = Objects.toString(player.get("state"), "-");
            String formattedLastJoin;
            if (player.get("last_join") instanceof Number num) {
                long timestamp = num.longValue();
                Instant instant = Instant.ofEpochMilli(timestamp);
                ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());
                formattedLastJoin = DateTimeFormatter.ofPattern("HH:mm:ss - dd-MM-yyyy").format(dateTime);
            } else formattedLastJoin = "00:00:00 - 00-00-0000";

            Component playerLine = playerTemplate
                    .replaceText(builder -> builder.matchLiteral("%number%")
                            .replacement(Component.text(playerNumber)))
                    .replaceText(builder -> builder.matchLiteral("%time%")
                            .replacement(Component.text(formattedLastJoin)))
                    .replaceText(builder -> builder.matchLiteral("%time_sign%")
                            .replacement(MessageUtils.optionalMiniMessage("messages.time_sign").orElse(Component.text("⌚"))
                                    .clickEvent(ClickEvent.copyToClipboard(formattedLastJoin))))
                    .replaceText(builder -> builder.matchLiteral("%player%")
                            .replacement(Component.text(nickname)
                                    .clickEvent(ClickEvent.copyToClipboard(nickname))))
                    .replaceText(builder -> builder.matchLiteral("%ip%")
                            .replacement(Component.text(ip)
                                    .clickEvent(ClickEvent.copyToClipboard(ip))))
                    .replaceText(builder -> builder.matchLiteral("%country%")
                            .replacement(Component.text(country)
                                    .clickEvent(ClickEvent.copyToClipboard(country))))
                    .replaceText(builder -> builder.matchLiteral("%state%")
                            .replacement(Component.text(state)
                                    .clickEvent(ClickEvent.copyToClipboard(state))));

            playersBuilder = playersBuilder.append(playerLine);
        }

        List<Map<String, Object>> resultTotalPage = dbDriver.selectData("COUNT(*) AS total", "players", where, countParams.toArray());
        int totalPages = 1;
        if (!resultTotalPage.isEmpty()) {
            Object countValue = resultTotalPage.get(0).get("total");
            if (countValue instanceof Number num) {
                int total = num.intValue();
                totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
            }
        }

        Component finalPlayersBuilder = playersBuilder;
        int finalPage = page;
        int finalTotalPages = totalPages;

        StringBuilder filterString = new StringBuilder();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            filterString.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
        }
        String filtersRaw = filterString.toString().trim();

        listTemplateRaw = listTemplateRaw
                .replace("%prev_page%", filtersRaw + " " + (finalPage <= 1 ? "1" : finalPage - 1))
                .replace("%next_page%", filtersRaw + " " + (finalPage >= finalTotalPages ? finalTotalPages : finalPage + 1));
        Component listTemplate = MiniMessage.miniMessage().deserialize(listTemplateRaw)
                .replaceText(builder -> builder.matchLiteral("%players%").replacement(finalPlayersBuilder))
                .replaceText(builder -> builder.matchLiteral("%current%").replacement(Component.text(finalPage)))
                .replaceText(builder -> builder.matchLiteral("%total%").replacement(Component.text(finalTotalPages)));

        sender.sendMessage(listTemplate);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) return List.of();

        String last = args[args.length - 1].toLowerCase();

        List<String> completions = List.of("player:", "ip:", "country:", "state:", "time:", "page:");

        if (!last.contains(":")) {
            return completions.stream().filter(c -> c.startsWith(last)).toList();
        }

        String[] split = last.split(":", 2);
        if (split.length == 2 && split[0].equals("page")) {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
        }

        return List.of();
    }
}
