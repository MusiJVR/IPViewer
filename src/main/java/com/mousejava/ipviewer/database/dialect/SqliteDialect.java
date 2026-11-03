package com.mousejava.ipviewer.database.dialect;

import java.util.List;
import java.util.Set;

public final class SqliteDialect implements SqlDialect {
    @Override
    public List<String> playersSchema() {
        return List.of("""
                CREATE TABLE IF NOT EXISTS ipv_players (
                    uuid CHAR(36) NOT NULL,
                    nickname VARCHAR(16) NOT NULL,
                    ip VARCHAR(45) NOT NULL,
                    country VARCHAR(255) NOT NULL,
                    state VARCHAR(255) NOT NULL,
                    last_join TIMESTAMP NOT NULL,
                    PRIMARY KEY (uuid),
                    CONSTRAINT uk_ipv_players_nickname UNIQUE (nickname)
                )
                """);
    }

    @Override
    public String playersInsert() {
        return """
                INSERT INTO ipv_players (uuid, nickname, ip, country, state, last_join)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
    }

    @Override
    public String playersUpdate() {
        return """
                UPDATE ipv_players
                SET nickname = ?, ip = ?, country = ?, state = ?, last_join = ?
                WHERE uuid = ?
                """;
    }

    @Override
    public String playersSelectByUuid() {
        return """
                SELECT uuid, nickname, ip, country, state, last_join
                FROM ipv_players
                WHERE uuid = ?
                """;
    }

    @Override
    public String playersSelectPage(Set<String> filters) {
        return """
                SELECT uuid, nickname, ip, country, state, last_join
                FROM ipv_players
                %s ORDER BY nickname ASC LIMIT ? OFFSET ?
                """.formatted(playersWhere(filters));
    }

    @Override
    public String playersCount(Set<String> filters) {
        return "SELECT COUNT(*) FROM ipv_players " + playersWhere(filters);
    }
}
