package com.everest.x.core.command;

import com.everest.x.core.EverestCorePlugin;
import com.everest.x.core.user.User;
import com.everest.x.core.util.Pings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class EverestCommand implements CommandExecutor {

    private final EverestCorePlugin plugin;

    public EverestCommand(EverestCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return reload(sender);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("hooks")) {
            sender.sendMessage(plugin.messages().get("command.hooks",
                    "hooks", plugin.hooks().summary()));
            return true;
        }
        return info(sender);
    }

    private boolean info(CommandSender sender) {
        String coins = "-";
        String ping = "-";
        if (sender instanceof Player player) {
            ping = String.valueOf(Pings.of(player));
            User user = plugin.users().get(player.getUniqueId());
            coins = user == null ? "..." : String.valueOf(user.getCoins());
        }
        for (String line : plugin.messages().getList("command.info",
                "version", plugin.getDescription().getVersion(),
                "server-id", plugin.settings().serverId(),
                "server-type", plugin.settings().serverType().name(),
                "ping", ping,
                "coins", coins,
                "hooks", plugin.hooks().summary())) {
            sender.sendMessage(line);
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("everest.admin")) {
            sender.sendMessage(plugin.messages().get("command.no-permission"));
            return true;
        }
        plugin.reloadCore();
        sender.sendMessage(plugin.messages().get("command.reloaded"));
        return true;
    }
}
