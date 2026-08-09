package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.IncantationPlentifulShear;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;

public class PlayerShearEntityEventListener implements Listener {

    public PlayerShearEntityEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent e) {
        String worldName = e.getEntity().getWorld().getName();
        if (!worldName.startsWith("world") && !AranarthUtils.isSmpWorld(worldName) && !worldName.startsWith("resource")) {
            return;
        }

        ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
        if (!heldItem.hasItemMeta()) return;
        if (!heldItem.getItemMeta().getPersistentDataContainer().has(INCANTATION_TYPE)) return;

        String type = heldItem.getItemMeta().getPersistentDataContainer().get(INCANTATION_TYPE, PersistentDataType.STRING);
        if (type.equals("incantation_plentiful")) {
            new IncantationPlentifulShear().execute(e);
        }
    }
}
