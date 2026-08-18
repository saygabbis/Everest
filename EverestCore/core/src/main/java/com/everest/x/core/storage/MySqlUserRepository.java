package com.everest.x.core.storage;

import com.everest.x.core.config.PluginSettings;
import com.everest.x.core.user.User;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public final class MySqlUserRepository implements UserRepository {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS everest_users (
              uuid VARCHAR(36) NOT NULL PRIMARY KEY,
              name VARCHAR(16) NOT NULL,
              coins BIGINT NOT NULL DEFAULT 0,
              first_join BIGINT NOT NULL,
              last_join BIGINT NOT NULL
            )
            """;

    private static final String SELECT = """
            SELECT name, coins, first_join, last_join FROM everest_users WHERE uuid = ?
            """;

    private static final String UPSERT = """
            INSERT INTO everest_users (uuid, name, coins, first_join, last_join)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE name = VALUES(name), coins = VALUES(coins), last_join = VALUES(last_join)
            """;

    private final HikariDataSource dataSource;

    public MySqlUserRepository(PluginSettings settings) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://" + settings.mysqlHost() + ":" + settings.mysqlPort()
                + "/" + settings.mysqlDatabase()
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
        hikari.setUsername(settings.mysqlUsername());
        hikari.setPassword(settings.mysqlPassword());
        hikari.setMaximumPoolSize(settings.mysqlPoolSize());
        hikari.setPoolName("EverestCore");
        hikari.setConnectionTimeout(10_000);
        this.dataSource = new HikariDataSource(hikari);
    }

    public void init() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        }
    }

    @Override
    public User loadOrCreate(UUID uuid, String name, long startingCoins) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement select = connection.prepareStatement(SELECT)) {
            select.setString(1, uuid.toString());
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return new User(
                            uuid,
                            result.getString("name"),
                            result.getLong("coins"),
                            result.getLong("first_join"),
                            result.getLong("last_join")
                    );
                }
            }
        }
        long now = System.currentTimeMillis();
        User created = new User(uuid, name, startingCoins, now, now);
        save(created);
        return created;
    }

    @Override
    public void save(User user) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement upsert = connection.prepareStatement(UPSERT)) {
            upsert.setString(1, user.getUniqueId().toString());
            upsert.setString(2, trimName(user.getName()));
            upsert.setLong(3, user.getCoins());
            upsert.setLong(4, user.getFirstJoin());
            upsert.setLong(5, user.getLastJoin());
            upsert.executeUpdate();
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static String trimName(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.length() > 16 ? name.substring(0, 16) : name;
    }
}
