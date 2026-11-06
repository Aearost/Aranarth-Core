package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.*;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
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


    /**
     * The rank edited by the open permission screen, or null if it is not a rank screen.
     */
    private static DominionRank getScreenRank(InventoryClickEvent e) {
        GuiHolder holder = GuiHolder.of(e.getView().getTopInventory());
        return holder != null && holder.getData() instanceof GuiDominionPermissions.RankScreen(
                DominionRank rank
        ) ? rank : null;
    }

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

        String title = ChatUtils.stripColorFormatting(e.getView().getTitle());
        boolean isHub = title.equals(Lang.getFor(player, GuiDominionPermissions.HUB_TITLE_KEY));

        // Only the leader can use this GUI, except for the Resources item which is gated by its own permission
        if (!dominion.getLeader().equals(player.getUniqueId())
                && !(isHub && e.getSlot() == GuiDominionPermissions.RESOURCES_SLOT)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.leader_only_permissions")));
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        // Main hub screen, navigate to sections or toggle settings
        if (isHub) {
            int slot = e.getSlot();
            switch (slot) {
                case GuiDominionPermissions.BENDING_SLOT -> {
                    GuiDominionFlagSelect.openBending(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case GuiDominionPermissions.MOB_SPAWNING_SLOT -> {
                    GuiDominionFlagSelect.openMobSpawning(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case GuiDominionPermissions.EXPLOSIONS_SLOT -> {
                    GuiDominionFlagSelect.openExplosions(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case GuiDominionPermissions.MEMBER_PVP_SLOT -> {
                    GuiDominionFlagSelect.openMemberPvp(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 12 -> {
                    GuiDominionPermissions.openRankGui(player, DominionRank.NEWCOMER);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 13 -> {
                    GuiDominionPermissions.openRankGui(player, DominionRank.CITIZEN);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 14 -> {
                    GuiDominionPermissions.openRankGui(player, DominionRank.LIEUTENANT);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 20 -> {
                    GuiDominionPermissions.openRelationGui(player, DominionRank.ALLIED);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 21 -> {
                    GuiDominionPermissions.openRelationGui(player, DominionRank.TRUCED);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 22 -> {
                    GuiDominionPermissions.openRelationGui(player, DominionRank.NEUTRAL);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 23 -> {
                    GuiDominionPermissions.openRelationGui(player, DominionRank.ENEMIED);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 24 -> {
                    GuiDominionPermissions.openRelationGui(player, DominionRank.WANDERER);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 9 -> {
                    GuiDominionMembers.open(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case 10 -> {
                    GuiDominionPlayerPermissions.initiateSearch(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case GuiDominionPermissions.GUARDIANS_SLOT -> {
                    GuiDefenderAreaSelect.open(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case GuiDominionPermissions.OUTPOSTS_SLOT -> {
                    GuiOutposts.open(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case GuiDominionPermissions.REGIONS_SLOT -> {
                    GuiDominionRegions.open(player);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
                }
                case GuiDominionPermissions.RESOURCES_SLOT -> {
                    String worldName = player.getWorld().getName();
                    if (!worldName.startsWith("world") && !worldName.startsWith("smp")) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.resources_gameplay_world")));
                        return;
                    }
                    // Resources are claimed only on the server hosting the dominion's land
                    if (!DominionUtils.isHostedHere(dominion)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.home_server_only", "server", DominionUtils.getHostingServerDisplayName(dominion))));
                        return;
                    }
                    if (DominionUtils.hasPermission(player, dominion, DominionPermission.RESOURCES)) {
                        if (dominion.getClaimableResources() > 0) {
                            new GuiDominionResources(player).openGui();
                            player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.resources_none")));
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.resources_no_permission")));
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
                DominionRank rank = getScreenRank(e);
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

            DominionRank rank = getScreenRank(e);
            if (rank == null) {
                return;
            }

            if (rank == DominionRank.LEADER) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.leader_full_permissions")));
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
            e.getClickedInventory().setItem(e.getSlot(), GuiDominionPermissions.buildPermissionItem(player, perm, newState));
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
        if (dominion == null) {
            return;
        }
        java.util.Set<DominionPermission> enabled = dominion.getDominionPermissions().getPermissions(rank);
        for (Map.Entry<Integer, DominionPermission> entry : GuiDominionPermissions.getRankSlotPermissions().entrySet()) {
            inv.setItem(entry.getKey(), GuiDominionPermissions.buildPermissionItem(player, entry.getValue(), enabled.contains(entry.getValue())));
        }
        player.updateInventory();
    }

    /**
     * Repopulates all relation permission slots in the open inventory without closing it.
     */
    private void repopulateRelation(org.bukkit.inventory.Inventory inv, Player player, DominionRank rank) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }
        java.util.Set<DominionPermission> enabled = dominion.getDominionPermissions().getPermissions(rank);
        boolean omitPvp = rank == DominionRank.NEUTRAL || rank == DominionRank.ENEMIED || rank == DominionRank.WANDERER;
        for (Map.Entry<Integer, DominionPermission> entry : GuiDominionPermissions.getRelationSlotPermissions().entrySet()) {
            if (omitPvp && entry.getValue() == DominionPermission.PVP) {
                continue;
            }
            inv.setItem(entry.getKey(), GuiDominionPermissions.buildPermissionItem(player, entry.getValue(), enabled.contains(entry.getValue())));
        }
        player.updateInventory();
    }
}
