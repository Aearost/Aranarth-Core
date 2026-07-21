package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.BREWED_POTION;

/**
 * Tags potions in brewing stand output slots when a brew completes to avoid potion duplication.
 */
public class BrewingListener implements Listener {

    public BrewingListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBrew(BrewEvent e) {
        BrewerInventory brewer = e.getContents();
        // Tag each output slot one tick later so the brewed items are in place
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            for (int slot = 0; slot <= 2; slot++) {
                ItemStack item = brewer.getItem(slot);
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    continue;
                }
                meta.getPersistentDataContainer().set(BREWED_POTION, PersistentDataType.BYTE, (byte) 1);
                item.setItemMeta(meta);
            }
        }, 1L);
    }
}
