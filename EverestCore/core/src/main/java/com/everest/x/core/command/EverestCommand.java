package com.everest.x.core.command;

import com.everest.x.core.EverestCorePlugin;
import com.everest.x.core.spawn.SpawnService;
import com.everest.x.core.user.User;
import com.everest.x.core.util.Pings;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class EverestCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList("reload", "hooks", "config", "spawn", "setspawn");

    private final EverestCorePlugin plugin;

    public EverestCommand(EverestCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawn")) {
            return spawn(sender, args);
        }
        if (command.getName().equalsIgnoreCase("setspawn")) {
            return setSpawn(sender, args);
        }
        if (args.length == 0) {
            return info(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload":
                return reload(sender);
            case "hooks":
                sender.sendMessage(plugin.messages().get("command.hooks",
                        "hooks", plugin.hooks().summary()));
                return true;
            case "config":
                return config(sender);
            case "spawn":
                return spawn(sender, Arrays.copyOfRange(args, 1, args.length));
            case "setspawn":
                return setSpawn(sender, Arrays.copyOfRange(args, 1, args.length));
            default:
                return info(sender);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String root = command.getName().toLowerCase(Locale.ROOT);
        if (root.equals("spawn") || root.equals("setspawn")) {
            return args.length == 1 ? matchingSpawns(args[0]) : Collections.emptyList();
        }
        if (args.length == 2
                && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("setspawn"))) {
            return matchingSpawns(args[1]);
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBS.stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .filter(sub -> allowed(sender, sub))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return Collections.emptyList();
    }

    private List<String> matchingSpawns(String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return plugin.spawn().names().stream()
                .filter(name -> name.startsWith(prefix))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean allowed(CommandSender sender, String sub) {
        switch (sub) {
            case "reload":
            case "config":
                return sender.hasPermission("everest.admin");
            case "setspawn":
                return sender.hasPermission("everest.setspawn") || sender.hasPermission("everest.admin");
            case "spawn":
                return sender.hasPermission("everest.spawn");
            default:
                return true;
        }
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
                "hooks", plugin.hooks().summary(),
                "spawn", spawnStatus())) {
            sender.sendMessage(line);
        }
        return true;
    }

    private String spawnStatus() {
        SpawnService spawn = plugin.spawn();
        if (!spawn.isEnabled()) {
            return "desativado";
        }
        if (!spawn.isConfigured()) {
            return "não definido";
        }
        Location location = spawn.location();
        if (location == null) {
            return spawn.motherName() + " (mundo ausente)";
        }
        return spawn.motherName() + " · " + location.getWorld().getName()
                + " "
                + format(location.getX()) + ", "
                + format(location.getY()) + ", "
                + format(location.getZ());
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
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

    private boolean config(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().get("command.players-only"));
            return true;
        }
        if (!player.hasPermission("everest.admin")) {
            player.sendMessage(plugin.messages().get("command.no-permission"));
            return true;
        }
        plugin.configMenu().open(player);
        return true;
    }

    private boolean spawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().get("command.players-only"));
            return true;
        }
        if (!player.hasPermission("everest.spawn")) {
            player.sendMessage(plugin.messages().get("command.no-permission"));
            return true;
        }
        SpawnService spawn = plugin.spawn();
        if (!spawn.isEnabled()) {
            player.sendMessage(plugin.messages().get("command.spawn.disabled"));
            return true;
        }
        if (!spawn.isConfigured()) {
            player.sendMessage(plugin.messages().get("command.spawn.missing"));
            return true;
        }
        String name = args.length == 0 ? spawn.motherName() : SpawnService.normalize(args[0]);
        if (name.isEmpty() || !spawn.exists(name)) {
            player.sendMessage(plugin.messages().get("command.spawn.not-found",
                    "name", args.length == 0 ? "?" : args[0]));
            return true;
        }
        if (!spawn.teleport(player, name)) {
            player.sendMessage(plugin.messages().get("command.spawn.world-missing",
                    "world", spawn.worldName(name)));
            return true;
        }
        player.sendMessage(plugin.messages().get("command.spawn.teleported", "name", name));
        return true;
    }

    private boolean setSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().get("command.players-only"));
            return true;
        }
        if (!player.hasPermission("everest.setspawn") && !player.hasPermission("everest.admin")) {
            player.sendMessage(plugin.messages().get("command.no-permission"));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(plugin.messages().get("command.spawn.set-usage"));
            return true;
        }
        String name = SpawnService.normalize(args[0]);
        if (name.isEmpty()) {
            player.sendMessage(plugin.messages().get("command.spawn.invalid-name"));
            return true;
        }
        if (!plugin.spawn().setFrom(name, player)) {
            player.sendMessage(plugin.messages().get("command.spawn.world-missing", "world", "?"));
            return true;
        }
        Location location = player.getLocation();
        player.sendMessage(plugin.messages().get("command.spawn.set",
                "name", name,
                "world", location.getWorld().getName(),
                "x", format(location.getX()),
                "y", format(location.getY()),
                "z", format(location.getZ())));
        return true;
    }
}
