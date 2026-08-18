package com.everest.x.core.storage;

import com.everest.x.core.user.User;

import java.util.UUID;

/**
 * Acesso a disco/SQL. Chamadas são bloqueantes — sempre fora da thread principal.
 */
public interface UserRepository {

    User loadOrCreate(UUID uuid, String name, long startingCoins) throws Exception;

    void save(User user) throws Exception;

    void close();
}
