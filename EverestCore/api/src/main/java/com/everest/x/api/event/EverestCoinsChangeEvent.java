package com.everest.x.api.event;

import com.everest.x.api.EverestUser;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Saldo de coins da rede mudou. Outros plugins escutam; não cancelável na v1.
 */
public class EverestCoinsChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EverestUser user;
    private final long oldAmount;
    private final long newAmount;

    public EverestCoinsChangeEvent(EverestUser user, long oldAmount, long newAmount) {
        this.user = user;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
    }

    public EverestUser getUser() {
        return user;
    }

    public long getOldAmount() {
        return oldAmount;
    }

    public long getNewAmount() {
        return newAmount;
    }

    public long getDelta() {
        return newAmount - oldAmount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
