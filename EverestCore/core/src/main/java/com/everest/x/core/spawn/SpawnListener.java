package com.everest.x.core.spawn;

import com.everest.x.core.EverestCorePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class SpawnListener implements Listener {

    private final EverestCorePlugin plugin;

    public SpawnListener(EverestCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        SpawnService spawn = plugin.spawn();
        if (!spawn.isEnabled() || !spawn.teleportOnJoin() || !spawn.isConfigured()) {
            return;
        }
        Player player = event.getPlayer();
        spawn.teleport(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        SpawnService spawn = plugin.spawn();
        if (!spawn.isEnabled() || !spawn.teleportOnRespawn() || !spawn.isConfigured()) {
            return;
        }
        Location location = spawn.location();
        if (location != null) {
            event.setRespawnLocation(location);
        }
    }
}
