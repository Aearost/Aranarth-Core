package com.aearost.aranarthcore.event.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.block.BlockPhysicsEvent;

/**
 * Handles adding and removing light blocks above Eyeblossoms when opening and closing.
 */
public class EyeblossomLight {
    public void execute(BlockPhysicsEvent e) {
        if (e.getBlock().getType() == Material.CLOSED_EYEBLOSSOM || e.getBlock().getType() == Material.OPEN_EYEBLOSSOM) {
            Block above = e.getBlock().getRelative(BlockFace.UP);
            // If it is opening
            if (e.getBlock().getType() == Material.OPEN_EYEBLOSSOM) {
                if (above.getType() == Material.AIR) {
                    above.setType(Material.LIGHT);
                    Levelled level = (Levelled) above.getBlockData();
                    level.setLevel(10);
                    above.setBlockData(level, true);
                }
            } else {
                if (above.getType() == Material.LIGHT) {
                    above.setType(Material.AIR);
                }
            }
        }
    }
}
