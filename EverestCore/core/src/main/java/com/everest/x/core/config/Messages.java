package com.everest.x.core.config;

import com.everest.x.core.util.Colors;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Messages {

    private final JavaPlugin plugin;
    private FileConfiguration yaml;
    private String prefix;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        InputStream bundled = plugin.getResource("messages.yml");
        if (bundled != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }
        prefix = Colors.color(yaml.getString("prefix", "&6Everest &8» &7"));
    }

    public String prefix() {
        return prefix;
    }

    public String get(String path) {
        String raw = yaml.getString(path);
        if (raw == null) {
            return prefix + path;
        }
        return Colors.color(raw.replace("{prefix}", prefix));
    }

    public String get(String path, String... replacements) {
        String text = get(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return text;
    }

    public List<String> getList(String path, String... replacements) {
        List<String> source = yaml.getStringList(path);
        if (source.isEmpty()) {
            return Collections.singletonList(get(path, replacements));
        }
        List<String> colored = new ArrayList<>(source.size());
        for (String line : source) {
            String text = Colors.color(line.replace("{prefix}", prefix));
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
            colored.add(text);
        }
        return colored;
    }
}
