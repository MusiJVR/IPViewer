package com.mousejava.ipviewer.database.repository;

import com.mousejava.ipviewer.data.ListFilter;
import com.mousejava.ipviewer.database.api.DatabaseManager;
import com.mousejava.ipviewer.database.api.SchemaRepository;
import com.mousejava.ipviewer.database.dialect.SqlDialect;
import com.mousejava.ipviewer.data.PlayerProfile;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayersRepository implements SchemaRepository {
    private final DatabaseManager<SqlDialect> database;

    public PlayersRepository(DatabaseManager<SqlDialect> database) {
        this.database = database;
    }

    @Override
    public void initializeSchema() {
        database.dialect().playersSchema().forEach(database::execute);
    }

    public void register(PlayerProfile playerProfile) {
        database.execute(database.dialect().playersInsert(), playerProfile.uuid().toString(), playerProfile.nickname(), playerProfile.ip(), playerProfile.country(), playerProfile.state(), playerProfile.lastJoin());
    }

    public void update(PlayerProfile playerProfile) {
        database.execute(database.dialect().playersUpdate(), playerProfile.nickname(), playerProfile.ip(), playerProfile.country(), playerProfile.state(), playerProfile.lastJoin(), playerProfile.uuid().toString());
    }

    public Optional<PlayerProfile> findByUuid(UUID uuid) {
        return database.queryOne(database.dialect().playersSelectByUuid(), this::map, uuid.toString());
    }

    public List<PlayerProfile> findPage(ListFilter filter, int page, int pageSize) {
        if (page < 1 || pageSize < 1)
            throw new IllegalArgumentException("Page and page size must be positive");

        Map<String, Object> filters = filter.parameters();
        List<Object> parameters = new ArrayList<>(filters.values());
        parameters.add(pageSize);
        parameters.add(((long) page - 1) * pageSize);
        return database.query(database.dialect().playersSelectPage(filters.keySet()), this::map, parameters.toArray());
    }

    public long count(ListFilter filter) {
        Map<String, Object> filters = filter.parameters();
        return database.queryOne(database.dialect().playersCount(filters.keySet()), row -> row.getLong(1),
                filters.values().toArray()).orElse(0L);
    }

    private PlayerProfile map(ResultSet row) throws SQLException {
        return new PlayerProfile(
                UUID.fromString(row.getString("uuid")),
                row.getString("nickname"),
                row.getString("ip"),
                row.getString("country"),
                row.getString("state"),
                row.getTimestamp("last_join")
        );
    }
}
