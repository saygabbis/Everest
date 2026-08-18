package com.everest.x.api;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de entrada do EverestCore.
 * Outros plugins devem usar {@code softdepend: [EverestCore]} e checar
 * {@link #isAvailable()} — assim Bedwars/Login/Survival rodam isolados.
 */
public final class EverestAPI {

    private static EverestProvider provider;

    private EverestAPI() {
    }

    public static void register(EverestProvider instance) {
        provider = instance;
    }

    public static void unregister(EverestProvider instance) {
        if (provider == instance) {
            provider = null;
        }
    }

    public static boolean isAvailable() {
        return provider != null && provider.isReady();
    }

    public static EverestProvider getProvider() {
        return require();
    }

    public static EverestUser getUser(Player player) {
        return require().getUser(player);
    }

    public static Optional<EverestUser> findUser(UUID uuid) {
        return require().findUser(uuid);
    }

    public static ServerType getServerType() {
        return require().getServerType();
    }

    public static String getServerId() {
        return require().getServerId();
    }

    public static void sendToServer(Player player, String serverId) {
        require().sendToServer(player, serverId);
    }

    public static void registerHook(EverestHook hook) {
        require().registerHook(hook);
    }

    public static void unregisterHook(EverestHook hook) {
        require().unregisterHook(hook);
    }

    public static Collection<EverestHook> getHooks() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        return provider.getHooks();
    }

    private static EverestProvider require() {
        if (!isAvailable()) {
            throw new IllegalStateException("EverestCore não está carregado. Use EverestAPI.isAvailable() antes.");
        }
        return provider;
    }
}
