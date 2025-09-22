package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

/**
 * Handles preventing of a chest if it is locked and the player clicking is not permitted to open it.
 */
public class ChestOpenPrevent {

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        if (isContainer(block)) {
            // The player may not be trusted when the relative is the locked chest
            if (!isPlayerTrustedToContainer(player, block)) {
                Block north = block.getRelative(BlockFace.NORTH);
                Block east = block.getRelative(BlockFace.EAST);
                Block south = block.getRelative(BlockFace.SOUTH);
                Block west = block.getRelative(BlockFace.WEST);

                if (!isPlayerTrustedToContainer(player, north)) {
                    e.setCancelled(true);
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to open this!"));
                } else if (!isPlayerTrustedToContainer(player, east)) {
                    e.setCancelled(true);
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to open this!"));
                } else if (!isPlayerTrustedToContainer(player, south)) {
                    e.setCancelled(true);
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to open this!"));
                } else if (!isPlayerTrustedToContainer(player, west)) {
                    e.setCancelled(true);
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to open this!"));
                }
            }
        }
    }

    private boolean isContainer(Block block) {
        return block.getType() == Material.CHEST
                || block.getType() == Material.TRAPPED_CHEST
                || block.getType() == Material.BARREL
                || block.getType().name().endsWith("SHULKER_BOX");
    }

    /** Verifies if the chest is locked for the player.
     * @param player The player who clicked the container block.
     * @param block The block that was clicked.
     * @return Confirmation whether the player is trusted to the container or not.
     */
    public boolean isPlayerTrustedToContainer(Player player, Block block) {
        // Skips non-container relative blocks
        if (!isContainer(block)) {
            return false;
        } else {
            List<LockedContainer> containers = AranarthUtils.getLockedContainers();
            if (containers != null) {
                for (LockedContainer container : containers) {
                    if (container.getLocation().getBlockX() == block.getX()
                            && container.getLocation().getBlockY() == block.getY()
                            && container.getLocation().getBlockZ() == block.getZ()) {
                        return true;
                    }
                }
            }
            // Leave all containers unlocked when there are no locked containers
            else {
                return true;
            }
        }
        return false;
    }

}
