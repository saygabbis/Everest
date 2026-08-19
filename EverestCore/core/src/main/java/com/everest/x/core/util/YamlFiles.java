package com.everest.x.core.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * YAML sempre em UTF-8. No Windows o Bukkit 1.8 lê com o charset do sistema
 * e os acentos (ã, é, ç) quebram no chat e nos menus.
 */
public final class YamlFiles {

    private YamlFiles() {
    }

    public static YamlConfiguration load(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (file == null || !file.exists()) {
            return yaml;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            yaml.load(reader);
        } catch (IOException | InvalidConfigurationException exception) {
            Logger.getLogger("EverestCore").log(Level.WARNING, "Falha ao ler " + file.getName(), exception);
        }
        return yaml;
    }

    public static YamlConfiguration load(InputStream stream) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (stream == null) {
            return yaml;
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            yaml.load(reader);
        } catch (IOException | InvalidConfigurationException exception) {
            Logger.getLogger("EverestCore").log(Level.WARNING, "Falha ao ler YAML do jar", exception);
        }
        return yaml;
    }

    public static void save(FileConfiguration yaml, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Não foi possível criar " + parent.getAbsolutePath());
        }
        String data = yaml.saveToString();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(data);
        }
    }
}
