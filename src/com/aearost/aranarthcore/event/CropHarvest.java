package com.aearost.aranarthcore.event;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class CropHarvest implements Listener {

	public CropHarvest(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Allows for the full harvest of a crop and automatic re-plant
	 * This can be done by left clicking on the crop while sneaking
	 * 
	 * @param e
	 */
	@EventHandler
	public void onLogStrip(final BlockBreakEvent e) {
		if (e.getPlayer().isSneaking()) {
			Block block = e.getBlock();
			if (getIsBlockCrop(block)) {
				if (getIsMature(block)) {
					e.setCancelled(true);
					// Prevents the block from actually being broken
					ArrayList<ItemStack> drops = new ArrayList<>(block.getDrops());
					if (drops.size() > 1) {
						// The first index (0) is always 1 of the crop (wheat, beetroot, carrot, potato)
						// The second index (1) is always the seed (wheat seeds, beetroot seeds, carrot, potato)
						final ItemStack seed = drops.get(1);
						seed.setAmount(seed.getAmount() - 1);
					}
					// Only applies for nether wart
					else {
						final ItemStack seed = drops.get(0);
						seed.setAmount(seed.getAmount() - 1);
					}
					for (ItemStack drop : drops) {
						if (drop != null && drop.getAmount() > 0) {
							block.getWorld().dropItemNaturally(block.getLocation(), drop);
						}
					}
					block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 1.3F, 2.0F);
					
					Ageable crop = (Ageable) block.getBlockData();
					crop.setAge(0);
					// mcMMO Herbalism XP gain is lost because of this
					// Without this call, there's no way for the plant to be re-planted
					block.setBlockData(crop);
				}
			}
		}
	}

	private boolean getIsBlockCrop(Block block) {
		if (block.getType() == Material.WHEAT || block.getType() == Material.CARROTS
				|| block.getType() == Material.POTATOES || block.getType() == Material.BEETROOTS
				|| block.getType() == Material.NETHER_WART) {
			return true;
		}
		return false;
	}

	private boolean getIsMature(Block block) {
		if (block.getBlockData() instanceof Ageable) {
			Ageable crop = (Ageable) block.getBlockData();
			if (crop.getMaximumAge() == crop.getAge()) {
				return true;
			}
		}
		return false;
	}
}
