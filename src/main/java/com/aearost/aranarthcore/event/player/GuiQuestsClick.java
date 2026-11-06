package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.gui.GuiQuests;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles clicks within the Aranarth Quests GUI.
 */
public class GuiQuestsClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getClickedInventory() == null) {
            return;
        }
        // Only handle clicks on the GUI itself, not the player's own inventory
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) {
            return;
        }

        int slot = e.getRawSlot();
        if (slot == GuiQuests.DAILY_BONUS_SLOT || slot == GuiQuests.WEEKLY_BONUS_SLOT) {
            QuestType type = slot == GuiQuests.DAILY_BONUS_SLOT ? QuestType.DAILY : QuestType.WEEKLY;
            if (QuestUtils.claimBoardBonus(player, type)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                GuiQuests.refresh(player, e.getView().getTopInventory());
            }
            return;
        }

        QuestType type = null;
        int index = -1;
        for (int i = 0; i < 3; i++) {
            if (slot == GuiQuests.DAILY_SLOTS[i]) {
                type = QuestType.DAILY;
                index = i;
            } else if (slot == GuiQuests.WEEKLY_SLOTS[i]) {
                type = QuestType.WEEKLY;
                index = i;
            }
        }
        if (type == null || index >= QuestUtils.getQuests(player.getUniqueId(), type).size()) {
            return;
        }

        boolean isCompleted = QuestUtils.getQuests(player.getUniqueId(), type).get(index).isCompleted();
        if (isCompleted) {
            if (QuestUtils.claimQuestReward(player, type, index)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                GuiQuests.refresh(player, e.getView().getTopInventory());
            }
            return;
        }

        if (!e.isRightClick()) {
            return;
        }
        switch (QuestUtils.reroll(player, type, index)) {
            case SUCCESS -> {
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
                GuiQuests.refresh(player, e.getView().getTopInventory());
            }
            case ALREADY_USED -> player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player,
                    type == QuestType.DAILY ? "quest.reroll_used_daily" : "quest.reroll_used_weekly")));
            case NOT_ENOUGH_MONEY -> player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.reroll_no_money",
                    "amount", "$" + QuestUtils.formatNumber(QuestUtils.getRerollCost(QuestUtils.getQuests(player.getUniqueId(), type).get(index))))));
            case NONE_AVAILABLE -> player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.reroll_none")));
            default -> {
            }
        }
    }
}
