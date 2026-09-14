package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.BannerExtendPatternLimit;
import com.aearost.aranarthcore.event.mob.GuiVillagerClick;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.gui.*;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Centralizes all logic to be called by clicking in an inventory.
 */
public class InventoryClickEventListener implements Listener {

    public InventoryClickEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static boolean isDominionFoodTitle(String title) {
        return title.startsWith(GuiDominionFood.TITLE_PREFIX);
    }

    /**
     * Prevents hoppers from extracting double brew copies before the player can pick them up.
     */
    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent e) {
        if (DoubleBrewingBonus.activeCopyLocations.isEmpty()) {
            return;
        }
        if (!(e.getSource() instanceof BrewerInventory brewer)) {
            return;
        }
        var loc = brewer.getLocation();
        if (loc != null && DoubleBrewingBonus.activeCopyLocations.contains(loc)) {
            if (e.getItem().hasItemMeta()) {
                ItemMeta meta = e.getItem().getItemMeta();
                if (meta.getPersistentDataContainer().has(CustomKeys.BREWING_COPY, PersistentDataType.BYTE)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Chat snapshot GUIs are purely view-only
        if (GuiChatSnapshot.isSnapshotGui(ChatUtils.stripColorFormatting(e.getView().getTitle()))) {
            e.setCancelled(true);
            return;
        }
        if (e.getView().getType() == InventoryType.CHEST) {
            if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.wrench.title"))) {
                new GuiWrenchClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.teleport.title"))) {
                new GuiHomepadClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.blacklistselect.title_clear"))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.blacklistselect.title_select"))) {
                new GuiBlacklistSelectClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.blacklisteditor.title_prefix"))
                    && e.getView().getTopInventory().getSize() == 54) {
                new GuiBlacklistEditorClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.blacklist.title"))) {
                new GuiBlacklistClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.villager.title"))) {
                new GuiVillagerClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.quiver.title"))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.quiver.title_select"))) {
                new GuiQuiverClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.potions.title_view").split("\\(")[0].trim())
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.potions.title_remove").split("\\(")[0].trim())) {
                new GuiPotionRemove().execute(e);
                new GuiPotionListPreventRemoval().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.potions.title_add").split("\\(")[0].trim())) {
                new GuiPotionPreventNonPotionAdd().execute(e);
                new GuiPotionAdd().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.shulker.title"))) {
                new GuiShulkerPreventDrop().execute(e);
                new ShulkerPreventSlotSwitch().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.ranks.title"))) {
                new GuiRanksClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.rankup.title"))) {
                new GuiRankupClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.homes.title").split("\\(")[0].trim())) {
                new GuiHomesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.delhome.title"))) {
                new GuiDelhomeClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.warps.title"))) {
                new GuiWarpClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.tables.title"))) {
                new GuiTablesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.store.title_main").split(" - ")[0] + " - ")) {
                new GuiStoreClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.compressor.title"))) {
                new GuiCompressorClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.crate.title_vote").split(" - ")[0] + " - ")) {
                new GuiCrateClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.shoplocation.title"))) {
                new GuiShopLocationClick().execute(e);
            } else if (isDominionFoodTitle(ChatUtils.stripColorFormatting(e.getView().getTitle()))) {
                new GuiDominionFoodClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(com.aearost.aranarthcore.utils.Lang.get(GuiDominionResourcesPreview.TITLE_SUFFIX_KEY))
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).contains("'s Resources")) {
                new GuiDominionResourcesPreviewClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(com.aearost.aranarthcore.utils.Lang.get(GuiDominionResourcesPreview.TITLE_SUFFIX_KEY))) {
                new GuiDominionResourcesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get(GuiDominionPermissions.HUB_TITLE_KEY))
                    || (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(com.aearost.aranarthcore.utils.Lang.get("gui.dominionperms.title_suffix"))
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(com.aearost.aranarthcore.utils.Lang.get("gui.dominionperms.title_suffix_player")))) {
                new GuiDominionPermissionsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(com.aearost.aranarthcore.utils.Lang.get("gui.dominionperms.title_suffix_player"))) {
                new GuiDominionPlayerPermissionsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.defendermanage.title"))) {
                new GuiDefenderManageClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.defenders.title").split("\\(")[0].trim() + " (")) {
                new GuiDefendersClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.outposts.title"))) {
                new GuiOutpostsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.dominionmembers.title"))) {
                new GuiDominionMembersClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.brewbook.title"))) {
                new GuiBrewBookClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.brewshop.title"))) {
                new GuiBrewShopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.voteshop.title"))) {
                new GuiVoteShopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.voteshoppurchase.title"))) {
                new GuiVoteShopPurchaseClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.topdeaths.title"))) {
                new GuiTopDeathsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.topkills.title"))) {
                new GuiTopKillsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.topguesses.title"))) {
                new GuiTopGuessesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.quests.title"))) {
                new GuiQuestsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.jobs.title"))) {
                new GuiJobsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.jobsjoin.title"))) {
                new GuiJobsJoinClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.jobsstats.title"))) {
                new GuiJobsStatsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.jobsleave.title"))) {
                new GuiJobsLeaveClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.ChatUtils.stripColorFormatting(com.aearost.aranarthcore.utils.Lang.get("gui.loginstreak.title")))) {
                new GuiLoginStreakClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.toggle.title"))) {
                new GuiToggleClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.sounds.title"))) {
                new GuiSoundsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Top Voters")) {
                new GuiVoteTopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Top ")
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.topkills.title"))
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.topdeaths.title"))
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.topguesses.title"))) {
                new GuiMctopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.petfood.title"))) {
                new GuiPetFoodClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get(GuiHeadExchange.TITLE_KEY))) {
                new GuiHeadExchangeClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get(GuiMcstats.TITLE_SELF_KEY))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(com.aearost.aranarthcore.utils.Lang.get(GuiMcstats.TITLE_SUFFIX_KEY))) {
                new GuiMcstatsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(com.aearost.aranarthcore.utils.Lang.get("gui.reaper.title"))) {
                new GuiReaperClick().execute(e);
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
                    new DoubleBrewingBonus().execute(e);
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
