package com.everest.x.parkour.session;

import com.everest.x.parkour.course.Course;

import java.util.UUID;

public final class ParkourSession {

    private final UUID playerId;
    private final Course course;
    private final SavedInventory saved;
    private long startedAt;
    private int checkpointIndex = -1;
    private long lastFailAt;

    public ParkourSession(UUID playerId, Course course, SavedInventory saved) {
        this.playerId = playerId;
        this.course = course;
        this.saved = saved;
        this.startedAt = 0L;
    }

    public UUID playerId() {
        return playerId;
    }

    public Course course() {
        return course;
    }

    public SavedInventory saved() {
        return saved;
    }

    public int checkpointIndex() {
        return checkpointIndex;
    }

    public void reachCheckpoint(int index) {
        this.checkpointIndex = index;
    }

    public void beginTimer() {
        if (startedAt == 0L) {
            startedAt = System.currentTimeMillis();
        }
    }

    public boolean timerRunning() {
        return startedAt > 0L;
    }

    public long elapsed() {
        if (startedAt == 0L) {
            return 0L;
        }
        return System.currentTimeMillis() - startedAt;
    }

    public boolean tryFail() {
        long now = System.currentTimeMillis();
        if (now - lastFailAt < 400L) {
            return false;
        }
        lastFailAt = now;
        return true;
    }

    public Course.Point respawnPoint() {
        if (checkpointIndex >= 0 && checkpointIndex < course.checkpoints().size()) {
            return course.checkpoints().get(checkpointIndex);
        }
        return course.start();
    }

    public boolean atStart() {
        return checkpointIndex < 0;
    }
}
