package com.aearost.aranarthcore.event;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class TorchflowerGrow implements Listener {

	public TorchflowerGrow(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Drops the torchflower seeds when the plant grows to be fully matured.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onTorchflowerGrow(final BlockGrowEvent e) {
		
		Location location = e.getBlock().getLocation();
		
		if (location.getBlock().getType() == Material.TORCHFLOWER_CROP) {
			// If it's a fully grown torchflower
			if (!(e.getNewState().getBlockData() instanceof Ageable)) {
				Random r = new Random();
				// Will randomly select 0, 1, or 2 seeds
				int amountOfSeeds = r.nextInt(2);
				if (amountOfSeeds > 0) {
					location.getWorld().dropItemNaturally(location, new ItemStack(Material.TORCHFLOWER_SEEDS, amountOfSeeds));
				}
				
			}
		}
	}
}
