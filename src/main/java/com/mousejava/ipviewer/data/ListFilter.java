package com.mousejava.ipviewer.data;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public record ListFilter(String player, String ip, String country, String state, Timestamp joinedSince) {
    public Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (player != null) parameters.put("player", player);
        if (ip != null) parameters.put("ip", ip);
        if (country != null) parameters.put("country", country);
        if (state != null) parameters.put("state", state);
        if (joinedSince != null) parameters.put("time", joinedSince);
        return parameters;
    }
}
