package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.gui.GuiQuests;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Quest;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.QuestUtils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Handles clicks within the Aranarth Quests GUI.
 * Cancels all clicks, and claims quest rewards when completed quests are clicked.
 */
public class GuiQuestsClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        // Only handle clicks on the GUI itself, not the player's own inventory
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        int slot = e.getRawSlot();
        UUID uuid = player.getUniqueId();

        QuestType type = null;
        int index = -1;

        // Daily quest slots: 4 → index 0, 11 → index 1, 15 → index 2
        // Weekly quest slots: 29 → index 0, 33 → index 1, 40 → index 2
        if (slot == 4) { type = QuestType.DAILY; index = 0; }
        else if (slot == 11) { type = QuestType.DAILY; index = 1; }
        else if (slot == 15) { type = QuestType.DAILY; index = 2; }
        else if (slot == 29) { type = QuestType.WEEKLY; index = 0; }
        else if (slot == 33) { type = QuestType.WEEKLY; index = 1; }
        else if (slot == 40) { type = QuestType.WEEKLY; index = 2; }

        if (type == null) return;

        boolean completed = type == QuestType.DAILY
                ? QuestUtils.isDailyCompleted(uuid, index)
                : QuestUtils.isWeeklyCompleted(uuid, index);
        boolean claimed = type == QuestType.DAILY
                ? QuestUtils.isDailyClaimed(uuid, index)
                : QuestUtils.isWeeklyClaimed(uuid, index);

        if (!completed || claimed) return;

        boolean success = QuestUtils.claimQuestReward(player, type, index);
        if (!success) return;

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Update the slot to show the claimed (gray) state
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        int rank = aranarthPlayer.getRank();

        List<Quest> active = type == QuestType.DAILY
                ? QuestUtils.getActiveDailyQuests(uuid, rank)
                : QuestUtils.getActiveWeeklyQuests(uuid, rank);

        if (index < active.size()) {
            int progress = type == QuestType.DAILY
                    ? QuestUtils.getDailyProgress(uuid, index)
                    : QuestUtils.getWeeklyProgress(uuid, index);
            ItemStack updatedItem = GuiQuests.makeQuestItem(active.get(index), progress, true, true);
            e.getInventory().setItem(slot, updatedItem);
        }
    }
}
