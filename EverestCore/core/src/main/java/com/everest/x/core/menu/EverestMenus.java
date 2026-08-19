package com.everest.x.core.menu;

import com.everest.x.core.EverestCorePlugin;
import com.everest.x.core.spawn.SpawnPoint;
import com.everest.x.core.spawn.SpawnService;
import com.everest.x.core.util.Colors;
import com.everest.x.core.util.YamlFiles;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EverestMenus {

    private static final int[] SPAWN_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final EverestCorePlugin plugin;
    private FileConfiguration yaml;

    public EverestMenus(EverestCorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "menus.yml");
        if (!file.exists()) {
            plugin.saveResource("menus.yml", false);
        }
        yaml = YamlFiles.load(file);
        yaml.setDefaults(YamlFiles.load(plugin.getResource("menus.yml")));
    }

    public void openHome(Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.View.HOME, null);
        Inventory inventory = create(holder, 4, "home.title", "&8Configuração do Everest");
        fill(inventory);
        inventory.setItem(13, named(
                Material.BLAZE_ROD,
                "home.items.spawns.name", "&6Spawns",
                lore("home.items.spawns.lore",
                        "&7Gerencie os pontos de spawn",
                        "&7deste servidor.",
                        "",
                        "&eClique para abrir.")));
        putNav(inventory, true);
        player.openInventory(inventory);
    }

    public void openSpawns(Player player) {
        MenuHolder holder = new MenuHolder(MenuHolder.View.SPAWNS, null);
        Inventory inventory = create(holder, 5, "spawns.title", "&8Spawns");
        fill(inventory);

        List<String> names = plugin.spawn().nameList();
        if (names.isEmpty()) {
            inventory.setItem(22, named(
                    Material.EMPTY_MAP,
                    "spawns.empty.name", "&cNenhum spawn",
                    lore("spawns.empty.lore",
                            "&7Crie um com o esmeralda abaixo",
                            "&7ou use &f/setspawn <nome>&7.")));
        } else {
            int limit = Math.min(names.size(), SPAWN_SLOTS.length);
            for (int i = 0; i < limit; i++) {
                inventory.setItem(SPAWN_SLOTS[i], spawnIcon(names.get(i)));
            }
        }
        inventory.setItem(40, named(
                Material.EMERALD,
                "spawns.create.name", "&aNovo spawn",
                lore("spawns.create.lore",
                        "&7Cria um spawn na sua posição.",
                        "",
                        "&eClique e digite o nome no chat.")));
        putNav(inventory, false);
        player.openInventory(inventory);
    }

    public void openSpawnEdit(Player player, String spawnName) {
        SpawnService spawn = plugin.spawn();
        if (!spawn.exists(spawnName)) {
            openSpawns(player);
            return;
        }
        SpawnPoint point = spawn.point(spawnName);
        MenuHolder holder = new MenuHolder(MenuHolder.View.EDIT, spawnName);
        Inventory inventory = create(holder, 5, "edit.title", "&8Spawn: {name}", "name", spawnName);
        fill(inventory);

        inventory.setItem(10, named(
                Material.NAME_TAG,
                "edit.rename.name", "&eRenomear",
                lore("edit.rename.lore",
                        "&7Nome atual: &f" + spawnName,
                        "",
                        "&eClique e digite o novo nome.")));
        inventory.setItem(12, iconSlot(point));
        inventory.setItem(14, named(
                Material.ENDER_PEARL,
                "edit.position.name", "&bAtualizar posição",
                lore("edit.position.lore",
                        "&7Salva este spawn onde você está.",
                        coords(point),
                        "",
                        "&eClique para atualizar.")));
        inventory.setItem(16, toggleItem(
                Material.NETHER_STAR,
                "edit.default.name", "&6Spawn padrão",
                spawn.isDefault(spawnName),
                "edit.default.lore-on", "edit.default.lore-off"));
        inventory.setItem(19, toggleItem(
                Material.WATCH,
                "edit.logout.name", "&dRetorno no login",
                point.logoutReturn(),
                "edit.logout.lore-on", "edit.logout.lore-off"));
        inventory.setItem(21, toggleItem(
                Material.BED,
                "edit.world-respawn.name", "&cRespawn do mundo",
                point.worldRespawn(),
                "edit.world-respawn.lore-on", "edit.world-respawn.lore-off"));
        inventory.setItem(25, named(
                Material.TNT,
                "edit.delete.name", "&cRemover spawn",
                lore("edit.delete.lore",
                        "&7Apaga &f" + spawnName + "&7 para sempre.",
                        "",
                        "&cClique para confirmar.")));
        putNav(inventory, false);
        player.openInventory(inventory);
    }

    public void openDelete(Player player, String spawnName) {
        MenuHolder holder = new MenuHolder(MenuHolder.View.DELETE, spawnName);
        Inventory inventory = create(holder, 3, "delete.title", "&8Remover?", "name", spawnName);
        fill(inventory);
        inventory.setItem(11, named(
                Material.EMERALD_BLOCK,
                "delete.confirm.name", "&aConfirmar",
                lore("delete.confirm.lore",
                        "&7Remove o spawn &f" + spawnName + "&7.",
                        "",
                        "&cIsso não tem volta.")));
        inventory.setItem(15, named(
                Material.REDSTONE_BLOCK,
                "delete.cancel.name", "&cCancelar",
                lore("delete.cancel.lore",
                        "&7Volta sem apagar nada.")));
        putNav(inventory, false);
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event, Player player, MenuHolder holder) {
        int size = event.getView().getTopInventory().getSize();
        int slot = event.getRawSlot();
        if (slot >= size) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (isPane(clicked) || clicked == null || clicked.getType() == Material.AIR) {
            if (holder.view() == MenuHolder.View.EDIT && slot == 12) {
                handleIcon(event, player, holder.spawnName());
            }
            return;
        }

        if (slot == size - 1) {
            player.closeInventory();
            return;
        }
        if (slot == size - 9) {
            goBack(player, holder);
            return;
        }

        switch (holder.view()) {
            case HOME -> clickHome(player, slot);
            case SPAWNS -> clickSpawns(event, player, slot);
            case EDIT -> clickEdit(event, player, holder.spawnName(), slot);
            case DELETE -> clickDelete(player, holder.spawnName(), slot);
        }
    }

    private void clickHome(Player player, int slot) {
        if (slot == 13) {
            reopen(() -> openSpawns(player));
        }
    }

    private void clickSpawns(InventoryClickEvent event, Player player, int slot) {
        if (slot == 40) {
            player.closeInventory();
            plugin.chatPrompt().askCreate(player);
            return;
        }
        String name = spawnAt(slot);
        if (name == null) {
            return;
        }
        if (event.isRightClick()) {
            reopen(() -> openSpawnEdit(player, name));
            return;
        }
        if (event.isLeftClick()) {
            player.closeInventory();
            if (plugin.spawn().teleport(player, name)) {
                player.sendMessage(plugin.messages().get("command.spawn.teleported", "name", name));
                click(player);
            } else {
                player.sendMessage(plugin.messages().get("command.spawn.world-missing",
                        "world", plugin.spawn().worldName(name)));
            }
        }
    }

    private void clickEdit(InventoryClickEvent event, Player player, String spawnName, int slot) {
        SpawnService spawn = plugin.spawn();
        if (slot == 10) {
            player.closeInventory();
            plugin.chatPrompt().askRename(player, spawnName);
            return;
        }
        if (slot == 12) {
            handleIcon(event, player, spawnName);
            return;
        }
        if (slot == 14) {
            if (spawn.updateLocation(spawnName, player)) {
                player.sendMessage(plugin.messages().get("menu.position.updated", "name", spawnName));
                click(player);
                reopen(() -> openSpawnEdit(player, spawnName));
            }
            return;
        }
        if (slot == 16) {
            spawn.setDefault(spawnName);
            player.sendMessage(plugin.messages().get("menu.default.set", "name", spawnName));
            click(player);
            reopen(() -> openSpawnEdit(player, spawnName));
            return;
        }
        if (slot == 19) {
            SpawnPoint point = spawn.point(spawnName);
            boolean next = point == null || !point.logoutReturn();
            spawn.setLogoutReturn(spawnName, next);
            click(player);
            reopen(() -> openSpawnEdit(player, spawnName));
            return;
        }
        if (slot == 21) {
            SpawnPoint point = spawn.point(spawnName);
            boolean next = point == null || !point.worldRespawn();
            spawn.setWorldRespawn(spawnName, next);
            click(player);
            reopen(() -> openSpawnEdit(player, spawnName));
            return;
        }
        if (slot == 25) {
            reopen(() -> openDelete(player, spawnName));
        }
    }

    private void clickDelete(Player player, String spawnName, int slot) {
        if (slot == 15) {
            reopen(() -> openSpawnEdit(player, spawnName));
            return;
        }
        if (slot == 11) {
            plugin.spawn().remove(spawnName);
            player.sendMessage(plugin.messages().get("menu.delete.done", "name", spawnName));
            click(player);
            reopen(() -> openSpawns(player));
        }
    }

    private void handleIcon(InventoryClickEvent event, Player player, String spawnName) {
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) {
            player.sendMessage(plugin.messages().get("menu.icon.need-item"));
            return;
        }
        if (plugin.spawn().setIcon(spawnName, cursor)) {
            player.sendMessage(plugin.messages().get("menu.icon.set", "item", cursor.getType().name()));
            click(player);
            reopen(() -> openSpawnEdit(player, spawnName));
        }
    }

    private void goBack(Player player, MenuHolder holder) {
        switch (holder.view()) {
            case HOME -> player.closeInventory();
            case SPAWNS -> reopen(() -> openHome(player));
            case EDIT -> reopen(() -> openSpawns(player));
            case DELETE -> reopen(() -> openSpawnEdit(player, holder.spawnName()));
        }
    }

    private String spawnAt(int slot) {
        int index = -1;
        for (int i = 0; i < SPAWN_SLOTS.length; i++) {
            if (SPAWN_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return null;
        }
        List<String> names = plugin.spawn().nameList();
        return index < names.size() ? names.get(index) : null;
    }

    private ItemStack spawnIcon(String name) {
        SpawnService spawn = plugin.spawn();
        SpawnPoint point = spawn.point(name);
        if (point == null) {
            return named(Material.BARRIER, "spawns.missing.name", "&c" + name, List.of());
        }
        ItemStack item = new ItemStack(point.icon(), 1, point.iconData());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        boolean isDefault = spawn.isDefault(name);
        meta.setDisplayName(Colors.color(isDefault ? "&6" + name + " &e✦" : "&6" + name));
        List<String> lore = new ArrayList<>();
        lore.add(Colors.color("&7Mundo: &f" + point.world()));
        lore.add(Colors.color(coords(point)));
        lore.add(Colors.color("&7Yaw / Pitch: &f" + format(point.yaw()) + " / " + format(point.pitch())));
        lore.add(Colors.color("&7Spawn padrão: " + yn(isDefault)));
        lore.add(Colors.color("&7Retorno no login: " + yn(point.logoutReturn())));
        lore.add(Colors.color("&7Respawn do mundo: " + yn(point.worldRespawn())));
        lore.add("");
        lore.add(Colors.color("&eEsquerdo: teleportar"));
        lore.add(Colors.color("&eDireito: configurar"));
        meta.setLore(lore);
        if (isDefault) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
        }
        hide(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack iconSlot(SpawnPoint point) {
        ItemStack item = new ItemStack(point.icon(), 1, point.iconData());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Colors.color(text("edit.icon.name", "&bÍcone")));
        List<String> lore = lore("edit.icon.lore",
                "&7Item atual: &f" + point.icon().name(),
                "",
                "&ePegue um bloco no criativo",
                "&ee clique aqui para trocar.");
        meta.setLore(lore);
        hide(meta);
        item.setItemMeta(meta);
        return item;
    }

    private Inventory create(MenuHolder holder, int rows, String titlePath, String fallback, String... replacements) {
        String title = text(titlePath, fallback);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            title = title.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        title = Colors.color(title);
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inventory = plugin.getServer().createInventory(holder, rows * 9, title);
        holder.setInventory(inventory);
        return inventory;
    }

    private void fill(Inventory inventory) {
        ItemStack pane = pane();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, pane);
            }
        }
    }

    private void putNav(Inventory inventory, boolean home) {
        int size = inventory.getSize();
        inventory.setItem(size - 9, named(
                Material.ARROW,
                home ? "nav.home-back.name" : "nav.back.name",
                home ? "&7Fechar" : "&aVoltar",
                lore(home ? "nav.home-back.lore" : "nav.back.lore",
                        home ? "&7Fecha este menu." : "&7Volta ao menu anterior.")));
        inventory.setItem(size - 1, named(
                Material.BARRIER,
                "nav.close.name", "&cFechar",
                lore("nav.close.lore", "&7Fecha este menu.")));
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            hide(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isPane(ItemStack item) {
        if (item == null || item.getType() != Material.STAINED_GLASS_PANE || item.getDurability() != 15) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && " ".equals(meta.getDisplayName());
    }

    private ItemStack toggleItem(
            Material material,
            String namePath,
            String nameFallback,
            boolean enabled,
            String onPath,
            String offPath) {
        ItemStack item = named(material, namePath, nameFallback, lore(enabled ? onPath : offPath));
        if (enabled) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                hide(meta);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack named(Material material, String namePath, String nameFallback, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Colors.color(text(namePath, nameFallback)));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        hide(meta);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> lore(String path, String... fallback) {
        List<String> source = yaml.getStringList(path);
        if (source.isEmpty() && fallback != null) {
            source = List.of(fallback);
        }
        List<String> colored = new ArrayList<>(source.size());
        for (String line : source) {
            colored.add(Colors.color(line));
        }
        return colored;
    }

    private String text(String path, String fallback) {
        String value = yaml.getString(path);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void hide(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
    }

    private static String yn(boolean value) {
        return value ? "&aSim" : "&cNão";
    }

    private static String coords(SpawnPoint point) {
        return "&7XYZ: &f" + format(point.x()) + ", " + format(point.y()) + ", " + format(point.z());
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private void click(Player player) {
        player.playSound(player.getLocation(), Sound.CLICK, 0.6f, 1.4f);
    }

    private void reopen(Runnable action) {
        plugin.scheduler().later(action, 1L);
    }
}
