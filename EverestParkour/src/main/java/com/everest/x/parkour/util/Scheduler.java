package com.everest.x.parkour.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class Scheduler {

    private final JavaPlugin plugin;
    private final BukkitScheduler bukkit;

    public Scheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bukkit = plugin.getServer().getScheduler();
    }

    public void sync(Runnable task) {
        if (plugin.getServer().isPrimaryThread()) {
            task.run();
            return;
        }
        bukkit.runTask(plugin, task);
    }

    public void later(Runnable task, long ticks) {
        bukkit.runTaskLater(plugin, task, ticks);
    }
}
