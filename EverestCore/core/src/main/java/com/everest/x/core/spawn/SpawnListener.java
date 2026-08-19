package com.everest.x.core.spawn;

import com.everest.x.core.EverestCorePlugin;
import com.everest.x.core.user.User;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpawnListener implements Listener {

    private final EverestCorePlugin plugin;
    private final Map<UUID, String> deathWorlds = new ConcurrentHashMap<>();

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
        User user = plugin.users().get(player.getUniqueId());
        Location location = spawn.resolveJoin(user);
        if (location != null) {
            player.teleport(location);
        }
    }

    public void applyJoin(Player player, User user) {
        SpawnService spawn = plugin.spawn();
        if (!spawn.isEnabled() || !spawn.teleportOnJoin()) {
            return;
        }
        Location location = spawn.resolveJoin(user);
        if (location != null) {
            player.teleport(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        World world = event.getEntity().getWorld();
        if (world != null) {
            deathWorlds.put(event.getEntity().getUniqueId(), world.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        SpawnService spawn = plugin.spawn();
        if (!spawn.isEnabled() || !spawn.teleportOnRespawn() || !spawn.isConfigured()) {
            return;
        }
        String worldName = deathWorlds.remove(event.getPlayer().getUniqueId());
        World deathWorld = worldName == null ? event.getPlayer().getWorld() : plugin.getServer().getWorld(worldName);
        Location location = spawn.resolveRespawn(deathWorld);
        if (location != null) {
            event.setRespawnLocation(location);
        }
    }
}
