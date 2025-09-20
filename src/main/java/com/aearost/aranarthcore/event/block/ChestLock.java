package com.aearost.aranarthcore.event.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles adding or removing a lock from a chest.
 */
public class ChestLock {

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (isContainer(block)) {
            // Add variable to track whether the player is trying to lock the chest
            // Store this and the player being trusted in another variable
        }
    }

    private boolean isContainer(Block block) {
        return block.getType() == Material.CHEST
                || block.getType() == Material.TRAPPED_CHEST
                || block.getType() == Material.BARREL
                || block.getType().name().endsWith("SHULKER_BOX");
    }

}
