package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles preventing of a chest if it is locked and the player clicking is not permitted to open it.
 */
public class ContainerOpenPrevent {

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        if (AranarthUtils.isContainerBlock(e.getClickedBlock())) {
            if (!AranarthUtils.canOpenContainer(player, block)) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                if (!aranarthPlayer.getIsInAdminMode()) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to open this!"));
                    e.setCancelled(true);
                }
            }
        }
    }
}
