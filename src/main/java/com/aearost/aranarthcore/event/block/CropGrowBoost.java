package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Adjusts crop growth speed based on the current month's defined rates.
 * Crops growing in favorable months may advance extra stages per tick;
 * crops in unfavorable months have a chance of their growth being cancelled.
 */
public class CropGrowBoost {

	public void execute(BlockGrowEvent e) {
		Block block = e.getBlock();
		Material seedKey = getSeedKeyForBlock(block.getType());
		if (seedKey == null) return;

		double speed = CropUtils.getCropGrowthSpeed(AranarthUtils.getMonth(), seedKey);

		if (speed < 1.0) {
			// Dynamically cancel growth to simulate a slower growth rate
			if (ThreadLocalRandom.current().nextDouble() < 1.0 - speed) {
				e.setCancelled(true);
			}
		} else if (speed > 1.0 && block.getBlockData() instanceof Ageable crop) {
			// Cancel the event and manually advance by extra stages for a faster rate
			e.setCancelled(true);
			double extraGrowth = speed - 1.0;
			int guaranteedExtra = (int) extraGrowth;
			double fracChance = extraGrowth - guaranteedExtra;
			int extraStages = guaranteedExtra + (ThreadLocalRandom.current().nextDouble() < fracChance ? 1 : 0);
			int newAge = Math.min(crop.getAge() + 1 + extraStages, crop.getMaximumAge());
			crop.setAge(newAge);
			block.setBlockData(crop);
		}
	}

	/**
	 * Maps a crop block material to the corresponding seed key used in getCropGrowthSpeed.
	 * @param type The block material.
	 * @return The seed key material, or null if the block is not a tracked crop.
	 */
	private Material getSeedKeyForBlock(Material type) {
		return switch (type) {
			case WHEAT -> Material.WHEAT_SEEDS;
			case CARROTS -> Material.CARROT;
			case POTATOES -> Material.POTATO;
			case BEETROOTS -> Material.BEETROOT_SEEDS;
			case NETHER_WART -> Material.NETHER_WART;
			case CACTUS -> Material.CACTUS;
			case COCOA -> Material.COCOA_BEANS;
			case SUGAR_CANE -> Material.SUGAR_CANE;
			case MELON_STEM -> Material.MELON_SEEDS;
			case PUMPKIN_STEM -> Material.PUMPKIN_SEEDS;
			case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
			default -> null;
		};
	}
}
