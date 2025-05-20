package com.aearost.aranarthcore.event.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Adds extra behaviour to the ancient crops.
 */
public class AncientPlantGrow {

	public void execute(BlockGrowEvent e) {
		Location location = e.getBlock().getLocation();
		Block block = e.getBlock();
		// Drops extra torchflower seeds and adds light above torchflowers
		if (block.getType() == Material.TORCHFLOWER_CROP) {
			// If it's a fully grown torchflower
			if (!(e.getNewState().getBlockData() instanceof Ageable)) {
				Random r = new Random();
				// Will randomly select 0, 1, 2, or 3 seeds
				int amountOfSeeds = r.nextInt(3);
				if (amountOfSeeds > 0) {
					location.getWorld().dropItemNaturally(location, new ItemStack(Material.TORCHFLOWER_SEEDS, amountOfSeeds));
				}

				Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
				if (locationAbove.getBlock().getType() == Material.AIR) {
					locationAbove.getBlock().setType(Material.LIGHT);
				}
			}
		}
		// Adds light above pitcher crops
		else if (block.getType() == Material.PITCHER_CROP) {
			Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 2, location.getZ());
			if (locationAbove.getBlock().getType() == Material.AIR) {
				locationAbove.getBlock().setType(Material.LIGHT);
			}
		}
	}
}
