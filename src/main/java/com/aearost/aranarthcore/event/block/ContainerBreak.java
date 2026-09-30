package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles removing a container from the list of locked containers.
 */
public class ContainerBreak {

    public void execute(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (AranarthUtils.isContainerBlock(block)) {
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
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("lock.container_destroyed")));
                    } else if (breakResult == -1) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("lock.no_permission_destroy")));
                        e.setCancelled(true);
                    }
                }
                // Breaking someone else's locked container
                else {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                    if (aranarthPlayer.isInAdminMode()) {
                        boolean hasShop = ShopUtils.getShopForContainer(e.getBlock()) != null;
                        Location[] singleContainerLocation = new Location[] { e.getBlock().getLocation(), null };
                        int breakResult = AranarthUtils.removeLockedContainer(singleContainerLocation);
                        if (breakResult == 0) {
                            // If there's a shop on this chest, ShopDestroy will handle the success message
                            if (!hasShop) {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("lock.container_destroyed")));
                            }
                        } else if (breakResult == -1) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("lock.no_permission_destroy")));
                            e.setCancelled(true);
                        }
                    } else {
                        // Getting the locations of the locked container
                        Location[] locations = lockedContainer.getLocations();

                        // Only display message if there are no shops on either container half
                        boolean hasShop = ShopUtils.getShopForContainer(locations[0].getBlock()) != null
                                || (locations[1] != null && ShopUtils.getShopForContainer(locations[1].getBlock()) != null);
                        if (!hasShop) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("lock.no_permission_destroy")));
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
