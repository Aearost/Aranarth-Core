package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.BannerExtendPatternLimit;
import com.aearost.aranarthcore.event.mob.GuiVillagerClick;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Centralizes all logic to be called by clicking in an inventory.
 */
public class InventoryClickEventListener implements Listener {

    public InventoryClickEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getType() == InventoryType.CHEST) {
            if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Teleport")) {
                new GuiHomepadClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Blacklist")) {
                new GuiBlacklistClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Villager")) {
                new GuiVillagerClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Quiver")
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Arrow Selection")) {
                new GuiQuiverClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Potions")
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Remove Potions")
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Your Potions")) {
                new GuiPotionPreventNonPotionAdd().execute(e);
                new GuiPotionRemove().execute(e);
                new GuiPotionListPreventRemoval().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Shulker")) {
                new GuiShulkerPreventDrop().execute(e);
                new ShulkerPreventSlotSwitch().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Ranks")) {
                new GuiRanksClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Rankup Confirm")) {
                new GuiRankupClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Your Homes")) {
                new GuiHomesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Warps")) {
                new GuiWarpClick().execute(e);
            }
        } else {
            if (e.getClickedInventory() != null) {
                if (e.getView().getType() == InventoryType.ANVIL) {
                    new AranarthiumArmourCraft().execute(e);
                } else if (e.getClickedInventory().getType() == InventoryType.LOOM) {
                    new BannerExtendPatternLimit().execute(e);
                } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Fletching Table")) {
                    new FletchingTableCraft().execute(e);
                }
            }
        }

        // Execute regardless of inventory type
//        new QuiverSwitchSlots().execute(e);
    }
}
