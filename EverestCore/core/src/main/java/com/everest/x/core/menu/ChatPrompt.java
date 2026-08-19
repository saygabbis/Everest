package com.everest.x.core.menu;

import com.everest.x.core.EverestCorePlugin;
import com.everest.x.core.spawn.SpawnService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatPrompt implements Listener {

    public enum Kind {
        RENAME,
        CREATE
    }

    public record Pending(Kind kind, String spawnName, long expiresAt) {
    }

    private final EverestCorePlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatPrompt(EverestCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void askRename(Player player, String spawnName) {
        begin(player, new Pending(Kind.RENAME, spawnName, now() + 30_000L));
        player.sendMessage(plugin.messages().get("menu.rename.intro"));
        player.sendMessage(plugin.messages().get("menu.rename.highlight", "name", spawnName));
        player.sendMessage(plugin.messages().get("menu.rename.hint"));
    }

    public void askCreate(Player player) {
        begin(player, new Pending(Kind.CREATE, "", now() + 30_000L));
        player.sendMessage(plugin.messages().get("menu.create.intro"));
        player.sendMessage(plugin.messages().get("menu.create.highlight"));
        player.sendMessage(plugin.messages().get("menu.rename.hint"));
    }

    public boolean isWaiting(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Pending prompt = pending.get(event.getPlayer().getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage() == null ? "" : event.getMessage().trim();
        plugin.scheduler().sync(() -> handle(event.getPlayer(), prompt, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    private void begin(Player player, Pending prompt) {
        pending.put(player.getUniqueId(), prompt);
        plugin.scheduler().later(() -> {
            if (pending.remove(player.getUniqueId(), prompt) && player.isOnline()) {
                player.sendMessage(plugin.messages().get("menu.prompt-expired"));
            }
        }, 20L * 30L);
    }

    private void handle(Player player, Pending prompt, String message) {
        Pending current = pending.get(player.getUniqueId());
        if (current != prompt) {
            return;
        }
        pending.remove(player.getUniqueId());
        if (!player.isOnline()) {
            return;
        }
        if (now() > prompt.expiresAt()) {
            player.sendMessage(plugin.messages().get("menu.prompt-expired"));
            return;
        }
        if (isCancel(message)) {
            player.sendMessage(plugin.messages().get("menu.prompt-cancelled"));
            reopen(player, prompt);
            return;
        }

        String name = SpawnService.normalize(message);
        if (name.isEmpty()) {
            player.sendMessage(plugin.messages().get("command.spawn.invalid-name"));
            reopen(player, prompt);
            return;
        }

        if (prompt.kind() == Kind.CREATE) {
            if (plugin.spawn().exists(name)) {
                player.sendMessage(plugin.messages().get("menu.create.exists", "name", name));
                reopen(player, prompt);
                return;
            }
            if (!plugin.spawn().setFrom(name, player)) {
                player.sendMessage(plugin.messages().get("command.spawn.world-missing", "world", "?"));
                plugin.menus().openSpawns(player);
                return;
            }
            player.sendMessage(plugin.messages().get("menu.create.done", "name", name));
            plugin.menus().openSpawnEdit(player, name);
            return;
        }

        if (name.equals(prompt.spawnName())) {
            plugin.menus().openSpawnEdit(player, name);
            return;
        }
        if (plugin.spawn().exists(name)) {
            player.sendMessage(plugin.messages().get("menu.rename.exists", "name", name));
            plugin.menus().openSpawnEdit(player, prompt.spawnName());
            return;
        }
        if (!plugin.spawn().rename(prompt.spawnName(), name)) {
            player.sendMessage(plugin.messages().get("menu.rename.fail"));
            plugin.menus().openSpawnEdit(player, prompt.spawnName());
            return;
        }
        player.sendMessage(plugin.messages().get("menu.rename.done", "name", name));
        plugin.menus().openSpawnEdit(player, name);
    }

    private void reopen(Player player, Pending prompt) {
        if (prompt.kind() == Kind.CREATE) {
            plugin.menus().openSpawns(player);
            return;
        }
        plugin.menus().openSpawnEdit(player, prompt.spawnName());
    }

    private static boolean isCancel(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.equals("cancelar") || lower.equals("cancel") || lower.equals("sair");
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
