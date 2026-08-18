package com.everest.x.core.user;

import com.everest.x.core.EverestCorePlugin;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UserCache {

    private final EverestCorePlugin plugin;
    private final Map<UUID, User> online = new ConcurrentHashMap<>();

    public UserCache(EverestCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void put(User user) {
        user.attach(plugin, plugin.scheduler(), () -> plugin.persistAsync(user));
        online.put(user.getUniqueId(), user);
    }

    public User get(UUID uuid) {
        return online.get(uuid);
    }

    public Optional<User> find(UUID uuid) {
        return Optional.ofNullable(online.get(uuid));
    }

    public void remove(UUID uuid) {
        online.remove(uuid);
    }

    public Collection<User> all() {
        return online.values();
    }

    public void clear() {
        online.clear();
    }
}
