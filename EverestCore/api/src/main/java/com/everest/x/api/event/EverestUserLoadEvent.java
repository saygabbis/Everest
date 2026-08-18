package com.everest.x.api.event;

import com.everest.x.api.EverestUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Disparado na thread principal quando o perfil do jogador terminou de carregar.
 */
public class EverestUserLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EverestUser user;

    public EverestUserLoadEvent(EverestUser user) {
        this.user = user;
    }

    public EverestUser getUser() {
        return user;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(user.getUniqueId());
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
