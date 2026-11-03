package com.mousejava.ipviewer;

import com.mousejava.ipviewer.command.IPViewerCommand;
import com.mousejava.ipviewer.command.api.ICommand;
import com.mousejava.ipviewer.database.api.DatabaseConfig;
import com.mousejava.ipviewer.database.api.DatabaseManager;
import com.mousejava.ipviewer.database.api.SchemaRepository;
import com.mousejava.ipviewer.database.dialect.*;
import com.mousejava.ipviewer.database.repository.*;
import com.mousejava.ipviewer.listener.*;
import com.mousejava.ipviewer.util.*;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class IPViewer extends JavaPlugin {
    private static IPViewer instance;
    private DatabaseManager<SqlDialect> database;
    private PlayersRepository playersRepository;

    public static IPViewer getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Scheduler.init(this);
        MessageUtil.init(this);

        database = initializeDatabase();

        playersRepository = new PlayersRepository(database);

        List.of(
                playersRepository
        ).forEach(SchemaRepository::initializeSchema);

        registerListeners();
        registerCommands();
    }

    @Override
    public void onDisable() {
        if (database != null && database.isRunning())
            database.close();
    }

    private DatabaseManager<SqlDialect> initializeDatabase() {
        DatabaseConfig config = DatabaseConfig.from(getConfig(), getDataFolder());
        SqlDialect dialect = switch (config.type()) {
            case SQLITE -> new SqliteDialect();
            case MYSQL -> new MySqlDialect();
        };

        return new DatabaseManager<>(getName() + "Pool", config, dialect);
    }

    private void registerListeners() {
        List<Listener> listeners = List.of(
                new PlayerJoinListener(this, playersRepository)
        );

        PluginManager pluginManager = getServer().getPluginManager();
        listeners.forEach(listener -> pluginManager.registerEvents(listener, this));
    }

    private void registerCommands() {
        List<ICommand> commands = List.of(
                new IPViewerCommand(this, playersRepository)
        );

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            commands.forEach(command -> registrar.register(command.create(), command.description(), command.aliases()));
        });
    }
}
