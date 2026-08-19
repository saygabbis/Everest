package com.everest.x.parkour;

import com.everest.x.parkour.command.ParkourCommand;
import com.everest.x.parkour.config.Messages;
import com.everest.x.parkour.course.CourseService;
import com.everest.x.parkour.course.HologramService;
import com.everest.x.parkour.hook.CoreAccess;
import com.everest.x.parkour.session.SessionListener;
import com.everest.x.parkour.session.SessionService;
import com.everest.x.parkour.util.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EverestParkourPlugin extends JavaPlugin {

    private Scheduler scheduler;
    private Messages messages;
    private CourseService courses;
    private SessionService sessions;
    private HologramService holograms;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        scheduler = new Scheduler(this);
        messages = new Messages(this);
        courses = new CourseService(this);
        sessions = new SessionService(this);
        holograms = new HologramService(this);

        getServer().getPluginManager().registerEvents(new SessionListener(this), this);
        ParkourCommand command = new ParkourCommand(this);
        getCommand("parkour").setExecutor(command);
        getCommand("parkour").setTabCompleter(command);

        if (getServer().getPluginManager().isPluginEnabled("EverestCore")) {
            CoreAccess.register(this);
        }
        scheduler.later(holograms::spawnAll, 20L);

        getLogger().info("Parkour pronto - " + courses.all().size() + " pista(s).");
    }

    @Override
    public void onDisable() {
        if (holograms != null) {
            holograms.despawnAll();
        }
        if (sessions != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                sessions.clear(player);
            }
        }
    }

    public void reloadParkour() {
        reloadConfig();
        messages.reload();
        courses.reload();
        holograms.spawnAll();
    }

    public void reward(Player player) {
        if (getServer().getPluginManager().isPluginEnabled("EverestCore")) {
            CoreAccess.reward(player, this);
        }
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public Messages messages() {
        return messages;
    }

    public CourseService courses() {
        return courses;
    }

    public SessionService sessions() {
        return sessions;
    }

    public HologramService holograms() {
        return holograms;
    }
}
