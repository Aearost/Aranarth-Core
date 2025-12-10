package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles automatically locking a placed container.
 */
public class ContainerAutoLock {

    public void execute(BlockPlaceEvent e) {
        if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
            return;
        }

        Dominion playerDominion = DominionUtils.getPlayerDominion(e.getPlayer().getUniqueId());
        Dominion blockDominion = DominionUtils.getDominionOfChunk(e.getBlock().getChunk());
        if (blockDominion != null) {
            if (playerDominion == null || !playerDominion.getOwner().equals(blockDominion.getOwner())) {
                return;
            }
        }

        Block placed = e.getBlockPlaced();
        if (placed.getState() instanceof Chest) {
            // Two Chest objects will be used thus this must be explicitly defined
            org.bukkit.block.data.type.Chest placedChestData = (org.bukkit.block.data.type.Chest) e.getBlockPlaced().getBlockData();
            String chestTypeBeingPlaced = placedChestData.getType().name();

            int value = 0;
            // Placing the right block of a double chest
            if (!chestTypeBeingPlaced.equals("SINGLE")) {
                Block leftBlock = null;
                Block rightBlock = null;
                boolean isPlacingLeftChest = chestTypeBeingPlaced.equals("RIGHT");
                // Placing the left block of a double chest, this becomes locs[0]
                if (isPlacingLeftChest) {
                    leftBlock = placed;
                    rightBlock = getConnectedChest(placed, placedChestData);
                } else {
                    rightBlock = placed;
                    leftBlock = getConnectedChest(placed, placedChestData);
                }

                LockedContainer lockedContainer = null;
                Location[] locs = new Location[] { leftBlock.getLocation(), rightBlock.getLocation() };
                if (isPlacingLeftChest) {
                    lockedContainer = AranarthUtils.getLockedContainerAtBlock(rightBlock);
                } else {
                    lockedContainer = AranarthUtils.getLockedContainerAtBlock(leftBlock);
                }

                // Updates the locations so both will be locked
                if (lockedContainer != null) {
                    AranarthUtils.removeLockedContainer(lockedContainer.getLocations());
                    lockedContainer.setLocations(locs);
                    AranarthUtils.addLockedContainer(lockedContainer);
                }
                return;
            }
        }


        // Logic to create a locked container for single chests or for shulkers or barrels
        if (placed.getState() instanceof Chest || placed.getState() instanceof ShulkerBox || placed.getType() == Material.BARREL) {
            List<UUID> trusted = new ArrayList<>();
            trusted.add(e.getPlayer().getUniqueId());
            LockedContainer lockedContainer = new LockedContainer(e.getPlayer().getUniqueId(), trusted, new Location[] { placed.getLocation(), null });
            AranarthUtils.addLockedContainer(lockedContainer);
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&7This container has been locked"));
        }
    }

    /**
     * Provides the chest block that was initially there before the placed block was added and created a double chest.
     * @param chestBlock The block that was just placed.
     * @param chestData The data of the chest.
     * @return The block that was initially there before the placed block was added and created a double chest.
     */
    private Block getConnectedChest(Block chestBlock, org.bukkit.block.data.type.Chest chestData) {
        // Get the direction the chest is facing
        org.bukkit.block.BlockFace facing = chestData.getFacing();

        // Calculate offset based on chest type and facing direction
        org.bukkit.block.BlockFace offset = null;

        String chestTypeBeingPlaced = chestData.getType().name();
        if (chestTypeBeingPlaced.equals("LEFT")) {
            // Left chest - connected chest is to the right
            offset = getBlockFaceToLeft(facing);
        } else {
            // Right chest - connected chest is to the left
            offset = getBlockFaceToRight(facing);
        }

        Block connected = chestBlock.getRelative(offset);
        if (connected.getType() == Material.CHEST) {
            return connected;
        }

        return null;
    }

    /**
     * Provides the block relative to the left of the block.
     * @param facing The direction that the right block is currently facing.
     * @return The face of the left block.
     */
    private org.bukkit.block.BlockFace getBlockFaceToLeft(org.bukkit.block.BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.SELF;
        };
    }

    /**
     * Provides the block relative to the right of the block.
     * @param facing The direction that the left block is currently facing.
     * @return The face of the right block.
     */
    private org.bukkit.block.BlockFace getBlockFaceToRight(org.bukkit.block.BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.SELF;
        };
    }

}
