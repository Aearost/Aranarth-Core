package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.*;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
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
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.leader_only_permissions")));
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        String title = ChatUtils.stripColorFormatting(e.getView().getTitle());

        // Main hub screen, navigate to sections or toggle settings
        if (title.equals(Lang.get(GuiDominionPermissions.HUB_TITLE_KEY))) {
            int slot = e.getSlot();
            switch (slot) {
                case 17 -> {
                    boolean newState = !dominion.isBendingEnabled();
                    dominion.setBendingEnabled(newState);
                    DominionUtils.updateDominion(dominion);
                    e.getClickedInventory().setItem(slot, GuiDominionPermissions.buildBendingToggleItem(newState));
                    player.updateInventory();
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                }
                case 30 -> {
                    boolean newState = !dominion.isMobSpawningEnabled();
                    dominion.setMobSpawningEnabled(newState);
                    DominionUtils.updateDominion(dominion);
                    e.getClickedInventory().setItem(slot, GuiDominionPermissions.buildMobSpawningToggleItem(newState));
                    player.updateInventory();
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                }
                case 32 -> {
                    boolean newState = !dominion.isExplosionEnabled();
                    dominion.setExplosionEnabled(newState);
                    DominionUtils.updateDominion(dominion);
                    e.getClickedInventory().setItem(slot, GuiDominionPermissions.buildExplosionToggleItem(newState));
                    player.updateInventory();
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                }
                case 16 -> {
                    boolean newState = !dominion.isMemberPvpEnabled();
                    dominion.setMemberPvpEnabled(newState);
                    DominionUtils.updateDominion(dominion);
                    e.getClickedInventory().setItem(slot, GuiDominionPermissions.buildMemberPvpToggleItem(newState));
                    player.updateInventory();
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
                }
                case 12 -> { GuiDominionPermissions.openRankGui(player, DominionRank.NEWCOMER); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 13 -> { GuiDominionPermissions.openRankGui(player, DominionRank.CITIZEN); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 14 -> { GuiDominionPermissions.openRankGui(player, DominionRank.LIEUTENANT); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 20 -> { GuiDominionPermissions.openRelationGui(player, DominionRank.ALLIED); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 21 -> { GuiDominionPermissions.openRelationGui(player, DominionRank.TRUCED); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 22 -> { GuiDominionPermissions.openRelationGui(player, DominionRank.NEUTRAL); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 23 -> { GuiDominionPermissions.openRelationGui(player, DominionRank.ENEMIED); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 24 -> { GuiDominionPermissions.openRelationGui(player, DominionRank.WANDERER); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 9 -> { GuiDominionMembers.open(player); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 10 -> { GuiDominionPlayerPermissions.initiateSearch(player); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 34 -> {
                    Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
                    if (playerDominion != null) {
                        Dominion chunkDominion = DominionUtils.getDominionOfChunk(player.getLocation().getChunk());
                        Outpost chunkOutpost = OutpostUtils.getOutpostPlayerIsIn(player);
                        boolean inMain = chunkDominion != null && chunkDominion.getId().equals(playerDominion.getId());
                        boolean inOutpost = chunkOutpost != null && chunkOutpost.getDominionId().equals(playerDominion.getId());
                        if (!inMain && !inOutpost) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.defenders_in_dominion")));
                            return;
                        }
                    }
                    GuiDefenders.open(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 28 -> { GuiOutposts.open(player); player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F); }
                case 31 -> {
                    String worldName = player.getWorld().getName();
                    if (!worldName.startsWith("world") && !worldName.startsWith("smp")) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_gameplay_world")));
                        return;
                    }
                    if (DominionUtils.hasPermission(player, dominion, DominionPermission.RESOURCES)) {
                        if (dominion.getClaimableResources() > 0) {
                            new GuiDominionResources(player).openGui();
                            player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_none")));
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_no_permission")));
                    }
                }
            }
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
                        repopulateRelation(e.getClickedInventory(), player, rank);
                    } else {
                        repopulateRank(e.getClickedInventory(), player, rank);
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
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.leader_full_permissions")));
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

            boolean newState = dominion.getDominionPermissions().getPermissions(rank).contains(perm);
            e.getClickedInventory().setItem(e.getSlot(), GuiDominionPermissions.buildPermissionItem(perm, newState));
            player.updateInventory();
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

    /**
     * Repopulates all rank permission slots in the open inventory without closing it.
     */
    private void repopulateRank(org.bukkit.inventory.Inventory inv, Player player, DominionRank rank) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) return;
        java.util.Set<DominionPermission> enabled = dominion.getDominionPermissions().getPermissions(rank);
        for (Map.Entry<Integer, DominionPermission> entry : GuiDominionPermissions.getRankSlotPermissions().entrySet()) {
            inv.setItem(entry.getKey(), GuiDominionPermissions.buildPermissionItem(entry.getValue(), enabled.contains(entry.getValue())));
        }
        player.updateInventory();
    }

    /**
     * Repopulates all relation permission slots in the open inventory without closing it.
     */
    private void repopulateRelation(org.bukkit.inventory.Inventory inv, Player player, DominionRank rank) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) return;
        java.util.Set<DominionPermission> enabled = dominion.getDominionPermissions().getPermissions(rank);
        boolean omitPvp = rank == DominionRank.NEUTRAL || rank == DominionRank.ENEMIED || rank == DominionRank.WANDERER;
        for (Map.Entry<Integer, DominionPermission> entry : GuiDominionPermissions.getRelationSlotPermissions().entrySet()) {
            if (omitPvp && entry.getValue() == DominionPermission.PVP) continue;
            inv.setItem(entry.getKey(), GuiDominionPermissions.buildPermissionItem(entry.getValue(), enabled.contains(entry.getValue())));
        }
        player.updateInventory();
    }
}
