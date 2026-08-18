package com.everest.x.core.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Ping em 1.8.8 não existe na API. Lê o campo NMS {@code ping} por reflection.
 */
public final class Pings {

    private static Method getHandle;
    private static Field pingField;

    private Pings() {
    }

    public static int of(Player player) {
        try {
            if (getHandle == null) {
                getHandle = player.getClass().getMethod("getHandle");
            }
            Object handle = getHandle.invoke(player);
            if (pingField == null) {
                pingField = handle.getClass().getField("ping");
            }
            return pingField.getInt(handle);
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }
}
