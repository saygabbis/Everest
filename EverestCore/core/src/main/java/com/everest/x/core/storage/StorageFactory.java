package com.everest.x.core.storage;

import com.everest.x.core.config.PluginSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public final class StorageFactory {

    private StorageFactory() {
    }

    public static UserRepository create(JavaPlugin plugin, PluginSettings settings) throws SQLException {
        if (settings.storageType() == PluginSettings.StorageType.MYSQL) {
            plugin.getLogger().info("Storage: MySQL (" + settings.mysqlHost() + "/" + settings.mysqlDatabase() + ")");
            MySqlUserRepository repository = new MySqlUserRepository(settings);
            repository.init();
            return repository;
        }
        plugin.getLogger().info("Storage: arquivos YAML em plugins/EverestCore/data/users");
        return new FileUserRepository(plugin);
    }

    public static UserRepository createOrDisable(JavaPlugin plugin, PluginSettings settings) {
        try {
            return create(plugin, settings);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao abrir o storage. Verifique config.yml.", exception);
            return null;
        }
    }
}
