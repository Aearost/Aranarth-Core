package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionMembers;
import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Handles click events for the Dominion Members GUI.
 * Clicking a skull cycles that member's rank: NEWCOMER → CITIZEN → CLERGY → NEWCOMER.
 * The leader's skull cannot be cycled here.
 */
public class GuiDominionMembersClick {

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

        // Only the leader can change ranks
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage("&cOnly the leader can change member ranks!"));
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        // Back button navigates to the main permissions screen
        if (clicked.getType() == Material.BARRIER) {
            new GuiDominionPermissions(player).openGui();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        if (!(clicked.getItemMeta() instanceof SkullMeta skullMeta)) {
            return;
        }

        OfflinePlayer skullOwner = skullMeta.getOwningPlayer();
        if (skullOwner == null) {
            return;
        }

        UUID targetUuid = skullOwner.getUniqueId();

        // Cannot change the leader's rank through this GUI
        if (targetUuid.equals(dominion.getLeader())) {
            player.sendMessage(ChatUtils.chatMessage("&cUse /dominion setleader to transfer leadership!"));
            return;
        }

        // Cannot change your own rank (leader changing themselves doesn't make sense)
        if (targetUuid.equals(player.getUniqueId())) {
            return;
        }

        DominionRank currentRank = dominion.getMemberRank(targetUuid);
        if (currentRank == null || currentRank == DominionRank.LEADER) {
            return;
        }

        DominionRank nextRank = cycleRank(currentRank);
        dominion.setMemberRank(targetUuid, nextRank);
        DominionUtils.updateDominion(dominion);
        String rankName = DominionUtils.getFormattedRankName(nextRank);

        String nickname = AranarthUtils.getNickname(skullOwner);
        player.sendMessage(ChatUtils.chatMessage("&e" + nickname + "&7's rank has been set to " + rankName));
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1F);

        // Notify the member if they're online
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatUtils.chatMessage("&7Your rank in &e" + dominion.getName() + "&7 has been changed to " + rankName));
        }

        // Refresh the GUI
        new GuiDominionMembers(player).openGui();
    }

    /**
     * Cycles a rank upward, wrapping around from CLERGY back to NEWCOMER.
     * LEADER is not included in the cycle.
     */
    private DominionRank cycleRank(DominionRank rank) {
        return switch (rank) {
            case ENEMIED -> DominionRank.ENEMIED;
            case WANDERER -> DominionRank.WANDERER;
            case NEUTRAL -> DominionRank.NEUTRAL;
            case TRUCED -> DominionRank.TRUCED;
            case ALLIED -> DominionRank.ALLIED;
            case NEWCOMER -> DominionRank.CITIZEN;
            case CITIZEN -> DominionRank.CLERGY;
            case CLERGY -> DominionRank.NEWCOMER;
            case LEADER -> DominionRank.LEADER;
        };
    }
}
