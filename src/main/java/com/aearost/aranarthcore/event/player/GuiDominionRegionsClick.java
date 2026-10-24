package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.gui.GuiDominionRegions;
import com.aearost.aranarthcore.gui.GuiHolder;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handles click events for the Dominion Regions GUI screens.
 */
public class GuiDominionRegionsClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            player.closeInventory();
            return;
        }

        // Re-checked on every click, as the permission may have been revoked while the GUI was open
        if (!DominionUtils.hasPermission(player, dominion, DominionPermission.MANAGE_REGIONS)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.no_permission_regions")));
            player.closeInventory();
            return;
        }

        Inventory top = e.getView().getTopInventory();
        Object data = GuiHolder.of(top).getData();
        int slot = e.getSlot();
        switch (data) {
            case GuiDominionRegions.ListScreen(int page, List<String> regions) -> handleListClick(e, player, dominion, top, slot, page, regions);
            case GuiDominionRegions.RegionScreen(String regionName) -> handleRegionClick(e, player, dominion, top, slot, regionName);
            case GuiDominionRegions.MembersScreen(String regionName, int page, List<UUID> members) -> handleMembersClick(player, dominion, top, slot, regionName, page, members);
            case GuiDominionRegions.ColorScreen(String regionName) -> handleColorClick(player, dominion, top, slot, regionName);
            case null, default -> {
            }
        }
    }

    private void handleListClick(InventoryClickEvent e, Player player, Dominion dominion, Inventory top, int slot, int page, List<String> regions) {
        int bottomRow = top.getSize() - 9;
        if (slot == bottomRow + 4) {
            // The hub can only be used by the leader, so other region managers close the GUI instead
            if (dominion.getLeader().equals(player.getUniqueId())) {
                new GuiDominionPermissions(player).openGui();
            } else {
                player.closeInventory();
            }
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
        } else if (slot == bottomRow && page > 0) {
            GuiDominionRegions.openList(player, page - 1);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
        } else if (slot == bottomRow + 8 && page < GuiDominionRegions.getMaxPage(regions.size())) {
            GuiDominionRegions.openList(player, page + 1);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
        } else if (slot >= 9 && slot < bottomRow) {
            int index = page * GuiDominionRegions.PAGE_SIZE + slot - 9;
            if (index >= regions.size()) {
                return;
            }
            String regionName = regions.get(index);
            // The region may have been deleted or renamed since the list was opened
            if (!dominion.getRegionMembers().containsKey(regionName)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_not_found", "region", regionName)));
                GuiDominionRegions.openList(player, page);
                return;
            }
            // Placing an item on a region changes its icon
            if (setIconFromCursor(e, player, dominion, regionName)) {
                top.setItem(slot, GuiDominionRegions.buildRegionItem(player, dominion, regionName));
                return;
            }
            GuiDominionRegions.openRegion(player, regionName);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
        }
    }

    private void handleRegionClick(InventoryClickEvent e, Player player, Dominion dominion, Inventory top, int slot, String regionName) {
        if (slot == GuiDominionRegions.REGION_BACK_SLOT) {
            GuiDominionRegions.open(player);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        if (!regionExists(player, dominion, regionName)) {
            return;
        }

        switch (slot) {
            case GuiDominionRegions.INFO_SLOT -> {
                if (setIconFromCursor(e, player, dominion, regionName)) {
                    top.setItem(slot, GuiDominionRegions.buildRegionInfoItem(player, dominion, regionName));
                }
            }
            case GuiDominionRegions.ACCESS_SLOT -> {
                boolean makePublic = !dominion.isRegionPublic(regionName);
                DominionUtils.setRegionPublic(dominion, regionName, makePublic);
                DominionUtils.updateDominion(dominion);
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, makePublic ? "dominion.region_now_public" : "dominion.region_now_private", "region", regionName)));

                // The members button is only shown while the region is private
                top.setItem(GuiDominionRegions.ACCESS_SLOT, GuiDominionRegions.buildAccessItem(player, makePublic));
                top.setItem(GuiDominionRegions.INFO_SLOT, GuiDominionRegions.buildRegionInfoItem(player, dominion, regionName));
                top.setItem(GuiDominionRegions.MEMBERS_SLOT, makePublic ? null : GuiDominionRegions.buildMembersItem(player, dominion, regionName, null));
                player.updateInventory();
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, makePublic ? 1.5F : 1F);
            }
            case GuiDominionRegions.MEMBERS_SLOT -> {
                if (dominion.isRegionPublic(regionName)) {
                    return;
                }
                GuiDominionRegions.openMembers(player, regionName, 0);
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            }
            case GuiDominionRegions.COLOR_SLOT -> {
                GuiDominionRegions.openColor(player, regionName);
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            }
            case GuiDominionRegions.DELETE_SLOT -> {
                // Shift-click is required so a region is never deleted by accident
                if (!e.isShiftClick()) {
                    return;
                }
                DominionUtils.deleteRegion(dominion, regionName);
                DominionUtils.updateDominion(dominion);
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_deleted", "region", regionName)));
                player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.2F);
                GuiDominionRegions.open(player);
            }
            default -> {
            }
        }
    }

    private void handleMembersClick(Player player, Dominion dominion, Inventory top, int slot, String regionName, int page, List<UUID> members) {
        int bottomRow = top.getSize() - 9;
        if (slot == bottomRow + 4) {
            GuiDominionRegions.openRegion(player, regionName);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        if (!regionExists(player, dominion, regionName)) {
            return;
        }
        // The region may have been made public with the command since this screen was opened
        if (dominion.isRegionPublic(regionName)) {
            GuiDominionRegions.openRegion(player, regionName);
            return;
        }

        if (slot == bottomRow && page > 0) {
            GuiDominionRegions.openMembers(player, regionName, page - 1);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }
        if (slot == bottomRow + 8 && page < GuiDominionRegions.getMaxPage(members.size())) {
            GuiDominionRegions.openMembers(player, regionName, page + 1);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }
        if (slot < 9 || slot >= bottomRow) {
            return;
        }

        int index = page * GuiDominionRegions.PAGE_SIZE + slot - 9;
        if (index >= members.size()) {
            return;
        }
        UUID targetUuid = members.get(index);
        // The member may have left or been removed since the screen was opened
        if (!dominion.getMembers().contains(targetUuid)) {
            return;
        }

        Set<UUID> regionMembers = dominion.getRegionMembers().get(regionName);
        String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(targetUuid));
        boolean nowInRegion;
        if (regionMembers.contains(targetUuid)) {
            regionMembers.remove(targetUuid);
            nowInRegion = false;
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_member_removed", "player", nickname, "region", regionName)));
        } else {
            regionMembers.add(targetUuid);
            nowInRegion = true;
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_member_added", "player", nickname, "region", regionName)));
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && !targetPlayer.equals(player)) {
                targetPlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(targetPlayer, "dominion.region_you_were_added", "region", regionName, "name", dominion.getName())));
            }
        }
        DominionUtils.updateDominion(dominion);

        top.setItem(slot, GuiDominionRegions.buildMemberItem(player, targetUuid, nowInRegion));
        player.updateInventory();
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, nowInRegion ? 1.5F : 1F);
    }

    private void handleColorClick(Player player, Dominion dominion, Inventory top, int slot, String regionName) {
        if (slot == GuiDominionRegions.COLOR_BACK_SLOT) {
            GuiDominionRegions.openRegion(player, regionName);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        if (!regionExists(player, dominion, regionName)) {
            return;
        }

        if (slot == GuiDominionRegions.HEX_COLOR_SLOT) {
            // Hex map colors are restricted to Saints with gradient access
            if (!player.hasPermission("aranarth.chat.gradient")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.mapcolor_no_hex_perm")));
                return;
            }
            GuiDominionRegions.initiateHexInput(player, regionName);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        String colorCode;
        String colorName;
        if (slot == GuiDominionRegions.RESET_COLOR_SLOT) {
            colorCode = "";
            colorName = "";
        } else if (slot >= 9 && slot < 9 + GuiDominionRegions.COLOR_OPTIONS.size()) {
            GuiDominionRegions.ColorOption option = GuiDominionRegions.COLOR_OPTIONS.get(slot - 9);
            colorCode = option.code();
            colorName = ChatUtils.stripColorFormatting(GuiDominionRegions.getColorDisplayName(player, option));
        } else {
            return;
        }

        if (colorCode.equals(dominion.getRegionMapColors().getOrDefault(regionName, ""))) {
            return;
        }
        DominionUtils.setRegionMapColor(player, dominion, regionName, colorCode, colorName);
        GuiDominionRegions.updateColorOptions(player, top, dominion, regionName);
        player.updateInventory();
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
    }

    /**
     * Changes the region's icon to the item on the player's cursor, if they are holding one.
     *
     * @return True if the player was holding an item, whether or not the icon changed.
     */
    private boolean setIconFromCursor(InventoryClickEvent e, Player player, Dominion dominion, String regionName) {
        Material heldItem = e.getCursor().getType();
        if (heldItem == Material.AIR) {
            return false;
        }
        if (heldItem == dominion.getRegionIcon(regionName)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_icon_same")));
            return true;
        }
        dominion.getRegionIcons().put(regionName, heldItem);
        DominionUtils.updateDominion(dominion);
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_icon_set", "region", regionName, "icon", ChatUtils.getFormattedItemName(heldItem.name()))));
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
        return true;
    }

    /**
     * Checks that the region still exists, returning to the region list if it was deleted or renamed.
     */
    private boolean regionExists(Player player, Dominion dominion, String regionName) {
        if (dominion.getRegionMembers().containsKey(regionName)) {
            return true;
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_not_found", "region", regionName)));
        GuiDominionRegions.open(player);
        return false;
    }
}
