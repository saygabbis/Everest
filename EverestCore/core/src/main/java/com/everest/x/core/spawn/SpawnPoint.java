package com.everest.x.core.spawn;

import org.bukkit.Material;

/**
 * Um spawn nomeado deste servidor, com ícone e flags de join/respawn.
 */
public final class SpawnPoint {

    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final Material icon;
    private final short iconData;
    private final boolean logoutReturn;
    private final boolean worldRespawn;

    public SpawnPoint(
            String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            Material icon,
            short iconData,
            boolean logoutReturn,
            boolean worldRespawn) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.icon = icon == null || icon == Material.AIR ? Material.DIRT : icon;
        this.iconData = iconData;
        this.logoutReturn = logoutReturn;
        this.worldRespawn = worldRespawn;
    }

    public String world() {
        return world;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public Material icon() {
        return icon;
    }

    public short iconData() {
        return iconData;
    }

    public boolean logoutReturn() {
        return logoutReturn;
    }

    public boolean worldRespawn() {
        return worldRespawn;
    }

    public SpawnPoint withFlags(boolean logoutReturn, boolean worldRespawn) {
        return new SpawnPoint(world, x, y, z, yaw, pitch, icon, iconData, logoutReturn, worldRespawn);
    }

    public SpawnPoint withIcon(Material icon, short iconData) {
        return new SpawnPoint(world, x, y, z, yaw, pitch, icon, iconData, logoutReturn, worldRespawn);
    }

    public SpawnPoint withLocation(String world, double x, double y, double z, float yaw, float pitch) {
        return new SpawnPoint(world, x, y, z, yaw, pitch, icon, iconData, logoutReturn, worldRespawn);
    }
}
