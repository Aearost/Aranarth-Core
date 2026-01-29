package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Handles preventing or adding custom logic to commands being executed by players.
 */
public class CommandOverrides {

    public void execute(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        String[] parts = e.getMessage().split(" ");

        if (aranarthPlayer.getCouncilRank() != 3) {
            if (parts[0].startsWith("/w") || parts[0].startsWith("/msg")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
                e.setCancelled(true);
                return;
            }
        }

        // Adding and removing the sub-elements upon changing element without relogging
        if (parts[0].startsWith("/b")) {
            if (parts[0].equalsIgnoreCase("/b") || parts[0].toLowerCase().startsWith("/bend")) {
                if (parts[1].equalsIgnoreCase("ch") || parts[1].equalsIgnoreCase("choose")) {
                    // Player executing this in the arena world prevents sub-elements from being removed when changing world
                    if (!player.getWorld().getName().equalsIgnoreCase("arena")) {
                        PermissionUtils.evaluatePlayerPermissions(player);
                    }
                }
            }
        }
    }


}
