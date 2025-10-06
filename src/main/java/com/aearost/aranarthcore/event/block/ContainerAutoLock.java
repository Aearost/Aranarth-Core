package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles automatically locking a placed container.
 */
public class ContainerAutoLock {

    public void execute(BlockPlaceEvent e) {
        if (e.getBlock().getState() instanceof Chest) {
            List<LockedContainer> lockedContainers = AranarthUtils.getLockedContainers();
            Player player = e.getPlayer();

            // If there is already a single chest that this is being connected to, add the second location to the locked container
            if (e.getBlockAgainst().getState() instanceof Chest chest) {
                // In case a chest is placed against trapped chest or vice versa
                if (e.getBlock().getType() == e.getBlockAgainst().getType()) {
                    LockedContainer lockedContainer = AranarthUtils.getLockedContainerAtBlock(e.getBlockAgainst());
                    List<UUID> trusted = new ArrayList<>();
                    trusted.add(player.getUniqueId());
                    // If creating a double chest and it is not yet locked
                    if (lockedContainer == null) {
                        Location[] locations = getLocationsFromBlocks(e.getBlockPlaced(), e.getBlockAgainst());
                        lockedContainer = new LockedContainer(player.getUniqueId(), trusted, locations);
                        AranarthUtils.addLockedContainer(lockedContainer);
                        player.sendMessage(ChatUtils.chatMessage("&7This container has been locked!"));
                        return;
                    } else {
                        if (lockedContainer.getOwner().equals(player.getUniqueId())) {
                            InventoryHolder holder = chest.getInventory().getHolder();
                            // Ensures it isn't already a double chest
                            if (!(holder instanceof DoubleChest)) {
                                Location[] singleContainerLocation = new Location[] { e.getBlockAgainst().getLocation(), null };
                                int breakResult = AranarthUtils.removeLockedContainer(singleContainerLocation);
                                if (breakResult == 0) {
                                    Location[] locations = getLocationsFromBlocks(e.getBlockPlaced(), e.getBlockAgainst());
                                    lockedContainer = new LockedContainer(player.getUniqueId(), trusted, locations);
                                    AranarthUtils.addLockedContainer(lockedContainer);
                                    return;
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with deleting the container..."));
                                    e.setCancelled(true);
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            UUID uuid = e.getPlayer().getUniqueId();
            Location loc = e.getBlock().getLocation();
            List<UUID> trustedPlayers = new ArrayList<>();
            trustedPlayers.add(uuid);
            // Only has the player placing the chest trusted by default
            LockedContainer container = new LockedContainer(uuid, trustedPlayers, AranarthUtils.getLocationsOfContainer(e.getBlock()));
            AranarthUtils.addLockedContainer(container);
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&7This container has been locked!"));
        }
    }

    /**
     * Provides the two locations where the first index is the left chest of the double chest being created.
     * @param placed The block that was placed.
     * @param against The block that it was placed against.
     * @return The locations of both blocks where the first index is the left chest and the second is the right.
     */
    private Location[] getLocationsFromBlocks(Block placed, Block against) {

        Bukkit.getLogger().info("PLACED - x: " + placed.getLocation().getBlockX() + " | y: " + placed.getLocation().getBlockY() + " | z: " + placed.getLocation().getBlockZ());
        Bukkit.getLogger().info("AGAINST - x: " + against.getLocation().getBlockX() + " | y: " + against.getLocation().getBlockY() + " | z: " + against.getLocation().getBlockZ());

        Location placedLoc = placed.getLocation();
        Location againstLoc = against.getLocation();

        if (against.getBlockData() instanceof org.bukkit.block.data.type.Chest againstData) {
            if (placed.getBlockData() instanceof org.bukkit.block.data.type.Chest placedData) {
                Bukkit.getLogger().info("Against face: " + againstData.getFacing().name());
                if (againstData.getFacing() == BlockFace.NORTH) {
                    // If the right chest was placed, against is the left chest
                    if (againstLoc.getBlockX() > placedLoc.getBlockX()) {
                        return new Location[] { againstLoc, placedLoc };
                    } else {
                        return new Location[] { placedLoc, againstLoc };
                    }
                } else if (againstData.getFacing() == BlockFace.EAST) {
                    // If the right chest was placed, against is the left chest
                    if (againstLoc.getBlockZ() > placedLoc.getBlockZ()) {
                        return new Location[] { againstLoc, placedLoc };
                    } else {
                        return new Location[] { placedLoc, againstLoc };
                    }
                } else if (againstData.getFacing() == BlockFace.SOUTH) {
                    // If the right chest was placed, against is the left chest
                    if (againstLoc.getBlockX() < placedLoc.getBlockX()) {
                        return new Location[] { againstLoc, placedLoc };
                    } else {
                        return new Location[] { placedLoc, againstLoc };
                    }
                } else if (againstData.getFacing() == BlockFace.WEST) {
                    // If the right chest was placed, against is the left chest
                    if (againstLoc.getBlockZ() < placedLoc.getBlockZ()) {
                        return new Location[] { againstLoc, placedLoc };
                    } else {
                        return new Location[] { placedLoc, againstLoc };
                    }
                }
            }
        }
        return null;
    }

}
