package com.everest.x.core.spawn;

import com.everest.x.core.EverestCorePlugin;
import com.everest.x.core.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registro dos spawns nomeados deste servidor. Um deles é o spawn padrão
 * (join spawn): usado na entrada, no respawn e em {@code /spawn} sem argumentos.
 */
public final class SpawnService {

    private final EverestCorePlugin plugin;
    private final Map<String, SpawnPoint> points = new LinkedHashMap<>();

    private boolean enabled;
    private boolean teleportOnJoin;
    private boolean teleportOnRespawn;
    private String defaultName;

    public SpawnService(EverestCorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("spawn.enabled", true);
        teleportOnJoin = config.getBoolean("spawn.teleport-on-join", true);
        teleportOnRespawn = config.getBoolean("spawn.teleport-on-respawn", true);
        defaultName = normalize(firstNonBlank(
                config.getString("spawn.default"),
                config.getString("spawn.mother")));
        points.clear();

        ConfigurationSection section = config.getConfigurationSection("spawn.points");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String name = normalize(key);
                String path = "spawn.points." + key;
                String world = config.getString(path + ".world");
                if (!name.isEmpty() && world != null && !world.isBlank()) {
                    points.put(name, readPoint(config, path, world));
                }
            }
        }

        loadLegacySpawn(config);
        if (!points.containsKey(defaultName)) {
            defaultName = points.keySet().stream().findFirst().orElse("");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return !defaultName.isEmpty() && points.containsKey(defaultName);
    }

    public boolean teleportOnJoin() {
        return teleportOnJoin;
    }

    public boolean teleportOnRespawn() {
        return teleportOnRespawn;
    }

    public void setTeleportOnJoin(boolean value) {
        teleportOnJoin = value;
        plugin.getConfig().set("spawn.teleport-on-join", value);
        plugin.saveConfig();
    }

    public void setTeleportOnRespawn(boolean value) {
        teleportOnRespawn = value;
        plugin.getConfig().set("spawn.teleport-on-respawn", value);
        plugin.saveConfig();
    }

    public Location location() {
        return location(defaultName);
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

    public List<String> nameList() {
        return new ArrayList<>(points.keySet());
    }

    public SpawnPoint point(String name) {
        return points.get(normalize(name));
    }

    public String defaultName() {
        return defaultName;
    }

    /** @deprecated use {@link #defaultName()} */
    @Deprecated
    public String motherName() {
        return defaultName;
    }

    public boolean isDefault(String name) {
        return !defaultName.isEmpty() && defaultName.equals(normalize(name));
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

        SpawnPoint previous = points.get(name);
        SpawnPoint created = new SpawnPoint(
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                previous == null ? Material.DIRT : previous.icon(),
                previous == null ? 0 : previous.iconData(),
                previous != null && previous.logoutReturn(),
                previous != null && previous.worldRespawn());
        points.put(name, created);
        if (defaultName.isEmpty()) {
            defaultName = name;
        }
        persistPoint(name, created);
        return true;
    }

    public boolean updateLocation(String rawName, Player player) {
        String name = normalize(rawName);
        SpawnPoint previous = points.get(name);
        Location location = player.getLocation();
        if (previous == null || location.getWorld() == null) {
            return false;
        }
        SpawnPoint updated = previous.withLocation(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        points.put(name, updated);
        persistPoint(name, updated);
        return true;
    }

    public boolean rename(String fromRaw, String toRaw) {
        String from = normalize(fromRaw);
        String to = normalize(toRaw);
        if (from.isEmpty() || to.isEmpty() || !points.containsKey(from) || points.containsKey(to)) {
            return false;
        }
        SpawnPoint point = points.remove(from);
        points.put(to, point);
        if (from.equals(defaultName)) {
            defaultName = to;
        }
        plugin.getConfig().set("spawn.points." + from, null);
        persistPoint(to, point);
        return true;
    }

    public boolean remove(String rawName) {
        String name = normalize(rawName);
        if (!points.containsKey(name)) {
            return false;
        }
        points.remove(name);
        plugin.getConfig().set("spawn.points." + name, null);
        if (name.equals(defaultName)) {
            defaultName = points.keySet().stream().findFirst().orElse("");
        }
        plugin.getConfig().set("spawn.default", defaultName);
        plugin.getConfig().set("spawn.mother", defaultName);
        plugin.saveConfig();
        return true;
    }

    public boolean setDefault(String rawName) {
        String name = normalize(rawName);
        if (!points.containsKey(name)) {
            return false;
        }
        defaultName = name;
        plugin.getConfig().set("spawn.default", defaultName);
        plugin.getConfig().set("spawn.mother", defaultName);
        plugin.saveConfig();
        return true;
    }

    public boolean setLogoutReturn(String rawName, boolean value) {
        return updateFlags(rawName, value, null);
    }

    public boolean setWorldRespawn(String rawName, boolean value) {
        String name = normalize(rawName);
        SpawnPoint current = points.get(name);
        if (current == null) {
            return false;
        }
        if (value) {
            for (Map.Entry<String, SpawnPoint> entry : points.entrySet()) {
                if (!entry.getKey().equals(name)
                        && entry.getValue().world().equals(current.world())
                        && entry.getValue().worldRespawn()) {
                    SpawnPoint cleared = entry.getValue().withFlags(
                            entry.getValue().logoutReturn(), false);
                    points.put(entry.getKey(), cleared);
                    persistPoint(entry.getKey(), cleared);
                }
            }
        }
        return updateFlags(name, null, value);
    }

    public boolean setIcon(String rawName, ItemStack item) {
        String name = normalize(rawName);
        SpawnPoint current = points.get(name);
        if (current == null || item == null || item.getType() == Material.AIR) {
            return false;
        }
        SpawnPoint updated = current.withIcon(item.getType(), item.getDurability());
        points.put(name, updated);
        persistPoint(name, updated);
        return true;
    }

    public boolean teleport(Player player) {
        return teleport(player, defaultName);
    }

    public boolean teleport(Player player, String name) {
        Location location = location(name);
        if (location == null) {
            return false;
        }
        player.teleport(location);
        return true;
    }

    public Location resolveJoin(User user) {
        if (!enabled || !teleportOnJoin) {
            return null;
        }
        if (user != null) {
            String last = normalize(user.lastSpawn());
            SpawnPoint remembered = points.get(last);
            if (remembered != null && remembered.logoutReturn()) {
                Location location = location(last);
                if (location != null) {
                    return location;
                }
            }
        }
        return location();
    }

    public Location resolveRespawn(World deathWorld) {
        if (!enabled || !teleportOnRespawn) {
            return null;
        }
        if (deathWorld != null) {
            for (Map.Entry<String, SpawnPoint> entry : points.entrySet()) {
                if (entry.getValue().worldRespawn() && deathWorld.getName().equals(entry.getValue().world())) {
                    Location location = location(entry.getKey());
                    if (location != null) {
                        return location;
                    }
                }
            }
        }
        return location();
    }

    public void rememberLogout(Player player, User user) {
        if (user == null || player.getWorld() == null) {
            return;
        }
        String world = player.getWorld().getName();
        String match = null;
        for (Map.Entry<String, SpawnPoint> entry : points.entrySet()) {
            if (!entry.getValue().logoutReturn() || !world.equals(entry.getValue().world())) {
                continue;
            }
            if (isDefault(entry.getKey()) || match == null) {
                match = entry.getKey();
            }
        }
        user.setLastSpawn(match == null ? "" : match);
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.trim().toLowerCase(new Locale("pt", "BR"));
        if (normalized.isEmpty() || normalized.length() > 32) {
            return "";
        }
        return normalized.matches("[\\p{L}0-9_-]{1,32}") ? normalized : "";
    }

    private boolean updateFlags(String rawName, Boolean logoutReturn, Boolean worldRespawn) {
        String name = normalize(rawName);
        SpawnPoint current = points.get(name);
        if (current == null) {
            return false;
        }
        SpawnPoint updated = current.withFlags(
                logoutReturn == null ? current.logoutReturn() : logoutReturn,
                worldRespawn == null ? current.worldRespawn() : worldRespawn);
        points.put(name, updated);
        persistPoint(name, updated);
        return true;
    }

    private void persistPoint(String name, SpawnPoint point) {
        FileConfiguration config = plugin.getConfig();
        String path = "spawn.points." + name;
        config.set(path + ".world", point.world());
        config.set(path + ".x", point.x());
        config.set(path + ".y", point.y());
        config.set(path + ".z", point.z());
        config.set(path + ".yaw", (double) point.yaw());
        config.set(path + ".pitch", (double) point.pitch());
        config.set(path + ".icon", point.icon().name());
        config.set(path + ".icon-data", (int) point.iconData());
        config.set(path + ".logout-return", point.logoutReturn());
        config.set(path + ".world-respawn", point.worldRespawn());
        config.set("spawn.default", defaultName);
        config.set("spawn.mother", defaultName);
        plugin.saveConfig();
    }

    private SpawnPoint readPoint(FileConfiguration config, String path, String world) {
        Material icon = Material.matchMaterial(config.getString(path + ".icon", "DIRT"));
        if (icon == null || icon == Material.AIR) {
            icon = Material.DIRT;
        }
        return new SpawnPoint(
                world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch"),
                icon,
                (short) config.getInt(path + ".icon-data", 0),
                config.getBoolean(path + ".logout-return", false),
                config.getBoolean(path + ".world-respawn", false));
    }

    private void loadLegacySpawn(FileConfiguration config) {
        if (!points.isEmpty() || !config.getBoolean("spawn.configured", false)) {
            return;
        }
        String world = config.getString("spawn.world");
        if (world == null || world.isBlank()) {
            return;
        }
        defaultName = "principal";
        points.put(defaultName, new SpawnPoint(
                world,
                config.getDouble("spawn.x"),
                config.getDouble("spawn.y"),
                config.getDouble("spawn.z"),
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch"),
                Material.DIRT,
                (short) 0,
                false,
                false));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
