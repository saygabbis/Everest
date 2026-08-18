package com.everest.x.core.menu;

import com.everest.x.core.EverestCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class ConfigMenuListener implements Listener {

    private final EverestCorePlugin plugin;

    public ConfigMenuListener(EverestCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ConfigMenu.ConfigMenuHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == plugin.configMenu().backSlot() || slot == plugin.configMenu().closeSlot()) {
            player.closeInventory();
            return;
        }
        if (slot == plugin.configMenu().spawnSlot()) {
            player.sendMessage(plugin.messages().get("command.config.spawn-coming-soon"));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ConfigMenu.ConfigMenuHolder) {
            event.setCancelled(true);
        }
    }
}
