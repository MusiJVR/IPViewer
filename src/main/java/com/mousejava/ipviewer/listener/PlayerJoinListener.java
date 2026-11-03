package com.mousejava.ipviewer.listener;

import com.mousejava.ipviewer.data.GeoLocation;
import com.mousejava.ipviewer.database.repository.PlayersRepository;
import com.mousejava.ipviewer.data.PlayerProfile;
import com.mousejava.ipviewer.service.GeoLocationScriptService;
import com.mousejava.ipviewer.util.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayersRepository playersRepository;
    private final GeoLocationScriptService scriptService;

    public PlayerJoinListener(JavaPlugin plugin, PlayersRepository playersRepository, GeoLocationScriptService scriptService) {
        this.plugin = plugin;
        this.playersRepository = playersRepository;
        this.scriptService = scriptService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String nickname = player.getName();
        String ip = player.getAddress().getAddress().getHostAddress();
        String scriptName = scriptService.getSelectedScript();

        Scheduler.runAsync(() -> {
            try {
                GeoLocation location = scriptService.lookup(scriptName, ip);
                Timestamp joinTimestamp = Timestamp.from(Instant.now());
                PlayerProfile playerProfile = new PlayerProfile(uuid, nickname, ip, location.country(), location.state(), joinTimestamp);
                playersRepository.findByUuid(uuid).ifPresentOrElse(
                        ignore -> playersRepository.update(playerProfile),
                        () -> playersRepository.register(playerProfile)
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "GeoIP lookup failed for %s using script %s".formatted(nickname, scriptName), e);
            }
        });
    }
}
