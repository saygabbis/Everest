package com.everest.x.api;

import java.util.UUID;

/**
 * Jogador da rede, sempre identificado por UUID.
 * Outros plugins (Bedwars, Duels, Survival) leem e alteram coins por aqui
 * quando o Core está instalado.
 */
public interface EverestUser {

    UUID getUniqueId();

    String getName();

    long getCoins();

    void setCoins(long amount);

    void addCoins(long amount);

    /**
     * @return {@code false} se não houver saldo suficiente
     */
    boolean takeCoins(long amount);

    long getFirstJoin();

    long getLastJoin();
}
