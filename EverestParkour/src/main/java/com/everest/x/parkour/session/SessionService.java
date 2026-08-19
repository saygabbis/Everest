package com.everest.x.parkour.session;

import com.everest.x.parkour.EverestParkourPlugin;
import com.everest.x.parkour.course.Course;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionService {

    private final EverestParkourPlugin plugin;
    private final Map<UUID, ParkourSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> editing = new ConcurrentHashMap<>();
    private final Map<UUID, String> standingPlate = new ConcurrentHashMap<>();

    public SessionService(EverestParkourPlugin plugin) {
        this.plugin = plugin;
    }

    public ParkourSession get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean inParkour(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void join(Player player, Course course, boolean restartMessage) {
        ParkourSession previous = sessions.get(player.getUniqueId());
        boolean same = previous != null && previous.course().name().equals(course.name());
        SavedInventory saved = same ? previous.saved() : SavedInventory.capture(player);
        ParkourSession session = new ParkourSession(player.getUniqueId(), course, saved);
        sessions.put(player.getUniqueId(), session);
        if (!same) {
            ParkourKit.apply(player, plugin);
        }
        if (same && restartMessage) {
            player.sendMessage(plugin.messages().get("restart"));
            return;
        }
        if (!same) {
            player.sendMessage(plugin.messages().get("join", "name", course.name()));
        }
    }

    public void restartAtStart(Player player) {
        ParkourSession session = get(player);
        if (session == null || session.course().start() == null) {
            return;
        }
        Course course = session.course();
        join(player, course, true);
        warpKeepLook(player, course.start());
        occupy(player, course.start());
    }

    public void warpKeepLook(Player player, Course.Point point) {
        teleportKeepLook(player, point);
    }

    public void returnToCheckpoint(Player player) {
        ParkourSession session = get(player);
        if (session == null) {
            return;
        }
        teleportKeepLook(player, session.respawnPoint());
        occupy(player, session.respawnPoint());
        player.sendMessage(plugin.messages().get("back-checkpoint"));
    }

    public void leave(Player player, boolean message) {
        ParkourSession removed = sessions.remove(player.getUniqueId());
        if (removed == null) {
            return;
        }
        removed.saved().restore(player);
        if (message) {
            player.sendMessage(plugin.messages().get("leave"));
        }
    }

    public void checkpoint(Player player, int index) {
        ParkourSession session = get(player);
        if (session == null) {
            return;
        }
        session.reachCheckpoint(index);
        player.sendMessage(plugin.messages().get("checkpoint",
                "index", String.valueOf(index + 1),
                "time", format(session.elapsed())));
    }

    public void fail(Player player) {
        ParkourSession session = get(player);
        if (session == null || !session.tryFail()) {
            return;
        }
        teleportKeepLook(player, session.respawnPoint());
        occupy(player, session.respawnPoint());
        player.sendMessage(plugin.messages().get("fail", "time", format(session.elapsed())));
    }

    public void finish(Player player) {
        ParkourSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.saved().restore(player);
        Course course = session.course();
        long elapsed = session.elapsed();
        String time = format(elapsed);
        player.sendMessage(plugin.messages().get("finish", "name", course.name(), "time", time));
        boolean record = course.beat(player.getUniqueId(), player.getName(), elapsed);
        plugin.courses().save();
        if (record) {
            player.sendMessage(plugin.messages().get("record", "time", time));
        } else {
            Course.Record best = course.personalBest(player.getUniqueId());
            if (best != null) {
                player.sendMessage(plugin.messages().get("best", "best", format(best.millis())));
            }
        }
        plugin.reward(player);
    }

    public void stopEdit(Player player) {
        editing.remove(player.getUniqueId());
    }

    public void edit(Player player, String courseName) {
        editing.put(player.getUniqueId(), courseName);
    }

    public boolean tryComplete(Player player) {
        ParkourSession session = get(player);
        if (session == null) {
            return false;
        }
        if (session.checkpointIndex() + 1 < session.course().checkpoints().size()) {
            player.sendMessage(plugin.messages().get("finish-checkpoints"));
            return false;
        }
        finish(player);
        return true;
    }

    public String editing(Player player) {
        return editing.get(player.getUniqueId());
    }

    public void clear(Player player) {
        ParkourSession session = sessions.remove(player.getUniqueId());
        standingPlate.remove(player.getUniqueId());
        if (session != null) {
            session.saved().restore(player);
        }
        editing.remove(player.getUniqueId());
    }

    public boolean occupyPlate(Player player, String key) {
        return !key.equals(standingPlate.put(player.getUniqueId(), key));
    }

    public void leftBlock(Player player, Location to) {
        String previous = standingPlate.get(player.getUniqueId());
        if (previous == null || previous.equals(plateKey(to))) {
            return;
        }
        standingPlate.remove(player.getUniqueId());
        ParkourSession session = get(player);
        if (session == null || session.timerRunning()) {
            return;
        }
        Course.Point start = session.course().start();
        if (start != null && previous.equals(plateKey(start))) {
            session.beginTimer();
        }
    }

    private void occupy(Player player, Course.Point point) {
        if (point != null) {
            standingPlate.put(player.getUniqueId(), plateKey(point));
        }
    }

    public static String plateKey(Block block) {
        return plateKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private static String plateKey(Location location) {
        return plateKey(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private static String plateKey(Course.Point point) {
        return plateKey(point.world(), point.x(), point.y(), point.z());
    }

    private static String plateKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public static String format(long millis) {
        long total = Math.max(0L, millis);
        long minutes = total / 60_000L;
        long seconds = (total % 60_000L) / 1000L;
        long rest = total % 1000L;
        if (minutes > 0) {
            return String.format("%d:%02d.%03d", minutes, seconds, rest);
        }
        return String.format("%d.%03d", seconds, rest);
    }

    private void teleportKeepLook(Player player, Course.Point point) {
        if (point == null) {
            return;
        }
        Location dest = point.toPosition(player.getLocation());
        if (dest != null) {
            player.teleport(dest);
        }
    }
}
