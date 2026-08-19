package com.everest.x.parkour.util;

import org.bukkit.ChatColor;

public final class Colors {

    private Colors() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
