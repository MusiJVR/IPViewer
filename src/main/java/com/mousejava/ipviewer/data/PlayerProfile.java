package com.mousejava.ipviewer.data;

import java.sql.Timestamp;
import java.util.UUID;

public record PlayerProfile(UUID uuid, String nickname, String ip, String country, String state, Timestamp lastJoin) {
}
