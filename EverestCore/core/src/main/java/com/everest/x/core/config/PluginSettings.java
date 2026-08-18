package com.everest.x.core.config;

import com.everest.x.api.ServerType;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginSettings {

    public enum StorageType {
        FILE,
        MYSQL
    }

    private final String serverId;
    private final ServerType serverType;
    private final StorageType storageType;
    private final long startingCoins;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final int mysqlPoolSize;

    public PluginSettings(FileConfiguration config) {
        this.serverId = config.getString("server.id", "lobby-1");
        this.serverType = ServerType.fromConfig(config.getString("server.type", "LOBBY"));
        this.storageType = parseStorage(config.getString("storage.type", "FILE"));
        this.startingCoins = config.getLong("coins.starting", 0L);
        this.mysqlHost = config.getString("storage.mysql.host", "127.0.0.1");
        this.mysqlPort = config.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = config.getString("storage.mysql.database", "everest");
        this.mysqlUsername = config.getString("storage.mysql.username", "everest");
        this.mysqlPassword = config.getString("storage.mysql.password", "");
        this.mysqlPoolSize = config.getInt("storage.mysql.pool-size", 10);
    }

    private static StorageType parseStorage(String raw) {
        if (raw != null && raw.equalsIgnoreCase("MYSQL")) {
            return StorageType.MYSQL;
        }
        return StorageType.FILE;
    }

    public String serverId() {
        return serverId;
    }

    public ServerType serverType() {
        return serverType;
    }

    public StorageType storageType() {
        return storageType;
    }

    public long startingCoins() {
        return startingCoins;
    }

    public String mysqlHost() {
        return mysqlHost;
    }

    public int mysqlPort() {
        return mysqlPort;
    }

    public String mysqlDatabase() {
        return mysqlDatabase;
    }

    public String mysqlUsername() {
        return mysqlUsername;
    }

    public String mysqlPassword() {
        return mysqlPassword;
    }

    public int mysqlPoolSize() {
        return mysqlPoolSize;
    }
}
