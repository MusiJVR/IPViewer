package com.mousejava.ipviewer.listener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mousejava.ipviewer.database.repository.PlayersRepository;
import com.mousejava.ipviewer.data.PlayerProfile;
import com.mousejava.ipviewer.util.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayersRepository playersRepository;

    public PlayerJoinListener(JavaPlugin plugin, PlayersRepository playersRepository) {
        this.plugin = plugin;
        this.playersRepository = playersRepository;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String nickname = player.getName();
        String ip = player.getAddress().getAddress().getHostAddress();

        Scheduler.runAsync(() -> {
            try {
                URL url = new URL("http://ip-api.com/json/%s?fields=country,regionName,status,message".formatted(ip));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (InputStream input = connection.getInputStream();
                     InputStreamReader reader = new InputStreamReader(input)) {

                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                    if (!"success".equalsIgnoreCase(json.get("status").getAsString())) {
                        plugin.getLogger().log(Level.WARNING, "GeoIP %s error: %s".formatted(nickname, json.get("message").getAsString()));
                        return;
                    }

                    String country = json.get("country").getAsString();
                    String state = json.get("regionName").getAsString();
                    Timestamp joinTimestamp = Timestamp.from(Instant.now());

                    PlayerProfile playerProfile = new PlayerProfile(uuid, nickname, ip, country, state, joinTimestamp);
                    playersRepository.findByUuid(uuid).ifPresentOrElse(
                            ignore -> playersRepository.update(playerProfile),
                            () -> playersRepository.register(playerProfile)
                    );
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Request error", e);
            }
        });
    }
}
