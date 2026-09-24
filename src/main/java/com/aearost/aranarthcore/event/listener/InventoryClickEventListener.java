package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.BannerExtendPatternLimit;
import com.aearost.aranarthcore.event.mob.GuiVillagerClick;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.gui.*;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.TradeManager;
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
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getView().getType() == InventoryType.CHEST) {
            if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.wrench.title"))) {
                new GuiWrenchClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.teleport.title"))) {
                new GuiHomepadClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.blacklistselect.title_clear"))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.blacklistselect.title_select"))) {
                new GuiBlacklistSelectClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.blacklisteditor.title_prefix"))
                    && e.getView().getTopInventory().getSize() == 54) {
                new GuiBlacklistEditorClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.blacklist.title"))) {
                new GuiBlacklistClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.villager.title"))) {
                new GuiVillagerClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.quiver.title"))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.quiver.title_select"))) {
                new GuiQuiverClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.potions.title_view").split("\\(")[0].trim())
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.potions.title_remove").split("\\(")[0].trim())) {
                new GuiPotionRemove().execute(e);
                new GuiPotionListPreventRemoval().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.potions.title_add").split("\\(")[0].trim())) {
                new GuiPotionPreventNonPotionAdd().execute(e);
                new GuiPotionAdd().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.shulker.title"))) {
                new GuiShulkerPreventDrop().execute(e);
                new ShulkerPreventSlotSwitch().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.ranks.title"))) {
                new GuiRanksClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.rankup.title"))) {
                new GuiRankupClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.homes.title").split("\\(")[0].trim())) {
                new GuiHomesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.delhome.title"))) {
                new GuiDelhomeClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.warps.title"))) {
                new GuiWarpClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.tables.title"))) {
                new GuiTablesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.store.title_main").split(" - ")[0] + " - ")) {
                new GuiStoreClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.compressor.title"))) {
                new GuiCompressorClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.crate.title_vote").split(" - ")[0] + " - ")) {
                new GuiCrateClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.shoplocation.title"))) {
                new GuiShopLocationClick().execute(e);
            } else if (isDominionFoodTitle(ChatUtils.stripColorFormatting(e.getView().getTitle()))) {
                new GuiDominionFoodClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(Lang.getFor(player, GuiDominionResourceFilters.TITLE_SUFFIX_KEY))) {
                new GuiDominionResourceFiltersClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(Lang.getFor(player, GuiDominionResourcesPreview.TITLE_SUFFIX_KEY))
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).contains("'s Resources")) {
                new GuiDominionResourcesPreviewClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(Lang.getFor(player, GuiDominionResourcesPreview.TITLE_SUFFIX_KEY))) {
                new GuiDominionResourcesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, GuiDominionFlagSelect.TITLE_MOB_SPAWNING_KEY))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, GuiDominionFlagSelect.TITLE_PVP_KEY))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, GuiDominionFlagSelect.TITLE_BENDING_KEY))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, GuiDominionFlagSelect.TITLE_EXPLOSIONS_KEY))) {
                new GuiDominionFlagSelectClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, GuiDominionPermissions.HUB_TITLE_KEY))
                    || (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(Lang.getFor(player, "gui.dominionperms.title_suffix"))
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(Lang.getFor(player, "gui.dominionperms.title_suffix_player")))) {
                new GuiDominionPermissionsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(Lang.getFor(player, "gui.dominionperms.title_suffix_player"))) {
                new GuiDominionPlayerPermissionsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, GuiDefenderAreaSelect.TITLE_KEY))) {
                new GuiDefenderAreaSelectClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.defendermanage.title"))) {
                new GuiDefenderManageClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.defenders.title").split("\\(")[0].trim() + " (")) {
                new GuiDefendersClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.outposts.title"))) {
                new GuiOutpostsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.dominionmembers.title"))) {
                new GuiDominionMembersClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.brewbook.title"))) {
                new GuiBrewBookClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.brewshop.title"))) {
                new GuiBrewShopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.voteshop.title"))) {
                new GuiVoteShopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.voteshoppurchase.title"))) {
                new GuiVoteShopPurchaseClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.topdeaths.title"))) {
                new GuiTopDeathsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.topkills.title"))) {
                new GuiTopKillsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.topguesses.title"))) {
                new GuiTopGuessesClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.quests.title"))) {
                new GuiQuestsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.jobs.title"))) {
                new GuiJobsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.jobsjoin.title"))) {
                new GuiJobsJoinClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.jobsstats.title"))) {
                new GuiJobsStatsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.jobsleave.title"))) {
                new GuiJobsLeaveClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(ChatUtils.stripColorFormatting(Lang.getFor(player, "gui.loginstreak.title")))) {
                new GuiLoginStreakClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.toggle.title"))) {
                new GuiToggleClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.sounds.title"))) {
                new GuiSoundsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Top Voters")) {
                new GuiVoteTopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Top ")
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.topkills.title"))
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.topdeaths.title"))
                    && !ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.topguesses.title"))) {
                new GuiMctopClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.petfood.title"))) {
                new GuiPetFoodClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, GuiHeadExchange.TITLE_KEY))) {
                new GuiHeadExchangeClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, GuiMcstats.TITLE_SELF_KEY))
                    || ChatUtils.stripColorFormatting(e.getView().getTitle()).endsWith(Lang.getFor(player, GuiMcstats.TITLE_SUFFIX_KEY))) {
                new GuiMcstatsClick().execute(e);
            } else if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals(Lang.getFor(player, "gui.reaper.title"))) {
                new GuiReaperClick().execute(e);
            } else if (TradeManager.isTradeGuiOpen(player)) {
                new GuiTradeClick().execute(e);
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
        new AfkCancelByInteract().execute(player);
//        new QuiverSwitchSlots().execute(e);
    }
}
