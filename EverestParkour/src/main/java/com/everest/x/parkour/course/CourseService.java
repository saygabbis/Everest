package com.everest.x.parkour.course;

import com.everest.x.parkour.EverestParkourPlugin;
import com.everest.x.parkour.util.YamlFiles;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class CourseService {

    private final EverestParkourPlugin plugin;
    private final File file;
    private final Map<String, Course> courses = new LinkedHashMap<>();

    public CourseService(EverestParkourPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "courses.yml");
        reload();
    }

    public void reload() {
        courses.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlFiles.load(file);
        ConfigurationSection section = yaml.getConfigurationSection("courses");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String name = normalize(key);
            ConfigurationSection data = section.getConfigurationSection(key);
            if (!name.isEmpty() && data != null) {
                courses.put(name, Course.read(name, data));
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("courses");
        for (Course course : courses.values()) {
            course.write(root.createSection(course.name()));
        }
        try {
            YamlFiles.save(yaml, file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao salvar courses.yml", exception);
        }
    }

    public Course create(String rawName) {
        String name = normalize(rawName);
        if (name.isEmpty() || courses.containsKey(name)) {
            return null;
        }
        Course course = new Course(name);
        courses.put(name, course);
        save();
        return course;
    }

    public boolean delete(String rawName) {
        Course removed = courses.remove(normalize(rawName));
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public Course get(String rawName) {
        return courses.get(normalize(rawName));
    }

    public boolean exists(String rawName) {
        return courses.containsKey(normalize(rawName));
    }

    public Collection<Course> all() {
        return Collections.unmodifiableCollection(courses.values());
    }

    public List<String> names() {
        return new ArrayList<>(courses.keySet());
    }

    public Course byStart(Location location) {
        for (Course course : courses.values()) {
            if (course.start() != null && course.start().matches(location)) {
                return course;
            }
        }
        return null;
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.trim().toLowerCase(new Locale("pt", "BR"));
        if (normalized.isEmpty() || normalized.length() > 32) {
            return "";
        }
        return normalized.matches("[\\p{L}0-9_-]{1,32}") ? normalized : "";
    }
}
