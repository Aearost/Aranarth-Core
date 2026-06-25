package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.BannerExtendPatternLimit;
import com.aearost.aranarthcore.event.mob.GuiVillagerClick;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.gui.GuiDefenders;
import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.gui.GuiOutposts;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Your Potions")
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Remove Potions")) {
                new GuiPotionRemove().execute(e);
                new GuiPotionListPreventRemoval().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Add Potions")) {
                new GuiPotionPreventNonPotionAdd().execute(e);
                new GuiPotionAdd().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Held Shulker")) {
                new GuiShulkerPreventDrop().execute(e);
                new ShulkerPreventSlotSwitch().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Ranks")) {
                new GuiRanksClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Rankup Confirm")) {
                new GuiRankupClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Your Homes")) {
                new GuiHomesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Delete Home")) {
                new GuiDelhomeClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Warps")) {
                new GuiWarpClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Tables")) {
                new GuiTablesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Aranarth Store - ")) {
                new GuiStoreClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Compressible Items")) {
                new GuiCompressorClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Crate - ")) {
                new GuiCrateClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Player Shops")) {
                new GuiShopLocationClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(" Food")) {
                new GuiDominionFoodClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(" Resources")) {
                new GuiDominionResourcesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(GuiDominionPermissions.HUB_TITLE)
                    || (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(" Permissions")
                        && !ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith("'s Permissions"))) {
                new GuiDominionPermissionsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith("'s Permissions")) {
                new GuiDominionPlayerPermissionsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(GuiDefenders.TITLE_PREFIX)) {
                new GuiDefendersClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(GuiOutposts.TITLE)) {
                new GuiOutpostsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Dominion Members")) {
                new GuiDominionMembersClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Aranarth Vote Shop")) {
                new GuiVoteShopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Vote Shop Purchase")) {
                new GuiVoteShopPurchaseClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Top Deaths")) {
                new GuiTopDeathsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Top Kills")) {
                new GuiTopKillsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Your Quests")) {
                new GuiQuestsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Login Streak")) {
                new GuiLoginStreakClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Player Toggles")) {
                new GuiToggleClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Top Voters")) {
                new GuiVoteTopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Top ")
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Top Kills")
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Top Deaths")) {
                new GuiMctopClick().execute(e);
            }
        } else {
            if (e.getClickedInventory() != null) {
                if (e.getView().getType() == InventoryType.ANVIL || e.getView().getType() == InventoryType.SMITHING) {
                    new AranarthiumArmourCraft().execute(e);
                } else if (e.getClickedInventory().getType() == InventoryType.LOOM) {
                    new BannerExtendPatternLimit().execute(e);
                } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Fletching Table")) {
                    new FletchingTableCraft().execute(e);
                } else if (e.getView().getType() == InventoryType.BREWING) {
                    new OrderChaosPotionBrewingPrevent().execute(e);
                }
            }
        }

        // Execute regardless of inventory type
        if (e.getWhoClicked() instanceof Player player) {
            new AfkCancelByInteract().execute(player);
        }
//        new QuiverSwitchSlots().execute(e);
    }
}
