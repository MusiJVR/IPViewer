package org.mousejava.ipviewer.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.mousejava.ipviewer.utils.DatabaseDriver;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class UpdateDatabaseHandler implements Listener {
    private final JavaPlugin plugin;
    private final DatabaseDriver dbDriver;

    public UpdateDatabaseHandler(JavaPlugin plugin, DatabaseDriver dbDriver) {
        this.plugin = plugin;
        this.dbDriver = dbDriver;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String nickname = player.getName();
        String ip = player.getAddress().getAddress().getHostAddress();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
                    long joinTimestamp = System.currentTimeMillis();

                    List<Map<String, Object>> result = dbDriver.selectData("uuid", "players", "WHERE uuid = ?", uuid);

                    Map<String, Object> data = new HashMap<>();
                    data.put("nickname", nickname);
                    data.put("ip", ip);
                    data.put("country", country);
                    data.put("state", state);
                    data.put("last_join", joinTimestamp);

                    if (result.isEmpty()) {
                        data.put("uuid", uuid);
                        dbDriver.insertData("players", data);
                    } else {
                        dbDriver.updateData("players", data, "uuid = ?", uuid);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
