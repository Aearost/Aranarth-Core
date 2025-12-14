package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles opening two doors at the same time if side by side.
 */
public class DoubleDoorOpen {

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();

        if (block.getBlockData() instanceof Door door) {
            if (AranarthUtils.isSpawnLocation(block.getLocation())) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
                if (!aranarthPlayer.getIsInAdminMode()) {
                    return;
                }
            }

            Dominion playerDominion = DominionUtils.getPlayerDominion(e.getPlayer().getUniqueId());
            Dominion blockDominion = DominionUtils.getDominionOfChunk(block.getChunk());
            if (blockDominion != null) {
                if (playerDominion == null || !playerDominion.getLeader().equals(blockDominion.getLeader())) {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
                    if (!aranarthPlayer.getIsInAdminMode()) {
                        return;
                    }
                }
            }

            Block sideBlock;
            if (door.getFacing() == BlockFace.NORTH || door.getFacing() == BlockFace.SOUTH) {
                if (door.getHinge() == Door.Hinge.LEFT) {
                    if (door.getFacing() == BlockFace.NORTH) {
                        sideBlock = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
                    } else {
                        sideBlock = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
                    }
                } else {
                    if (door.getFacing() == BlockFace.NORTH) {
                        sideBlock = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
                    } else {
                        sideBlock = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
                    }
                }
            } else {
                if (door.getHinge() == Door.Hinge.LEFT) {
                    if (door.getFacing() == BlockFace.EAST) {
                        sideBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
                    } else {
                        sideBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);
                    }
                } else {
                    if (door.getFacing() == BlockFace.EAST) {
                        sideBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);
                    } else {
                        sideBlock = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
                    }
                }
            }

            if (sideBlock.getBlockData() instanceof Door sideDoor) {
                sideDoor.setOpen(!sideDoor.isOpen());
                sideBlock.setBlockData(sideDoor, true);
            }
        }
    }

}
