package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.gui.GuiDominionPlayerPermissions;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles click events for the player-specific Dominion permission GUI.
 */
public class GuiDominionPlayerPermissionsClick {

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

        // Only the leader may use this GUI
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage("&cOnly the leader can manage permissions!"));
            return;
        }

        UUID targetUuid = GuiDominionPlayerPermissions.getTarget(player.getUniqueId());
        if (targetUuid == null) {
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        // Back button
        if (clicked.getType() == Material.BARRIER) {
            GuiDominionPlayerPermissions.clearTarget(player.getUniqueId());
            new GuiDominionPermissions(player).openGui();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        // Restore Defaults
        if (e.getSlot() == 4 && clicked.getType() == Material.ENDER_PEARL) {
            dominion.clearPlayerPermissionOverrides(targetUuid);
            DominionUtils.updateDominion(dominion);
            reopenForTarget(player, dominion, targetUuid);
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1F);
            return;
        }

        // Determine which permission was clicked
        boolean isMember = dominion.getMembers().contains(targetUuid);
        DominionRank effectiveRank;
        boolean isRelation;

        if (isMember) {
            effectiveRank = dominion.getMemberRank(targetUuid);
            if (effectiveRank == null) {
                effectiveRank = DominionRank.NEWCOMER;
            }
            isRelation = false;
        } else {
            Dominion targetDominion = DominionUtils.getPlayerDominion(targetUuid);
            effectiveRank = DominionUtils.getRelationKey(targetDominion, dominion);
            isRelation = true;
        }

        Map<Integer, DominionPermission> slotMap = isRelation
                ? GuiDominionPermissions.getRelationSlotPermissions()
                : GuiDominionPermissions.getRankSlotPermissions();

        DominionPermission perm = slotMap.get(e.getSlot());
        if (perm == null) {
            return;
        }

        // PVP not toggleable for these relations
        if (isRelation && perm == DominionPermission.PVP
                && (effectiveRank == DominionRank.NEUTRAL
                || effectiveRank == DominionRank.ENEMIED
                || effectiveRank == DominionRank.WANDERER)) {
            return;
        }

        // Determine the inherited value for this permission
        Set<DominionPermission> inheritedPerms = dominion.getDominionPermissions().getPermissions(effectiveRank);
        boolean inheritedValue = inheritedPerms.contains(perm);

        dominion.togglePlayerPermissionOverride(targetUuid, perm, inheritedValue);
        DominionUtils.updateDominion(dominion);

        reopenForTarget(player, dominion, targetUuid);
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5F, 1.5F);
    }

    /**
     * Re-opens the player permission GUI for the same target, loading their profile asynchronously.
     */
    private void reopenForTarget(Player leader, Dominion dominion, UUID targetUuid) {
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = offlineTarget.getName() != null
                ? offlineTarget.getName()
                : AranarthUtils.getNickname(offlineTarget);

        com.aearost.aranarthcore.AranarthCore plugin = com.aearost.aranarthcore.AranarthCore.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerProfile profile = Bukkit.createProfile(targetUuid, targetName);
            profile.complete(true);
            Bukkit.getScheduler().runTask(plugin,
                    () -> GuiDominionPlayerPermissions.open(leader, dominion, targetUuid, targetName, profile));
        });
    }
}
