package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiQuests;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Handles a player right-clicking the Quest NPC villager.
 */
public class QuestNpcInteract {

    public void execute(PlayerInteractEntityEvent e) {
        if (!QuestUtils.isQuestNpc(e.getRightClicked())) return;
        e.setCancelled(true);
        Player player = e.getPlayer();
        new GuiQuests(player).openGui();
    }
}
