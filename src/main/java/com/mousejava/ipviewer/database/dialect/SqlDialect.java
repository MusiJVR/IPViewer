package com.mousejava.ipviewer.database.dialect;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface SqlDialect {
    List<String> playersSchema();
    String playersInsert();
    String playersUpdate();
    String playersSelectByUuid();
    String playersSelectPage(Set<String> filters);
    String playersCount(Set<String> filters);

    default String playersWhere(Set<String> filters) {
        if (filters.isEmpty()) return "";
        return "WHERE " + filters.stream().map(key -> switch (key) {
            case "player" -> "nickname LIKE ?";
            case "ip" -> "ip LIKE ?";
            case "country" -> "country LIKE ?";
            case "state" -> "state LIKE ?";
            case "time" -> "last_join >= ?";
            default -> throw new IllegalArgumentException("Unknown player filter: " + key);
        }).collect(Collectors.joining(" AND "));
    }
}
