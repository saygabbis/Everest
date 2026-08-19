package com.everest.x.parkour.session;

import com.everest.x.parkour.EverestParkourPlugin;
import com.everest.x.parkour.course.Course;
import com.everest.x.parkour.course.HologramService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

public final class SessionListener implements Listener {

    private final EverestParkourPlugin plugin;

    public SessionListener(EverestParkourPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlate(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!isPlate(block.getType())) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.sessions().occupyPlate(player, SessionService.plateKey(block))) {
            return;
        }

        Location location = block.getLocation();
        ParkourSession session = plugin.sessions().get(player);

        if (session != null) {
            Course course = session.course();
            if (course.end() != null && course.end().matches(location)) {
                plugin.sessions().tryComplete(player);
                return;
            }
            for (int i = 0; i < course.checkpoints().size(); i++) {
                if (course.checkpoints().get(i).matches(location) && i == session.checkpointIndex() + 1) {
                    plugin.sessions().checkpoint(player, i);
                    return;
                }
            }
            if (course.start() != null && course.start().matches(location)) {
                if (!session.timerRunning()) {
                    return;
                }
                plugin.sessions().join(player, course, true);
            }
            return;
        }

        Course started = plugin.courses().byStart(location);
        if (started != null && started.isReady() && player.hasPermission("everest.parkour.play")) {
            plugin.sessions().join(player, started, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.sessions().inParkour(player)) {
            return;
        }
        ItemStack item = player.getItemInHand();
        ParkourKit.Action kit = ParkourKit.actionOf(item);
        if (kit == null) {
            return;
        }
        event.setCancelled(true);
        switch (kit) {
            case RESTART -> plugin.sessions().restartAtStart(player);
            case CHECKPOINT -> plugin.sessions().returnToCheckpoint(player);
            case LEAVE -> plugin.sessions().leave(player, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.sessions().inParkour(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (plugin.sessions().inParkour(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && plugin.sessions().inParkour(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && plugin.sessions().inParkour(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreative(InventoryCreativeEvent event) {
        if (event.getWhoClicked() instanceof Player player && plugin.sessions().inParkour(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null) {
            return;
        }
        Player player = event.getPlayer();
        ParkourSession session = plugin.sessions().get(player);
        if (session != null && to.getY() < session.course().resolvedFailY()) {
            plugin.sessions().fail(player);
            return;
        }
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        plugin.sessions().leftBlock(player, to);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (HologramService.isHologram(event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player player && plugin.sessions().inParkour(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (HologramService.isHologram(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && plugin.sessions().inParkour(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFly(PlayerToggleFlightEvent event) {
        if (event.isFlying() && plugin.sessions().inParkour(event.getPlayer()) && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            event.getPlayer().setFlying(false);
            event.getPlayer().setAllowFlight(false);
        }
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        if (plugin.sessions().inParkour(event.getPlayer())) {
            plugin.sessions().leave(event.getPlayer(), true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.sessions().clear(event.getPlayer());
    }

    private static boolean isPlate(Material material) {
        return material == Material.GOLD_PLATE
                || material == Material.IRON_PLATE
                || material == Material.STONE_PLATE
                || material == Material.WOOD_PLATE;
    }
}
