package com.everest.x.core.menu;

import com.everest.x.core.EverestCorePlugin;
import com.everest.x.core.util.Colors;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ConfigMenu {

    private final EverestCorePlugin plugin;

    private FileConfiguration yaml;
    private int size;
    private int spawnSlot;
    private int backSlot;
    private int closeSlot;

    public ConfigMenu(EverestCorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "menus.yml");
        if (!file.exists()) {
            plugin.saveResource("menus.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        InputStream bundled = plugin.getResource("menus.yml");
        if (bundled != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(bundled, StandardCharsets.UTF_8)));
        }

        int rows = Math.max(1, Math.min(6, yaml.getInt("config.rows", 4)));
        size = rows * 9;
        spawnSlot = validSlot(yaml.getInt("config.items.spawn.slot", 13), 13);
        backSlot = validSlot(yaml.getInt("config.navigation.back.slot", size - 9), size - 9);
        closeSlot = validSlot(yaml.getInt("config.navigation.close.slot", size - 1), size - 1);
    }

    public void open(Player player) {
        ConfigMenuHolder holder = new ConfigMenuHolder();
        Inventory inventory = plugin.getServer().createInventory(
                holder,
                size,
                Colors.color(yaml.getString("config.title", "&8Configuração do Everest")));
        holder.inventory = inventory;

        inventory.setItem(spawnSlot, item("config.items.spawn", Material.BLAZE_ROD));
        inventory.setItem(backSlot, item("config.navigation.back", Material.ARROW));
        inventory.setItem(closeSlot, item("config.navigation.close", Material.BARRIER));
        player.openInventory(inventory);
    }

    public int spawnSlot() {
        return spawnSlot;
    }

    public int backSlot() {
        return backSlot;
    }

    public int closeSlot() {
        return closeSlot;
    }

    private ItemStack item(String path, Material fallback) {
        Material material = Material.matchMaterial(yaml.getString(path + ".material", fallback.name()));
        if (material == null || material == Material.AIR) {
            material = fallback;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(Colors.color(yaml.getString(path + ".name", "&fItem")));
        List<String> lore = new ArrayList<>();
        for (String line : yaml.getStringList(path + ".lore")) {
            lore.add(Colors.color(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int validSlot(int configured, int fallback) {
        return configured >= 0 && configured < size ? configured : fallback;
    }

    public static final class ConfigMenuHolder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
