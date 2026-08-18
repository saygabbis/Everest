package com.everest.x.core;

import com.everest.x.api.EverestHook;
import com.everest.x.api.EverestProvider;
import com.everest.x.api.EverestUser;
import com.everest.x.api.ServerType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class EverestProviderImpl implements EverestProvider {

    private final EverestCorePlugin plugin;

    public EverestProviderImpl(EverestCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isReady() {
        return plugin.isEnabled() && plugin.repository() != null;
    }

    @Override
    public EverestUser getUser(Player player) {
        return plugin.users().get(player.getUniqueId());
    }

    @Override
    public Optional<EverestUser> findUser(UUID uuid) {
        return plugin.users().find(uuid).map(user -> user);
    }

    @Override
    public ServerType getServerType() {
        return plugin.settings().serverType();
    }

    @Override
    public String getServerId() {
        return plugin.settings().serverId();
    }

    @Override
    public void sendToServer(Player player, String serverId) {
        plugin.proxy().sendToServer(player, serverId);
    }

    @Override
    public void registerHook(EverestHook hook) {
        plugin.hooks().register(hook);
        plugin.getLogger().info("Hook registrado: " + hook.getDisplayName() + " (" + hook.getId() + ")");
    }

    @Override
    public void unregisterHook(EverestHook hook) {
        plugin.hooks().unregister(hook);
    }

    @Override
    public Collection<EverestHook> getHooks() {
        return plugin.hooks().all();
    }
}
