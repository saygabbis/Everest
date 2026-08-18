package com.everest.x.core.storage;

import com.everest.x.core.user.User;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class FileUserRepository implements UserRepository {

    private final File folder;

    public FileUserRepository(JavaPlugin plugin) {
        this.folder = new File(plugin.getDataFolder(), "data/users");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Não foi possível criar " + folder.getAbsolutePath());
        }
    }

    @Override
    public User loadOrCreate(UUID uuid, String name, long startingCoins) {
        File file = fileOf(uuid);
        if (!file.exists()) {
            long now = System.currentTimeMillis();
            return new User(uuid, name, startingCoins, now, now);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return new User(
                uuid,
                yaml.getString("name", name),
                yaml.getLong("coins", startingCoins),
                yaml.getLong("first-join", System.currentTimeMillis()),
                yaml.getLong("last-join", System.currentTimeMillis())
        );
    }

    @Override
    public void save(User user) throws IOException {
        File file = fileOf(user.getUniqueId());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("uuid", user.getUniqueId().toString());
        yaml.set("name", user.getName());
        yaml.set("coins", user.getCoins());
        yaml.set("first-join", user.getFirstJoin());
        yaml.set("last-join", user.getLastJoin());
        yaml.save(file);
    }

    @Override
    public void close() {
        // nada a fechar
    }

    private File fileOf(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }
}
