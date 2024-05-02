package com.aearost.aranarthcore.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.util.EventUtils;

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
	public void onCropHarvest(final BlockBreakEvent e) {
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
							// Adds support to increase yield of wheat per crop if using fortune
							if (drop.getType() == Material.WHEAT || drop.getType() == Material.BEETROOT) {
								ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
								if (heldItem.containsEnchantment(Enchantment.LOOTING)) {
									int level = heldItem.getEnchantmentLevel(Enchantment.LOOTING);
									drop.setAmount(wheatBeetrootDropCalculation(level));
								}
							}
							block.getWorld().dropItemNaturally(block.getLocation(), drop);
						}
					}
					block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 1.3F, 2.0F);
					// This allows the crop to be set to the seed level
					Ageable crop = (Ageable) block.getBlockData();
					crop.setAge(0);
					
					// mcMMO Herbalism XP gain is lost because of this
					McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(e.getPlayer());
					HerbalismManager herbalismManager = new HerbalismManager(mcmmoPlayer);
					HashSet<Block> brokenBlocks = new HashSet<Block>();
					brokenBlocks.add(block);
					herbalismManager.awardXPForPlantBlocks(brokenBlocks);
					
					// Without this call, there's no way for the crop to actually be re-planted
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
	
	private int wheatBeetrootDropCalculation(int level) {
		// This uses the same formula as regular wheat seeds dropping
		Random r = new Random();
		final int bracket = r.nextInt(10) + 1;
		int amountToDrop = 1;
		// 70% chance of getting 1 wheat
		// 30% chance of getting 2 wheat
		if (level == 1) {
			if (bracket < 8) {
				amountToDrop = 1;
			} else {
				amountToDrop = 2;
			}
		}
		// 30% chance of getting 1 wheat
		// 70% chance of getting 2 wheat
		else if (level == 2) {
			if (bracket < 4) {
				amountToDrop = 1;
			} else {
				amountToDrop = 2;
			}
		}
		// 20% chance of getting 1 wheat
		// 60% chance of getting 2 wheat
		// 20% chance of getting 3 wheat
		else if (level == 3) {
			if (bracket < 3) {
				amountToDrop = 1;
			} else if (bracket < 7) {
				amountToDrop = 2;
			} else {
				amountToDrop = 3;
			}
		}
		return amountToDrop;
	}
}
