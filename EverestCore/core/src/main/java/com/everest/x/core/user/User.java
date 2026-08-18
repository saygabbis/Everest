package com.everest.x.core.user;

import com.everest.x.api.EverestUser;
import com.everest.x.api.event.EverestCoinsChangeEvent;
import com.everest.x.core.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class User implements EverestUser {

    private final UUID uuid;
    private final long firstJoin;
    private volatile String name;
    private volatile long coins;
    private volatile long lastJoin;
    private JavaPlugin plugin;
    private Scheduler scheduler;
    private Runnable persist;

    public User(UUID uuid, String name, long coins, long firstJoin, long lastJoin) {
        this.uuid = uuid;
        this.name = name;
        this.coins = coins;
        this.firstJoin = firstJoin;
        this.lastJoin = lastJoin;
    }

    public void attach(JavaPlugin plugin, Scheduler scheduler, Runnable persist) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.persist = persist;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getCoins() {
        return coins;
    }

    @Override
    public synchronized void setCoins(long amount) {
        applyCoins(Math.max(0L, amount));
    }

    @Override
    public synchronized void addCoins(long amount) {
        if (amount == 0L) {
            return;
        }
        applyCoins(Math.max(0L, coins + amount));
    }

    @Override
    public synchronized boolean takeCoins(long amount) {
        if (amount <= 0L) {
            return true;
        }
        if (coins < amount) {
            return false;
        }
        applyCoins(coins - amount);
        return true;
    }

    @Override
    public long getFirstJoin() {
        return firstJoin;
    }

    @Override
    public long getLastJoin() {
        return lastJoin;
    }

    public void setLastJoin(long lastJoin) {
        this.lastJoin = lastJoin;
    }

    private void applyCoins(long newAmount) {
        long old = this.coins;
        if (old == newAmount) {
            return;
        }
        this.coins = newAmount;
        if (persist != null) {
            persist.run();
        }
        if (scheduler == null || plugin == null || !plugin.isEnabled()) {
            return;
        }
        scheduler.sync(() -> Bukkit.getPluginManager().callEvent(
                new EverestCoinsChangeEvent(this, old, newAmount)));
    }
}
