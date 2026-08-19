package com.everest.x.parkour.course;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Course {

    public record Point(String world, int x, int y, int z, float yaw, float pitch) {

        public Location toLocation() {
            World loaded = Bukkit.getWorld(world);
            if (loaded == null) {
                return null;
            }
            return new Location(loaded, x + 0.5, y, z + 0.5, yaw, pitch);
        }

        public Location toPosition(Location look) {
            World loaded = Bukkit.getWorld(world);
            if (loaded == null) {
                return null;
            }
            float yawLook = look == null ? yaw : look.getYaw();
            float pitchLook = look == null ? pitch : look.getPitch();
            return new Location(loaded, x + 0.5, y, z + 0.5, yawLook, pitchLook);
        }

        public Location hologramBase() {
            World loaded = Bukkit.getWorld(world);
            if (loaded == null) {
                return null;
            }
            return new Location(loaded, x + 0.5, y + 1.55, z + 0.5);
        }

        public boolean matches(Location location) {
            return location != null
                    && location.getWorld() != null
                    && world.equals(location.getWorld().getName())
                    && x == location.getBlockX()
                    && y == location.getBlockY()
                    && z == location.getBlockZ();
        }

        public static Point from(Location location) {
            return new Point(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    location.getYaw(),
                    location.getPitch());
        }
    }

    public record Record(String name, long millis) {
    }

    private final String name;
    private Point start;
    private Point end;
    private final List<Point> checkpoints = new ArrayList<>();
    private Double failY;
    private final Map<UUID, Record> records = new LinkedHashMap<>();

    public Course(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Point start() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point end() {
        return end;
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    public List<Point> checkpoints() {
        return checkpoints;
    }

    public Double failY() {
        return failY;
    }

    public void setFailY(Double failY) {
        this.failY = failY;
    }

    public double resolvedFailY() {
        if (failY != null) {
            return failY;
        }
        if (start != null) {
            return start.y() - 8;
        }
        return Double.NEGATIVE_INFINITY;
    }

    public Map<UUID, Record> records() {
        return records;
    }

    public boolean isReady() {
        return start != null && end != null && start.toLocation() != null && end.toLocation() != null;
    }

    public Record personalBest(UUID uuid) {
        return records.get(uuid);
    }

    public boolean beat(UUID uuid, String playerName, long millis) {
        Record current = records.get(uuid);
        if (current != null && current.millis() <= millis) {
            return false;
        }
        records.put(uuid, new Record(playerName, millis));
        return true;
    }

    public void write(ConfigurationSection section) {
        writePoint(section, "start", start);
        writePoint(section, "end", end);
        if (failY != null) {
            section.set("fail-y", failY);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Point point : checkpoints) {
            list.add(pointMap(point));
        }
        section.set("checkpoints", list);
        ConfigurationSection recordSection = section.createSection("records");
        for (Map.Entry<UUID, Record> entry : records.entrySet()) {
            recordSection.set(entry.getKey().toString() + ".name", entry.getValue().name());
            recordSection.set(entry.getKey().toString() + ".millis", entry.getValue().millis());
        }
    }

    public static Course read(String name, ConfigurationSection section) {
        Course course = new Course(name);
        course.start = readPoint(section.getConfigurationSection("start"));
        course.end = readPoint(section.getConfigurationSection("end"));
        if (section.contains("fail-y")) {
            course.failY = section.getDouble("fail-y");
        }
        List<Map<?, ?>> raw = section.getMapList("checkpoints");
        for (Map<?, ?> map : raw) {
            Point point = readPoint(map);
            if (point != null) {
                course.checkpoints.add(point);
            }
        }
        ConfigurationSection recordSection = section.getConfigurationSection("records");
        if (recordSection != null) {
            for (String key : recordSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String player = recordSection.getString(key + ".name", "?");
                    long millis = recordSection.getLong(key + ".millis");
                    course.records.put(uuid, new Record(player, millis));
                } catch (IllegalArgumentException ignored) {
                    // uuid inválido
                }
            }
        }
        return course;
    }

    private static void writePoint(ConfigurationSection parent, String path, Point point) {
        if (point == null) {
            return;
        }
        ConfigurationSection section = parent.createSection(path);
        section.set("world", point.world());
        section.set("x", point.x());
        section.set("y", point.y());
        section.set("z", point.z());
        section.set("yaw", (double) point.yaw());
        section.set("pitch", (double) point.pitch());
    }

    private static Map<String, Object> pointMap(Point point) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", point.world());
        map.put("x", point.x());
        map.put("y", point.y());
        map.put("z", point.z());
        map.put("yaw", (double) point.yaw());
        map.put("pitch", (double) point.pitch());
        return map;
    }

    private static Point readPoint(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String world = section.getString("world");
        if (world == null || world.isBlank()) {
            return null;
        }
        return new Point(
                world,
                section.getInt("x"),
                section.getInt("y"),
                section.getInt("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }

    private static Point readPoint(Map<?, ?> map) {
        Object world = map.get("world");
        if (!(world instanceof String worldName) || worldName.isBlank()) {
            return null;
        }
        return new Point(
                worldName,
                asInt(map.get("x")),
                asInt(map.get("y")),
                asInt(map.get("z")),
                asFloat(map.get("yaw")),
                asFloat(map.get("pitch")));
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static float asFloat(Object value) {
        return value instanceof Number number ? number.floatValue() : 0f;
    }
}
