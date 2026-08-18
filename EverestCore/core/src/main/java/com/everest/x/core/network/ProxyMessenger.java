package com.everest.x.core.network;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Canal BungeeCord / Velocity. Sem proxy, o pacote é ignorado pelo servidor.
 */
public final class ProxyMessenger {

    public static final String CHANNEL = "BungeeCord";

    private final JavaPlugin plugin;

    public ProxyMessenger(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void sendToServer(Player player, String serverId) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(serverId);
            player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Não foi possível enviar " + player.getName()
                    + " para " + serverId, exception);
        }
    }
}
