package com.everest.x.parkour.hook;

import com.everest.x.api.EverestAPI;
import com.everest.x.api.EverestHook;
import com.everest.x.api.EverestUser;
import com.everest.x.parkour.EverestParkourPlugin;
import org.bukkit.entity.Player;

/**
 * Só é carregado se o EverestCore estiver no servidor.
 */
public final class CoreAccess {

    private CoreAccess() {
    }

    public static void register(EverestParkourPlugin plugin) {
        if (!EverestAPI.isAvailable()) {
            return;
        }
        EverestAPI.registerHook(new EverestHook() {
            @Override
            public String getId() {
                return "parkour";
            }

            @Override
            public String getDisplayName() {
                return "EverestParkour";
            }

            @Override
            public String getVersion() {
                return plugin.getDescription().getVersion();
            }
        });
        plugin.getLogger().info("Ligado ao EverestCore.");
    }

    public static void unregister() {
        // o Core limpa os hooks no disable
    }

    public static void reward(Player player, EverestParkourPlugin plugin) {
        if (!EverestAPI.isAvailable()) {
            return;
        }
        long coins = plugin.getConfig().getLong("reward-coins", 0L);
        if (coins <= 0L) {
            return;
        }
        EverestUser user = EverestAPI.getUser(player);
        if (user == null) {
            return;
        }
        user.addCoins(coins);
        player.sendMessage(plugin.messages().get("reward", "coins", String.valueOf(coins)));
    }
}
