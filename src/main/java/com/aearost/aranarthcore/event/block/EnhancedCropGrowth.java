package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.Random;

public class EnhancedCropGrowth implements Listener {

	public EnhancedCropGrowth(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a chance for crops to grow by 2 stages during the month of Florivor.
	 * @param e The event.
	 */
	@EventHandler
	public void onCropGrow(final BlockGrowEvent e) {
		if (AranarthUtils.getMonth() == Month.FLORIVOR) {
			// 30% chance of two stages increasing
			if ((new Random().nextInt(10) + 1) < 4) {
				Block block = e.getBlock();
				if (block.getBlockData() instanceof Ageable crop) {
					// Increase by an extra level if it can, will treat like usual otherwise
					if (crop.getMaximumAge() > crop.getAge() + 2) {
						crop.setAge(crop.getAge() + 2);
						block.setBlockData(crop);
					}
				}
			}
		}
	}

}
