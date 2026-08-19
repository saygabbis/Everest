package com.everest.x.parkour.session;

import com.everest.x.parkour.EverestParkourPlugin;
import com.everest.x.parkour.util.Colors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ParkourKit {

    public static final int SLOT_RESTART = 3;
    public static final int SLOT_CHECKPOINT = 4;
    public static final int SLOT_LEAVE = 8;

    public enum Action {
        RESTART,
        CHECKPOINT,
        LEAVE
    }

    private ParkourKit() {
    }

    public static void apply(Player player, EverestParkourPlugin plugin) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItem(SLOT_RESTART, named(
                plugin,
                Material.GOLD_PLATE,
                "kit.restart.name",
                "&6Reiniciar",
                "kit.restart.lore",
                "&7Clique direito para recomeçar",
                "&7a pista do início."));
        inventory.setItem(SLOT_CHECKPOINT, named(
                plugin,
                Material.IRON_PLATE,
                "kit.checkpoint.name",
                "&eCheckpoint",
                "kit.checkpoint.lore",
                "&7Clique direito para voltar",
                "&7ao último checkpoint."));
        inventory.setItem(SLOT_LEAVE, named(
                plugin,
                Material.BED,
                "kit.leave.name",
                "&cSair",
                "kit.leave.lore",
                "&7Clique direito para sair",
                "&7e recuperar seu inventário."));
        inventory.setHeldItemSlot(SLOT_RESTART);
        player.updateInventory();
    }

    public static Action actionOf(ItemStack item) {
        if (item == null) {
            return null;
        }
        if (item.getType() == Material.GOLD_PLATE) {
            return Action.RESTART;
        }
        if (item.getType() == Material.IRON_PLATE) {
            return Action.CHECKPOINT;
        }
        if (item.getType() == Material.BED) {
            return Action.LEAVE;
        }
        return null;
    }

    public static boolean isKitItem(ItemStack item) {
        return actionOf(item) != null;
    }

    private static ItemStack named(
            EverestParkourPlugin plugin,
            Material material,
            String namePath,
            String nameFallback,
            String lorePath,
            String... loreFallback) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Colors.color(plugin.messages().raw(namePath, nameFallback)));
        List<String> lore = plugin.messages().rawList(lorePath);
        if (lore.isEmpty()) {
            lore = List.of(loreFallback);
        }
        List<String> colored = new ArrayList<>(lore.size());
        for (String line : lore) {
            colored.add(Colors.color(line));
        }
        meta.setLore(colored);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
