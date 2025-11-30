package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Handles preventing vanilla commands from being executed by players.
 */
public class VanillaCommandCancel {

    public void execute(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.getCouncilRank() != 3) {
            String[] parts = e.getMessage().split(" ");
            if (parts[0].startsWith("/w") || parts[0].startsWith("/msg")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
                e.setCancelled(true);
                return;
            }
        }
    }


}
