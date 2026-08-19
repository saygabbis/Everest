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

    /**
     * @return {@code true} se o spawn padrão existe e o jogador foi teleportado
     */
    boolean teleportToSpawn(Player player);

    boolean teleportToSpawn(Player player, String name);

    boolean hasSpawn();

    boolean hasSpawn(String name);

    /** Nome do spawn padrão (join spawn). */
    String getDefaultSpawnName();

    /** @deprecated use {@link #getDefaultSpawnName()} */
    @Deprecated
    default String getMotherSpawnName() {
        return getDefaultSpawnName();
    }

    Collection<String> getSpawnNames();

    void registerHook(EverestHook hook);

    void unregisterHook(EverestHook hook);

    Collection<EverestHook> getHooks();
}
