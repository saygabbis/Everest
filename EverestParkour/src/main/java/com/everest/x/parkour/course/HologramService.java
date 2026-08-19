package com.everest.x.parkour.course;

import com.everest.x.parkour.EverestParkourPlugin;
import com.everest.x.parkour.util.Colors;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public final class HologramService {

    public static final String META = "everest-parkour-holo";

    private final EverestParkourPlugin plugin;
    private final List<ArmorStand> spawned = new ArrayList<>();

    public HologramService(EverestParkourPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnAll() {
        despawnAll();
        for (Course course : plugin.courses().all()) {
            spawn(course);
        }
    }

    public void refresh(Course course) {
        remove(course);
        spawn(course);
    }

    public void remove(Course course) {
        spawned.removeIf(stand -> {
            if (!stand.isValid()) {
                return true;
            }
            if (!course.name().equals(courseName(stand))) {
                return false;
            }
            stand.remove();
            return true;
        });
        clearNearby(course);
    }

    public void despawnAll() {
        for (ArmorStand stand : spawned) {
            if (stand.isValid()) {
                stand.remove();
            }
        }
        spawned.clear();
        for (Course course : plugin.courses().all()) {
            clearNearby(course);
        }
    }

    private void spawn(Course course) {
        if (course.start() != null) {
            spawnLines(course, course.start().hologramBase(), List.of(
                    plugin.messages().get("hologram.start")));
        }
        int index = 1;
        for (Course.Point point : course.checkpoints()) {
            spawnLines(course, point.hologramBase(), List.of(
                    plugin.messages().get("hologram.checkpoint", "index", String.valueOf(index))));
            index++;
        }
        if (course.end() != null) {
            spawnLines(course, course.end().hologramBase(), List.of(
                    plugin.messages().get("hologram.end")));
        }
    }

    private void spawnLines(Course course, Location base, List<String> lines) {
        if (base == null || base.getWorld() == null) {
            return;
        }
        double y = base.getY();
        for (String line : lines) {
            Location at = base.clone();
            at.setY(y);
            spawned.add(stand(course, at, line));
            y -= 0.28;
        }
    }

    private ArmorStand stand(Course course, Location location, String text) {
        World world = location.getWorld();
        for (ArmorStand existing : world.getEntitiesByClass(ArmorStand.class)) {
            if (existing.getLocation().distanceSquared(location) <= 0.16 && existing.isCustomNameVisible()) {
                existing.remove();
            }
        }
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setSmall(true);
        stand.setCustomName(Colors.color(text));
        stand.setCustomNameVisible(true);
        stand.setCanPickupItems(false);
        disableHitbox(stand);
        stand.setMetadata(META, new FixedMetadataValue(plugin, course.name()));
        return stand;
    }

    private static void disableHitbox(ArmorStand stand) {
        try {
            ArmorStand.class.getMethod("setMarker", boolean.class).invoke(stand, true);
            return;
        } catch (ReflectiveOperationException ignored) {
            // 1.8 vanilla
        }
        try {
            Object handle = stand.getClass().getMethod("getHandle").invoke(stand);
            try {
                handle.getClass().getMethod("n", boolean.class).invoke(handle, true);
            } catch (ReflectiveOperationException ignored) {
                handle.getClass().getField("noclip").setBoolean(handle, true);
            }
        } catch (ReflectiveOperationException ignored) {
            // holograma pode empurrar se o servidor não tiver marker
        }
    }

    private void clearNearby(Course course) {
        clearPoint(course.start());
        clearPoint(course.end());
        for (Course.Point point : course.checkpoints()) {
            clearPoint(point);
        }
    }

    private void clearPoint(Course.Point point) {
        if (point == null) {
            return;
        }
        Location base = point.hologramBase();
        if (base == null || base.getWorld() == null) {
            return;
        }
        World world = base.getWorld();
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            if (stand.getLocation().distanceSquared(base) <= 1.5
                    && (stand.hasMetadata(META) || (stand.isCustomNameVisible() && !stand.isVisible()))) {
                stand.remove();
            }
        }
    }

    private static String courseName(ArmorStand stand) {
        if (!stand.hasMetadata(META) || stand.getMetadata(META).isEmpty()) {
            return "";
        }
        return stand.getMetadata(META).get(0).asString();
    }

    public static boolean isHologram(Entity entity) {
        return entity instanceof ArmorStand && entity.hasMetadata(META);
    }
}
