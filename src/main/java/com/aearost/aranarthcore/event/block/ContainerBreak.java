package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles removing a container from the list of locked containers.
 */
public class ContainerBreak {

    public void execute(BlockBreakEvent e) {
        Player player = e.getPlayer();
        boolean wasLockedContainer = AranarthUtils.removeLockedContainerIfExists(e.getBlock().getLocation());
        if (wasLockedContainer) {
            if (AranarthUtils.isTrustedToContainer(player, e.getBlock().getLocation())) {
                player.sendMessage(ChatUtils.chatMessage("&7The locked container has been destroyed"));
            } else {
                if (AranarthUtils.getLockedContainers() != null) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to break this container!"));
                    e.setCancelled(true);
                }
            }
        }
    }
}
