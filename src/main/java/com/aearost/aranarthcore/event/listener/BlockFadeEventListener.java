package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.CoralDry;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.DominionLevelUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;

/**
 * Centralizes all logic to be called by blocks fading.
 */
public class BlockFadeEventListener implements Listener {

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int SEEP_RANGE = 7;

    public BlockFadeEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent e) {
        if (e.getBlock().getType().name().contains("CORAL")) {
            new CoralDry().execute(e);
            return;
        }

        // Farmland drying out (no water nearby) reverts to dirt — decrement the cached count
        if (e.getBlock().getType() == Material.FARMLAND) {
            Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(e.getBlock().getChunk());
            if (dominion != null) {
                dominion.setCachedFarmlandCount(Math.max(0, dominion.getCachedFarmlandCount() - 1));
                DominionLevelUtils.reevaluateDominion(dominion);
            }
            return;
        }

        Material type = e.getBlock().getType();
        if (type != Material.SNOW && type != Material.ICE) {
            return;
        }

        // Only interfere during winter months
        if (!DateUtils.isWinterMonth(AranarthUtils.getMonth())) {
            return;
        }

        Block block = e.getBlock();
        double temperature = block.getTemperature();

        // Only seep blocks (warm biome) need protection — cold biome blocks are handled elsewhere
        if (temperature < 0.85) {
            return;
        }

        // Cancel the fade if this block is within the seep range of a cold biome boundary
        World world = block.getWorld();
        int bx = block.getX(), by = block.getY(), bz = block.getZ();
        for (int[] dir : DIRS) {
            for (int dist = 1; dist <= SEEP_RANGE; dist++) {
                if (world.getTemperature(bx + dir[0] * dist, by, bz + dir[1] * dist) < 0.85) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        // No cold neighbour within range — deep in a warm biome, let it fade naturally
    }
}
