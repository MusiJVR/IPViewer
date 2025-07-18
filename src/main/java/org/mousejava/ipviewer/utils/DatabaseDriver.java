package org.mousejava.ipviewer.utils;

import org.mousejava.ipviewer.IPViewer;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseDriver {
    private Connection connection;

    public DatabaseDriver(String url) {
        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            IPViewer.getInstance().getLogger().log(Level.WARNING, "Error connecting to database", e);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            IPViewer.getInstance().getLogger().log(Level.WARNING, "Error closing database connection", e);
        }
    }

    public void createTable(String table, String... columns) {
        StringBuilder query = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %s (", table));

        for (int i = 0; i < columns.length; i++) {
            query.append(columns[i]);
            if (i < columns.length - 1) query.append(", ");
        }

        try (PreparedStatement statement = connection.prepareStatement(query.append(");").toString())) {
            statement.execute();
        } catch (SQLException e) {
            IPViewer.getInstance().getLogger().log(Level.WARNING, "Error creating table in database", e);
        }
    }

    public List<Map<String, Object>> selectData(String columns, String table, String condition, Object... parameters) {
        StringBuilder query = new StringBuilder(String.format("SELECT %s FROM %s", columns, table));

        if (condition != null && !condition.trim().isEmpty()) query.append(" ").append(condition).append(";");

        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);

            try (ResultSet resultSet = statement.executeQuery()) {
                return convertResultSetToList(resultSet);
            }
        } catch (SQLException e) {
            IPViewer.getInstance().getLogger().log(Level.WARNING, "Error executing SQL query", e);
            return new ArrayList<>();
        }
    }

    public void insertData(String table, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) throw new IllegalArgumentException("Parameters cannot be null or empty.");

        StringBuilder query = new StringBuilder(String.format("INSERT INTO %s (", table));

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");
        for (String column : parameters.keySet()) {
            columns.add(column);
            placeholders.add("?");
        }

        query.append(columns).append(") VALUES (").append(placeholders).append(");");

        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            int index = 1;
            for (Object value : parameters.values()) statement.setObject(index++, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            IPViewer.getInstance().getLogger().log(Level.WARNING, "Error executing SQL query", e);
        }
    }

    public void updateData(String table, Map<String, Object> parameters, String condition, Object... conditionParameters) {
        if (parameters == null || parameters.isEmpty()) throw new IllegalArgumentException("Parameters cannot be null or empty.");

        StringBuilder query = new StringBuilder(String.format("UPDATE %s SET ", table));

        StringJoiner columns = new StringJoiner(", ");
        for (String column : parameters.keySet()) {
            columns.add(column + " = ?");
        }

        query.append(columns);

        if (condition != null && !condition.trim().isEmpty()) query.append(" WHERE ").append(condition).append(";");

        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            int index = 1;
            for (Object value : parameters.values()) statement.setObject(index++, value);
            for (Object conditionValue : conditionParameters) statement.setObject(index++, conditionValue);
            statement.executeUpdate();
        } catch (SQLException e) {
            IPViewer.getInstance().getLogger().log(Level.WARNING, "Error executing SQL query", e);
        }
    }

    public void deleteData(String table, String condition, Object... parameters) {
        if (condition == null || condition.trim().isEmpty()) throw new IllegalArgumentException("Condition cannot be null or empty.");

        StringBuilder query = new StringBuilder(String.format("DELETE FROM %s WHERE %s;", table, condition));

        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
            statement.executeUpdate();
        } catch (SQLException e) {
            IPViewer.getInstance().getLogger().log(Level.WARNING, "Error executing SQL query", e);
        }
    }

    private static List<Map<String, Object>> convertResultSetToList(ResultSet resultSet) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            while (resultSet.next()) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) map.put(resultSetMetaData.getColumnName(i), resultSet.getObject(i));
                result.add(map);
            }
        } catch (SQLException e) {
            IPViewer.getInstance().getLogger().log(Level.WARNING, "Error converting SQL query", e);
        }

        return result;
    }
}
