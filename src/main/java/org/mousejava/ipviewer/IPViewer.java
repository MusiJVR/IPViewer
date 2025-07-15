package org.mousejava.ipviewer;

import org.bukkit.plugin.java.JavaPlugin;
import org.mousejava.ipviewer.commands.IPViewerCommand;
import org.mousejava.ipviewer.commands.IPViewerTabCompleter;
import org.mousejava.ipviewer.handlers.UpdateDatabaseHandler;
import org.mousejava.ipviewer.utils.DatabaseDriver;

public final class IPViewer extends JavaPlugin {
    private static IPViewer instance;
    private DatabaseDriver dbDriver;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dbDriver = new DatabaseDriver("jdbc:sqlite:" + getDataFolder() + "/ipviewer.db");
        dbDriver.createTable("players", "uuid TEXT PRIMARY KEY", "nickname TEXT NOT NULL", "ip TEXT NOT NULL", "country TEXT", "state TEXT", "last_join INTEGER");

        getServer().getPluginManager().registerEvents(new UpdateDatabaseHandler(this, dbDriver), this);
        getServer().getPluginCommand("ipviewer").setExecutor(new IPViewerCommand(this, dbDriver));
        getServer().getPluginCommand("ipviewer").setTabCompleter(new IPViewerTabCompleter(IPViewerCommand.getSubCommands()));
    }

    @Override
    public void onDisable() {
        dbDriver.closeConnection();
    }

    public static IPViewer getInstance() {
        return instance;
    }
}
