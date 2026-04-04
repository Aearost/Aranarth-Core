package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Adjusts crop growth speed based on the current month's defined rates.
 */
public class CropGrowBoost {

	public void execute(BlockGrowEvent e) {
		Block block = e.getBlock();
		Material blockType = block.getType();
		Material newType = e.getNewState().getType();

		final Material seedKey;
		final boolean isNewBlockGrowth;

		if (blockType == Material.CACTUS || (blockType == Material.AIR && newType == Material.CACTUS)) {
			seedKey = Material.CACTUS;
			isNewBlockGrowth = true;
		} else if (blockType == Material.SUGAR_CANE || (blockType == Material.AIR && newType == Material.SUGAR_CANE)) {
			seedKey = Material.SUGAR_CANE;
			isNewBlockGrowth = true;
		} else if (newType == Material.MELON) {
			seedKey = Material.MELON_SEEDS;
			isNewBlockGrowth = true;
		} else if (newType == Material.PUMPKIN) {
			seedKey = Material.PUMPKIN_SEEDS;
			isNewBlockGrowth = true;
		} else {
			seedKey = CropUtils.getSeedMaterial(blockType);
			isNewBlockGrowth = false;
		}

		double speed = CropUtils.getCropGrowthSpeed(AranarthUtils.getMonth(), seedKey);

		if (speed < 1.0) {
			// Dynamically cancel growth to simulate a slower growth rate
			if (ThreadLocalRandom.current().nextDouble() < 1.0 - speed) {
				e.setCancelled(true);
			}
		} else if (speed > 1.0) {
			if (!isNewBlockGrowth && block.getBlockData() instanceof Ageable crop) {
				// Cancel and manually advance extra age stages
				e.setCancelled(true);
				double extraGrowth = speed - 1.0;
				int guaranteedExtra = (int) extraGrowth;
				double fracChance = extraGrowth - guaranteedExtra;
				int extraStages = guaranteedExtra + (ThreadLocalRandom.current().nextDouble() < fracChance ? 1 : 0);
				int newAge = Math.min(crop.getAge() + 1 + extraStages, crop.getMaximumAge());
				crop.setAge(newAge);
				block.setBlockData(crop);
			} else if (isNewBlockGrowth && (seedKey == Material.CACTUS || seedKey == Material.SUGAR_CANE)) {
				// Allow natural growth then schedule extra blocks on top
				double extraGrowth = speed - 1.0;
				int guaranteedExtra = (int) extraGrowth;
				double fracChance = extraGrowth - guaranteedExtra;
				final int extraBlocks = guaranteedExtra + (ThreadLocalRandom.current().nextDouble() < fracChance ? 1 : 0);
				if (extraBlocks > 0) {
					// After the event the new block will exist - locate it one tick later
					final Location newBlockLoc = (blockType == Material.AIR) ? block.getLocation() : block.getRelative(BlockFace.UP).getLocation();

					Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
						Block newBlock = newBlockLoc.getBlock();
						if (newBlock.getType() != seedKey) return;
						Block above = newBlock.getRelative(BlockFace.UP);
						for (int i = 0; i < extraBlocks; i++) {
							if (above.getType() != Material.AIR) break;
							if (seedKey == Material.CACTUS && !isCactusPlaceable(above)) break;
							above.setType(seedKey);
							above = above.getRelative(BlockFace.UP);
						}
					}, 1L);
				}
			}
		}
	}

	/**
	 * Determines if a cactus block can be placed at the given location.
	 */
	private boolean isCactusPlaceable(Block block) {
		return block.getRelative(BlockFace.NORTH).getType() == Material.AIR
				&& block.getRelative(BlockFace.SOUTH).getType() == Material.AIR
				&& block.getRelative(BlockFace.EAST).getType() == Material.AIR
				&& block.getRelative(BlockFace.WEST).getType() == Material.AIR;
	}
}
