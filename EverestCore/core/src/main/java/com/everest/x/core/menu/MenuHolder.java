package com.everest.x.core.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {

    public enum View {
        HOME,
        SPAWNS,
        EDIT,
        DELETE
    }

    private final View view;
    private final String spawnName;
    private Inventory inventory;

    public MenuHolder(View view, String spawnName) {
        this.view = view;
        this.spawnName = spawnName;
    }

    public View view() {
        return view;
    }

    public String spawnName() {
        return spawnName;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
