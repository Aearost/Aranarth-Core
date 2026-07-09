package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDefenders;
import com.aearost.aranarthcore.gui.GuiDominionMembers;
import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.gui.GuiDominionPlayerPermissions;
import com.aearost.aranarthcore.gui.GuiOutposts;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Handles click events for the Dominion Permissions GUI screens.
 */
public class GuiDominionPermissionsClick {


    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        // Only the leader can use this GUI
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage("&cOnly the leader can manage permissions!"));
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        String title = ChatUtils.stripColorFormatting(e.getView().getTitle());

        // Main hub screen, navigate to sections or toggle settings
        if (title.equals(GuiDominionPermissions.HUB_TITLE)) {
            String itemName = ChatUtils.stripColorFormatting(clicked.getItemMeta().getDisplayName());
            if (itemName.startsWith("Bending")) {
                boolean newState = !dominion.isBendingEnabled();
                dominion.setBendingEnabled(newState);
                DominionUtils.updateDominion(dominion);
                e.getClickedInventory().setItem(e.getSlot(), GuiDominionPermissions.buildBendingToggleItem(newState));
                player.updateInventory();
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                return;
            }
            if (itemName.startsWith("Mob Spawning")) {
                boolean newState = !dominion.isMobSpawningEnabled();
                dominion.setMobSpawningEnabled(newState);
                DominionUtils.updateDominion(dominion);
                e.getClickedInventory().setItem(e.getSlot(), GuiDominionPermissions.buildMobSpawningToggleItem(newState));
                player.updateInventory();
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                return;
            }
            if (itemName.startsWith("Member PvP")) {
                boolean newState = !dominion.isMemberPvpEnabled();
                dominion.setMemberPvpEnabled(newState);
                DominionUtils.updateDominion(dominion);
                e.getClickedInventory().setItem(e.getSlot(), GuiDominionPermissions.buildMemberPvpToggleItem(newState));
                player.updateInventory();
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                return;
            }
            switch (itemName) {
                case "Newcomer" -> GuiDominionPermissions.openRankGui(player, DominionRank.NEWCOMER);
                case "Citizen" -> GuiDominionPermissions.openRankGui(player, DominionRank.CITIZEN);
                case "Lieutenant" -> GuiDominionPermissions.openRankGui(player, DominionRank.LIEUTENANT);
                case "Allied Dominions" -> GuiDominionPermissions.openRelationGui(player, DominionRank.ALLIED);
                case "Truced Dominions" -> GuiDominionPermissions.openRelationGui(player, DominionRank.TRUCED);
                case "Neutral Dominions" -> GuiDominionPermissions.openRelationGui(player, DominionRank.NEUTRAL);
                case "Enemied Dominions" -> GuiDominionPermissions.openRelationGui(player, DominionRank.ENEMIED);
                case "Wanderers" -> GuiDominionPermissions.openRelationGui(player, DominionRank.WANDERER);
                case "Members"     -> GuiDominionMembers.open(player);
                case "User Search" -> GuiDominionPlayerPermissions.initiateSearch(player);
                case "Defenders" -> {
                    Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
                    if (playerDominion != null) {
                        Dominion chunkDominion = DominionUtils.getDominionOfChunk(player.getLocation().getChunk());
                        Outpost chunkOutpost = OutpostUtils.getOutpostPlayerIsIn(player);
                        boolean inMain = chunkDominion != null && chunkDominion.getId().equals(playerDominion.getId());
                        boolean inOutpost = chunkOutpost != null && chunkOutpost.getDominionId().equals(playerDominion.getId());
                        if (!inMain && !inOutpost) {
                            player.sendMessage(ChatUtils.chatMessage("&cYou can only manage defenders while in your Dominion or one of its outposts"));
                            break;
                        }
                    }
                    GuiDefenders.open(player);
                }
                case "Outposts"    -> GuiOutposts.open(player);
            }
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }
        // Rank/relation permissions sub-screen (any title that isn't the main screen)
        else {
            // Back button
            if (clicked.getType() == Material.BARRIER) {
                new GuiDominionPermissions(player).openGui();
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                return;
            }

            // Restore Defaults button (slot 4)
            if (e.getSlot() == 4 && clicked.getType() == Material.ENDER_PEARL) {
                DominionRank rank = GuiDominionPermissions.getRankFromTitle(title);
                if (rank != null && rank != DominionRank.LEADER) {
                    dominion.getDominionPermissions().restoreDefaults(rank);
                    DominionUtils.updateDominion(dominion);
                    if (isRelationRank(rank)) {
                        GuiDominionPermissions.openRelationGui(player, rank);
                    } else {
                        GuiDominionPermissions.openRankGui(player, rank);
                    }
                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1F);
                }
                return;
            }

            DominionRank rank = GuiDominionPermissions.getRankFromTitle(title);
            if (rank == null) {
                return;
            }

            if (rank == DominionRank.LEADER) {
                player.sendMessage(ChatUtils.chatMessage("&cThe Leader rank always has full permissions!"));
                return;
            }

            boolean isRelation = isRelationRank(rank);
            DominionPermission perm = getPermissionFromSlot(e.getSlot(), isRelation);
            if (perm == null) {
                return;
            }

            // PvP for NEUTRAL, ENEMIED, and WANDERER is not toggleable
            if (perm == DominionPermission.PVP
                    && (rank == DominionRank.NEUTRAL || rank == DominionRank.ENEMIED || rank == DominionRank.WANDERER)) {
                return;
            }

            dominion.getDominionPermissions().togglePermission(rank, perm);
            DominionUtils.updateDominion(dominion);

            if (isRelation) {
                GuiDominionPermissions.openRelationGui(player, rank);
            } else {
                GuiDominionPermissions.openRankGui(player, rank);
            }
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
        }
    }

    /**
     * Gets the DominionPermission corresponding to a slot index using the grouped slot maps.
     */
    private DominionPermission getPermissionFromSlot(int slot, boolean isRelation) {
        Map<Integer, DominionPermission> slotMap = isRelation
                ? GuiDominionPermissions.getRelationSlotPermissions()
                : GuiDominionPermissions.getRankSlotPermissions();
        return slotMap.get(slot);
    }

    /**
     * Returns true if the rank is a relation (non-member) rank.
     */
    private boolean isRelationRank(DominionRank rank) {
        return rank == DominionRank.ALLIED || rank == DominionRank.TRUCED
                || rank == DominionRank.NEUTRAL || rank == DominionRank.WANDERER
                || rank == DominionRank.ENEMIED;
    }
}
