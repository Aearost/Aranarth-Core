package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

public class CropGrowBoost implements Listener {

	public CropGrowBoost(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds increased crop growth rates during the month of Florivor.
	 * @param e The event.
	 */
	@EventHandler
	public void onCropGrow(final BlockGrowEvent e) {
		if (AranarthUtils.getMonth() == 3) {
			Block block = e.getBlock();
			if (getIsBlockCrop(block)) {
				Ageable cropWithUpdatedMaturity = getNewCropMaturity(block);
				if (cropWithUpdatedMaturity != null) {
					block.setBlockData(cropWithUpdatedMaturity);
				} else {
					Bukkit.getLogger().info("Something went wrong with a crop's growth boost...");
				}
			}
		}
	}

	/**
	 * Confirms if the input block is indeed a crop.
	 * @param block The block.
	 * @return Confirmation of whether the block is a crop or not.
	 */
	private boolean getIsBlockCrop(Block block) {
        return block.getType() == Material.WHEAT || block.getType() == Material.CARROTS
                || block.getType() == Material.POTATOES || block.getType() == Material.BEETROOTS
                || block.getType() == Material.NETHER_WART;
    }

	/**
	 * Provides the Ageable result based on the new crop maturity.
	 * @param block The block.
	 * @return Confirmation of whether the block is fully matured or not.
	 */
	private Ageable getNewCropMaturity(Block block) {
		if (block.getBlockData() instanceof Ageable crop) {
            if (crop.getMaximumAge() == crop.getAge() + 1) {
				crop.setAge(crop.getMaximumAge());
			} else {
				crop.setAge(crop.getAge() + 2);
			}
			return crop;
		}
		return null;
	}
}
