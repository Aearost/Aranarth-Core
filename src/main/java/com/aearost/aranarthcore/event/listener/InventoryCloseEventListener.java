package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.gui.GuiChatSnapshot;
import com.aearost.aranarthcore.gui.GuiDominionFood;
import com.aearost.aranarthcore.gui.GuiHeadExchange;
import com.aearost.aranarthcore.gui.GuiWrench;
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
        if (GuiChatSnapshot.isSnapshotGui(ChatUtils.stripColorFormatting(e.getView().getTitle()))) {
            GuiChatSnapshot.close(e.getInventory());
            return;
        }
        if (e.getView().getType() == InventoryType.CHEST) {
            if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.wrench.title"))) {
                GuiWrench.openBlocks.remove(e.getPlayer().getUniqueId());
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.potions.title_add").split("\\(")[0].trim())) {
                new GuiPotionAddClose().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.quiver.title"))) {
                new GuiQuiverClose().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.shulker.title"))) {
                new GuiShulkerClose().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.crate.title_vote").split(" - ")[0] + " - ")) {
                new GuiCrateClose().execute(e);
            } else if (isDominionFoodTitle(ChatUtils.stripColorFormatting(e.getView().getTitle()))) {
                new GuiDominionFoodClose().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.petfood.title"))) {
                new GuiPetFoodClose().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get(GuiHeadExchange.TITLE_KEY))) {
                new GuiHeadExchangeClose().execute(e);
            }
        } else if (e.getView().getType() == InventoryType.ANVIL) {
            if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarthium Anvil")) {
                new GuiEnhancedAranarthiumClose().execute(e);
            }
        } else if (e.getView().getType() == InventoryType.WORKBENCH) {
            if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Fletching Table")) {
                new GuiFletchingTableClose().execute(e);
            }
        }
    }

    private static boolean isDominionFoodTitle(String title) {
        return title.startsWith(GuiDominionFood.TITLE_PREFIX);
    }
}
