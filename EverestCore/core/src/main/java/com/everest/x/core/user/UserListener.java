package com.everest.x.core.user;

import com.everest.x.api.event.EverestUserLoadEvent;
import com.everest.x.core.EverestCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.logging.Level;

public final class UserListener implements Listener {

    private final EverestCorePlugin plugin;

    public UserListener(EverestCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        long starting = plugin.settings().startingCoins();

        plugin.scheduler().async(() -> {
            try {
                User user = plugin.repository().loadOrCreate(uuid, name, starting);
                user.setName(name);
                user.setLastJoin(System.currentTimeMillis());
                plugin.repository().save(user);
                plugin.scheduler().sync(() -> {
                    if (!player.isOnline() || !plugin.isEnabled()) {
                        return;
                    }
                    plugin.users().put(user);
                    plugin.getServer().getPluginManager().callEvent(new EverestUserLoadEvent(user));
                    plugin.spawnListener().applyJoin(player, user);
                });
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Falha ao carregar " + name, exception);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        User user = plugin.users().get(uuid);
        if (user == null) {
            return;
        }
        plugin.spawn().rememberLogout(event.getPlayer(), user);
        plugin.persistAsync(user);
        plugin.users().remove(uuid);
    }
}
