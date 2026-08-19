package com.everest.x.parkour.command;

import com.everest.x.parkour.EverestParkourPlugin;
import com.everest.x.parkour.course.Course;
import com.everest.x.parkour.course.CourseService;
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

public final class ParkourCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAY = Arrays.asList("join", "leave", "list", "ajuda", "help");
    private static final List<String> ADMIN = Arrays.asList(
            "create", "edit", "delete", "setstart", "addcp", "finish", "failheight", "reload");

    private final EverestParkourPlugin plugin;

    public ParkourCommand(EverestParkourPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return help(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "ajuda", "help" -> help(sender);
            case "list" -> list(sender);
            case "join" -> join(sender, args);
            case "leave" -> leave(sender);
            case "create" -> create(sender, args);
            case "edit" -> edit(sender, args);
            case "delete" -> delete(sender, args);
            case "setstart" -> setStart(sender);
            case "addcp", "checkpoint" -> addCheckpoint(sender);
            case "finish", "setend" -> finish(sender, args);
            case "failheight", "setfail" -> failHeight(sender);
            case "reload" -> reload(sender);
            default -> {
                sender.sendMessage(plugin.messages().get("unknown"));
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>(PLAY);
            if (sender.hasPermission("everest.parkour.admin")) {
                options.addAll(ADMIN);
            }
            return options.stream()
                    .filter(option -> option.startsWith(prefix))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        if (args.length == 2 && isCourseArg(args[0])) {
            String prefix = args[1].toLowerCase(new Locale("pt", "BR"));
            return plugin.courses().names().stream()
                    .filter(name -> name.startsWith(prefix))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return Collections.emptyList();
    }

    private boolean help(CommandSender sender) {
        for (String line : plugin.messages().getList("help")) {
            sender.sendMessage(line);
        }
        return true;
    }

    private boolean list(CommandSender sender) {
        List<String> names = plugin.courses().names();
        if (names.isEmpty()) {
            sender.sendMessage(plugin.messages().get("list-empty"));
            return true;
        }
        sender.sendMessage(plugin.messages().get("list-header"));
        for (Course course : plugin.courses().all()) {
            sender.sendMessage(plugin.messages().get("list-line",
                    "name", course.name(),
                    "status", course.isReady()
                            ? plugin.messages().get("ready")
                            : plugin.messages().get("incomplete")));
        }
        return true;
    }

    private boolean join(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        if (!player.hasPermission("everest.parkour.play")) {
            player.sendMessage(plugin.messages().get("no-permission"));
            return true;
        }
        if (args.length < 2) {
            return help(sender);
        }
        Course course = plugin.courses().get(args[1]);
        if (course == null) {
            player.sendMessage(plugin.messages().get("not-found", "name", args[1]));
            return true;
        }
        if (!course.isReady()) {
            player.sendMessage(plugin.messages().get("not-ready"));
            return true;
        }
        var session = plugin.sessions().get(player);
        if (session != null && !session.course().name().equals(course.name())) {
            player.sendMessage(plugin.messages().get("already", "name", session.course().name()));
            return true;
        }
        plugin.sessions().join(player, course, session != null);
        if (course.start() != null && !course.start().matches(player.getLocation())) {
            plugin.sessions().warpKeepLook(player, course.start());
        }
        return true;
    }

    private boolean leave(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return true;
        }
        if (!plugin.sessions().inParkour(player)) {
            player.sendMessage(plugin.messages().get("not-in"));
            return true;
        }
        plugin.sessions().leave(player, true);
        return true;
    }

    private boolean create(CommandSender sender, String[] args) {
        Player player = admin(sender);
        if (player == null) {
            return true;
        }
        if (args.length < 2) {
            return help(sender);
        }
        String name = CourseService.normalize(args[1]);
        if (name.isEmpty()) {
            player.sendMessage(plugin.messages().get("invalid-name"));
            return true;
        }
        if (plugin.courses().exists(name)) {
            player.sendMessage(plugin.messages().get("exists", "name", name));
            return true;
        }
        if (!guardSingleEdit(player, null)) {
            return true;
        }
        Course created = plugin.courses().create(name);
        if (created == null) {
            player.sendMessage(plugin.messages().get("exists", "name", name));
            return true;
        }
        plugin.sessions().edit(player, name);
        player.sendMessage(plugin.messages().get("created", "name", name));
        return true;
    }

    private boolean edit(CommandSender sender, String[] args) {
        Player player = admin(sender);
        if (player == null) {
            return true;
        }
        if (args.length < 2) {
            return help(sender);
        }
        Course course = plugin.courses().get(args[1]);
        if (course == null) {
            player.sendMessage(plugin.messages().get("not-found", "name", args[1]));
            return true;
        }
        if (!guardSingleEdit(player, course.name())) {
            return true;
        }
        plugin.sessions().edit(player, course.name());
        player.sendMessage(plugin.messages().get("editing", "name", course.name()));
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        Player player = admin(sender);
        if (player == null) {
            return true;
        }
        if (args.length < 2) {
            return help(sender);
        }
        String name = CourseService.normalize(args[1]);
        Course course = plugin.courses().get(name);
        if (course == null || !plugin.courses().delete(name)) {
            player.sendMessage(plugin.messages().get("not-found", "name", args[1]));
            return true;
        }
        plugin.holograms().remove(course);
        player.sendMessage(plugin.messages().get("deleted", "name", name));
        return true;
    }

    private boolean setStart(CommandSender sender) {
        Player player = admin(sender);
        Course course = editing(player);
        if (course == null) {
            return true;
        }
        course.setStart(Course.Point.from(player.getLocation()));
        plugin.courses().save();
        plugin.holograms().refresh(course);
        player.sendMessage(plugin.messages().get("set-start", "name", course.name()));
        return true;
    }

    private boolean addCheckpoint(CommandSender sender) {
        Player player = admin(sender);
        Course course = editing(player);
        if (course == null) {
            return true;
        }
        course.checkpoints().add(Course.Point.from(player.getLocation()));
        plugin.courses().save();
        plugin.holograms().refresh(course);
        player.sendMessage(plugin.messages().get("add-checkpoint",
                "name", course.name(),
                "index", String.valueOf(course.checkpoints().size())));
        return true;
    }

    private boolean finish(CommandSender sender, String[] args) {
        Player player = admin(sender);
        Course course = editing(player);
        if (course == null) {
            return true;
        }
        if (args.length >= 2) {
            String name = CourseService.normalize(args[1]);
            if (!course.name().equals(name)) {
                player.sendMessage(plugin.messages().get("already-editing", "name", course.name()));
                return true;
            }
        }
        if (course.start() == null) {
            player.sendMessage(plugin.messages().get("need-start"));
            return true;
        }
        if (course.end() == null) {
            course.setEnd(Course.Point.from(player.getLocation()));
            plugin.courses().save();
            plugin.holograms().refresh(course);
            player.sendMessage(plugin.messages().get("set-end", "name", course.name()));
        }
        plugin.sessions().stopEdit(player);
        player.sendMessage(plugin.messages().get("edit-done", "name", course.name()));
        return true;
    }

    private boolean guardSingleEdit(Player player, String target) {
        String editing = plugin.sessions().editing(player);
        if (editing == null || editing.equals(target)) {
            return true;
        }
        player.sendMessage(plugin.messages().get("already-editing", "name", editing));
        return false;
    }

    private boolean failHeight(CommandSender sender) {
        Player player = admin(sender);
        Course course = editing(player);
        if (course == null) {
            return true;
        }
        double y = player.getLocation().getY();
        course.setFailY(y);
        plugin.courses().save();
        player.sendMessage(plugin.messages().get("set-fail",
                "name", course.name(),
                "y", String.format(Locale.US, "%.3f", y)));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("everest.parkour.admin")) {
            sender.sendMessage(plugin.messages().get("no-permission"));
            return true;
        }
        plugin.reloadParkour();
        sender.sendMessage(plugin.messages().get("reloaded"));
        return true;
    }

    private Course editing(Player player) {
        if (player == null) {
            return null;
        }
        String name = plugin.sessions().editing(player);
        if (name == null) {
            player.sendMessage(plugin.messages().get("need-edit"));
            return null;
        }
        Course course = plugin.courses().get(name);
        if (course == null) {
            player.sendMessage(plugin.messages().get("not-found", "name", name));
        }
        return course;
    }

    private Player admin(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return null;
        }
        if (!player.hasPermission("everest.parkour.admin")) {
            player.sendMessage(plugin.messages().get("no-permission"));
            return null;
        }
        return player;
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(plugin.messages().get("players-only"));
        return null;
    }

    private static boolean isCourseArg(String sub) {
        String lower = sub.toLowerCase(Locale.ROOT);
        return lower.equals("join") || lower.equals("edit") || lower.equals("delete") || lower.equals("finish");
    }
}
