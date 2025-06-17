package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.GuiEnhancedAranarthiumClose;
import com.aearost.aranarthcore.event.player.GuiPotionClose;
import com.aearost.aranarthcore.event.player.GuiQuiverClose;
import com.aearost.aranarthcore.event.player.GuiShulkerClose;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Centralizes all logic to be called by closing an inventory.
 */
public class InventoryCloseEventListener implements Listener {

    public InventoryCloseEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getView().getType() == InventoryType.CHEST) {
            if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Potions")) {
                new GuiPotionClose().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Quiver")) {
                new GuiQuiverClose().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Shulker")) {
                new GuiShulkerClose().execute(e);
            }
        } else if (e.getView().getType() == InventoryType.ANVIL) {
            new GuiEnhancedAranarthiumClose().execute(e);
        }
    }
}
