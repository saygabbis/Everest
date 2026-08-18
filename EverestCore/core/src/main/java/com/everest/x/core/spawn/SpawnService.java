package com.everest.x.core.spawn;

import com.everest.x.core.EverestCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registro dos spawns nomeados deste servidor. Um deles é o spawn mãe,
 * usado no join, respawn e em {@code /spawn} sem argumentos.
 */
public final class SpawnService {

    private final EverestCorePlugin plugin;
    private final Map<String, SpawnPoint> points = new LinkedHashMap<>();

    private boolean enabled;
    private boolean teleportOnJoin;
    private boolean teleportOnRespawn;
    private String motherName;

    public SpawnService(EverestCorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("spawn.enabled", true);
        teleportOnJoin = config.getBoolean("spawn.teleport-on-join", true);
        teleportOnRespawn = config.getBoolean("spawn.teleport-on-respawn", true);
        motherName = normalize(config.getString("spawn.mother", ""));
        points.clear();

        ConfigurationSection section = config.getConfigurationSection("spawn.points");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String name = normalize(key);
                String path = "spawn.points." + key;
                String world = config.getString(path + ".world");
                if (!name.isEmpty() && world != null && !world.isBlank()) {
                    points.put(name, new SpawnPoint(
                            world,
                            config.getDouble(path + ".x"),
                            config.getDouble(path + ".y"),
                            config.getDouble(path + ".z"),
                            (float) config.getDouble(path + ".yaw"),
                            (float) config.getDouble(path + ".pitch")));
                }
            }
        }

        loadLegacySpawn(config);
        if (!points.containsKey(motherName)) {
            motherName = points.keySet().stream().findFirst().orElse("");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return !motherName.isEmpty() && points.containsKey(motherName);
    }

    public boolean teleportOnJoin() {
        return teleportOnJoin;
    }

    public boolean teleportOnRespawn() {
        return teleportOnRespawn;
    }

    public Location location() {
        return location(motherName);
    }

    public Location location(String name) {
        SpawnPoint point = points.get(normalize(name));
        if (point == null) {
            return null;
        }
        World world = Bukkit.getWorld(point.world());
        if (world == null) {
            return null;
        }
        return new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
    }

    public boolean exists(String name) {
        return points.containsKey(normalize(name));
    }

    public Collection<String> names() {
        return Collections.unmodifiableSet(points.keySet());
    }

    public String motherName() {
        return motherName;
    }

    public String worldName(String name) {
        SpawnPoint point = points.get(normalize(name));
        return point == null ? "?" : point.world();
    }

    public boolean setFrom(String rawName, Player player) {
        String name = normalize(rawName);
        if (name.isEmpty()) {
            return false;
        }
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        points.put(name, new SpawnPoint(
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()));
        // O spawn mãe só muda pelo config; o primeiro criado assume o papel.
        if (motherName.isEmpty()) {
            motherName = name;
        }

        FileConfiguration config = plugin.getConfig();
        String path = "spawn.points." + name;
        config.set(path + ".world", world.getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
        config.set("spawn.mother", motherName);
        plugin.saveConfig();
        return true;
    }

    public boolean teleport(Player player) {
        return teleport(player, motherName);
    }

    public boolean teleport(Player player, String name) {
        Location location = location(name);
        if (location == null) {
            return false;
        }
        player.teleport(location);
        return true;
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z0-9_-]{1,32}") ? normalized : "";
    }

    private void loadLegacySpawn(FileConfiguration config) {
        if (!points.isEmpty() || !config.getBoolean("spawn.configured", false)) {
            return;
        }
        String world = config.getString("spawn.world");
        if (world == null || world.isBlank()) {
            return;
        }
        motherName = "principal";
        points.put(motherName, new SpawnPoint(
                world,
                config.getDouble("spawn.x"),
                config.getDouble("spawn.y"),
                config.getDouble("spawn.z"),
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch")));
    }

    private record SpawnPoint(
            String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch) {
    }
}
