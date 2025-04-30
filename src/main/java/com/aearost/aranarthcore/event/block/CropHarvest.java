package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.util.EventUtils;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class CropHarvest implements Listener {

	public CropHarvest(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Allows for the full harvest of a crop and automatic re-plant.
	 * This can be done by left-clicking on the crop while sneaking.
	 * @param e The event.
	 */
	@EventHandler
	public void onCropHarvest(final BlockBreakEvent e) {
		Block block = e.getBlock();
		if (getIsBlockCrop(block.getType())) {
			ArrayList<ItemStack> drops = new ArrayList<>(block.getDrops());
			ItemStack seed = null;
			Random random = new Random();
			if (drops.size() > 1) {
				// The first index (0) is 1 of the following crops (wheat, beetroot, carrot, potato)
				// The second index (1) is always the seed (wheat seeds, beetroot seeds, carrot, potato)
				seed = drops.get(1);
			}
			// Nether wart never has a second value
			else {
				if (drops.getFirst().getType() == Material.NETHER_WART) {
					seed = drops.getFirst();
				}

				// Prevent destruction of non-fully grown crop
				if (e.getPlayer().isSneaking()) {
					e.setCancelled(true);
				}
				// Prevents any other behaviour
				return;
			}

			ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
			int level = 0;
			boolean isUsingFortune = false;
			if (heldItem.containsEnchantment(Enchantment.FORTUNE)) {
				isUsingFortune = true;
				level = heldItem.getEnchantmentLevel(Enchantment.FORTUNE);
			}

			// Removes yields of seeds and regular non-fortune harvests during the winter
			if (DateUtils.isWinterMonth(AranarthUtils.getMonth())) {
				if (seed.getType() == Material.WHEAT_SEEDS || seed.getType() == Material.BEETROOT_SEEDS) {
					if (seed.getAmount() > 1) {
						seed.setAmount(seed.getAmount() - 1);
					} else if (seed.getAmount() == 1) {
						int randomSeedAmount = random.nextInt(2);
						seed = drops.get(randomSeedAmount);
					}

					int randomCropAmount = random.nextInt(2);

					// Not sneaking but holding fortune tool
					if (isUsingFortune && getIsMature(e.getBlock())) {
						if (!e.getPlayer().isSneaking()) {
							int amountToDrop = wheatBeetrootDropCalculation(level, DateUtils.isWinterMonth(AranarthUtils.getMonth()));
							drops.get(1).setAmount(amountToDrop);
							e.setCancelled(true);
							block.getWorld().dropItemNaturally(block.getLocation(), seed);
							block.getWorld().dropItemNaturally(block.getLocation(), drops.get(1));
							e.getBlock().setType(Material.AIR);
						}
					} else {
						drops.get(0).setAmount(randomCropAmount);
					}

				} else if (drops.get(1).getType() == Material.CARROTS || drops.get(1).getType() == Material.POTATOES) {
					if (seed.getAmount() >= 2) {
						seed.setAmount(seed.getAmount() - 2);
					} else if (seed.getAmount() == 1) {
						seed.setAmount(0);
					}
				}
			}

			// Auto-replant functionality
			if (e.getPlayer().isSneaking()) {
				e.setCancelled(true);
				if (getIsMature(block)) {
					// Prevents the block from actually being broken
					if (seed.getAmount() > 1) {
						seed.setAmount(seed.getAmount() - 1);
					}

                    for (ItemStack drop : drops) {
						if (drop != null && drop.getAmount() > 0) {
							// Adds support to increase yield per crop if using fortune
							if (drop.getType() == Material.WHEAT || drop.getType() == Material.BEETROOT) {
								drop.setAmount(wheatBeetrootDropCalculation(level, DateUtils.isWinterMonth(AranarthUtils.getMonth())));
							}
							Month month = AranarthUtils.getMonth();
							// Doubled crop yields during the month of Fructivor
							if (month == Month.FRUCTIVOR) {
								if (getIsBlockCrop(drop.getType())) {
									drop.setAmount(drop.getAmount() * 2);
								}
							}
							// Half crop rate yields during the months of Glacivor, Frigorvor, and Obscurvor
							else if (month == Month.GLACIVOR || month == Month.FRIGORVOR || month == Month.OBSCURVOR) {
								if (getIsBlockCrop(drop.getType())) {
									if (drop.getAmount() == 1) {
										if (random.nextInt(2) == 0) {
											drop.setAmount(0);
										}
									} else {
										drop.setAmount(drop.getAmount() / 2);
									}
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
					HashSet<Block> brokenBlocks = new HashSet<>();
					brokenBlocks.add(block);
					herbalismManager.awardXPForPlantBlocks(brokenBlocks);
					
					// Without this call, there's no way for the crop to actually be re-planted
					block.setBlockData(crop);
					return;
				}
			}

			e.setCancelled(true);
			block.setType(Material.AIR);
			Month month = AranarthUtils.getMonth();
			for (ItemStack drop : drops) {
				// Doubled crop yields during the month of Fructivor
				if (month == Month.FRUCTIVOR) {
					if (getIsBlockCrop(drop.getType())) {
						drop.setAmount(drop.getAmount() * 2);
					}
				}
				// Half crop rate yields during the months of Glacivor, Frigorvor, and Obscurvor
				else if (month == Month.GLACIVOR || month == Month.FRIGORVOR || month == Month.OBSCURVOR) {
					if (getIsBlockCrop(drop.getType())) {
						if (drop.getAmount() == 1) {
							if (random.nextInt(2) == 0) {
								drop.setAmount(0);
							}
						} else {
							drop.setAmount(drop.getAmount() / 2);
						}
					}
				}
				block.getWorld().dropItemNaturally(block.getLocation(), drop);
			}
		}
	}

	/**
	 * Confirms if the input item is indeed a crop.
	 * @param type The type of item it is.
	 * @return Confirmation of whether the block is a crop or not.
	 */
	private boolean getIsBlockCrop(Material type) {
        return type == Material.WHEAT || type == Material.CARROTS
                || type == Material.POTATOES || type == Material.BEETROOTS
                || type == Material.NETHER_WART;
    }

	/**
	 * Confirms if the input block is at its full maturity.
	 * @param block The block.
	 * @return Confirmation of whether the block is fully matured or not.
	 */
	private boolean getIsMature(Block block) {
		if (block.getBlockData() instanceof Ageable crop) {
            return crop.getMaximumAge() == crop.getAge();
		}
		return false;
	}

	/**
	 * Determines how much wheat or beetroot to be dropped based on Fortune.
	 * @param level The fortune level of the tool.
	 * @param isWinterMonth Confirmation whether the current server month is a winter month.
	 * @return The number of the crop to be dropped.
	 */
	private int wheatBeetrootDropCalculation(int level, boolean isWinterMonth) {
		// This uses the same formula as regular wheat seeds dropping
		Random r = new Random();
		final int bracket = r.nextInt(10) + 1;
		int amountToDrop = 1;

		// 70% chance of getting 1 wheat
		// 30% chance of getting 2 wheat
		if (level == 1) {
			if (bracket >= 8) {
				amountToDrop = 2;
			}
		}
		// 30% chance of getting 1 wheat
		// 70% chance of getting 2 wheat
		else if (level == 2) {
			if (bracket >= 4) {
				amountToDrop = 2;
			}
		}
		// 20% chance of getting 1 wheat
		// 60% chance of getting 2 wheat
		// 20% chance of getting 3 wheat
		else if (level == 3) {
			if (bracket >= 3 && bracket < 7) {
				amountToDrop = 2;
			} else if (bracket >= 7){
				amountToDrop = 3;
			}
		}

		// Removes wheat or beetroot drops partially in winter months
		if (isWinterMonth) {
			int amountToReduce = r.nextInt(2);
			amountToDrop = amountToDrop - amountToReduce;
		}
		return amountToDrop;
	}
}
