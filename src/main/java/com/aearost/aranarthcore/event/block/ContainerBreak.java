package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles removing a container from the list of locked containers.
 */
public class ContainerBreak {

    public void execute(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (AranarthUtils.getLockedContainers() != null) {
            LockedContainer lockedContainer = AranarthUtils.getLockedContainerAtBlock(e.getBlock());
            if (lockedContainer == null) {
                return;
            }

            if (lockedContainer.getOwner().equals(player.getUniqueId())) {
                Location[] singleContainerLocation = new Location[] { e.getBlock().getLocation(), null };
                int breakResult = AranarthUtils.removeLockedContainer(singleContainerLocation);
                if (breakResult == 0) {
                    player.sendMessage(ChatUtils.chatMessage("&7The locked container has been destroyed"));
                } else if (breakResult == -1) {
                    player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with destroying the container..."));
                    e.setCancelled(true);
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's locked container!"));
                e.setCancelled(true);
            }
        }
    }
}
