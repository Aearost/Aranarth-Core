package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiLoginStreak;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.LoginStreakUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

/**
 * Handles clicks within the Login Streak GUI.
 * Cancels all clicks and claims the streak reward when the active day item is clicked.
 */
public class GuiLoginStreakClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        int slot = e.getRawSlot();
        UUID uuid = player.getUniqueId();

        // Determine which day was clicked, if any
        int clickedDay = -1;
        for (int i = 0; i < GuiLoginStreak.DAY_SLOTS.length; i++) {
            if (slot == GuiLoginStreak.DAY_SLOTS[i]) {
                clickedDay = i + 1;
                break;
            }
        }
        if (clickedDay == -1) return;

        int currentDay = LoginStreakUtils.getStreakDay(uuid);
        if (clickedDay != currentDay) return;
        if (!LoginStreakUtils.canClaim(uuid)) return;

        boolean success = LoginStreakUtils.claimStreak(player);
        if (!success) return;

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Refresh day items and info item to reflect the new state
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        int rank = aranarthPlayer.getRank();
        int newDay = LoginStreakUtils.getStreakDay(uuid);
        boolean canClaimNow = LoginStreakUtils.canClaim(uuid); // always false right after claiming

        for (int i = 0; i < GuiLoginStreak.DAY_SLOTS.length; i++) {
            e.getInventory().setItem(
                    GuiLoginStreak.DAY_SLOTS[i],
                    GuiLoginStreak.makeDayItem(i + 1, newDay, canClaimNow, rank)
            );
        }
    }
}
