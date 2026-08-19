package com.everest.x.parkour.session;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class SavedInventory {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final int heldSlot;

    private SavedInventory(ItemStack[] contents, ItemStack[] armor, int heldSlot) {
        this.contents = contents;
        this.armor = armor;
        this.heldSlot = heldSlot;
    }

    public static SavedInventory capture(Player player) {
        PlayerInventory inventory = player.getInventory();
        return new SavedInventory(
                clone(inventory.getContents()),
                clone(inventory.getArmorContents()),
                inventory.getHeldItemSlot());
    }

    public void restore(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setContents(clone(contents));
        inventory.setArmorContents(clone(armor));
        inventory.setHeldItemSlot(Math.max(0, Math.min(8, heldSlot)));
        player.updateInventory();
    }

    private static ItemStack[] clone(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
