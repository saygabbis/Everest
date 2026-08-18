package com.everest.x.api;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação publicada pelo EverestCore.
 * Plugins externos devem usar {@link EverestAPI}, não esta interface.
 */
public interface EverestProvider {

    boolean isReady();

    EverestUser getUser(Player player);

    Optional<EverestUser> findUser(UUID uuid);

    ServerType getServerType();

    String getServerId();

    void sendToServer(Player player, String serverId);

    void registerHook(EverestHook hook);

    void unregisterHook(EverestHook hook);

    Collection<EverestHook> getHooks();
}
