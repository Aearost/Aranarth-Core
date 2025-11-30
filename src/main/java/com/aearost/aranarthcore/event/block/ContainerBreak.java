package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles removing a container from the list of locked containers.
 */
public class ContainerBreak {

    public void execute(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (e.getBlock().getType() == Material.CHEST || e.getBlock().getType() == Material.TRAPPED_CHEST || e.getBlock().getType() == Material.BARREL) {
            if (AranarthUtils.getLockedContainers() != null) {
                LockedContainer lockedContainer = AranarthUtils.getLockedContainerAtBlock(e.getBlock());
                if (lockedContainer == null) {
                    return;
                }

                // If breaking your own locked container
                if (lockedContainer.getOwner().equals(player.getUniqueId())) {
                    Location[] singleContainerLocation = new Location[] { e.getBlock().getLocation(), null };
                    int breakResult = AranarthUtils.removeLockedContainer(singleContainerLocation);
                    if (breakResult == 0) {
                        player.sendMessage(ChatUtils.chatMessage("&7The locked container has been destroyed"));
                    } else if (breakResult == -1) {
                        player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with destroying the container..."));
                        e.setCancelled(true);
                    }
                }
                // Breaking someone else's locked container
                else {
                    // Getting the locations of the locked container
                    Location[] locations = lockedContainer.getLocations();
                    Location above1 = locations[0].getBlock().getRelative(BlockFace.UP).getLocation();
                    Location above2 = null;
                    if (locations[1] != null) {
                        above2 = locations[1].getBlock().getRelative(BlockFace.UP).getLocation();
                    }

                    // Only display message if there are no shops above either of the locations
                    if (ShopUtils.getShopFromLocation(above1) == null && (above2 == null || ShopUtils.getShopFromLocation(above2) == null)) {
                        player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's locked container!"));
                        e.setCancelled(true);
                    }
                }
            }
        }
    }
}
