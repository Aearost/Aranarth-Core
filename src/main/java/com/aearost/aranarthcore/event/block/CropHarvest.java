package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

/**
 * Allows for the full harvest of a crop and automatic re-plant.
 * This can be done by left-clicking on the crop while sneaking.
 */
public class CropHarvest {

	public void execute(BlockBreakEvent e) {
		Block block = e.getBlock();

		// Prevent block destruction if sneaking
		if (e.getPlayer().isSneaking()) {
			e.setCancelled(true);
		}

		// Only apply logic to fully grown crops
		if (!getIsMature(block)) {
			return;
		}

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
		}

		if (drops.size() > 1) {
		}

		Material cropType = drops.get(0).getType();
		ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
		int fortuneLevel = 0;

		// Calculates crop yields including fortune and winter yields
		// Other crops are all considered seeds and fortune is already applied
		if (cropType == Material.WHEAT || cropType == Material.BEETROOT) {
			if (heldItem.containsEnchantment(Enchantment.FORTUNE)) {
				fortuneLevel = heldItem.getEnchantmentLevel(Enchantment.FORTUNE);
			}
			drops.get(0).setAmount(wheatBeetrootDropCalculation(fortuneLevel, DateUtils.isWinterMonth(AranarthUtils.getMonth())));
		} else if (cropType == Material.NETHER_WART) {
			if (heldItem.containsEnchantment(Enchantment.FORTUNE)) {
				fortuneLevel = heldItem.getEnchantmentLevel(Enchantment.FORTUNE);
			}
			int amountToDrop = cropDropCalculation(fortuneLevel, DateUtils.isWinterMonth(AranarthUtils.getMonth()));
			// Nether wart reduces by seed/crop, must yield at least 1
			if (amountToDrop == 0) {
				amountToDrop++;
			}
			drops.get(0).setAmount(amountToDrop);
		} else {
			if (heldItem.containsEnchantment(Enchantment.FORTUNE)) {
				fortuneLevel = heldItem.getEnchantmentLevel(Enchantment.FORTUNE);
			}
			int amountToDrop = cropDropCalculation(fortuneLevel, DateUtils.isWinterMonth(AranarthUtils.getMonth()));
			drops.get(0).setAmount(amountToDrop);
		}

		if (drops.size() > 1) {
		}

		// Removes yields of seeds during the winter
		if (DateUtils.isWinterMonth(AranarthUtils.getMonth())) {
			if (seed.getType() == Material.WHEAT_SEEDS || seed.getType() == Material.BEETROOT_SEEDS) {
				if (seed.getAmount() > 1) {
					seed.setAmount(seed.getAmount() - 1);
				} else if (seed.getAmount() == 1) {
					int randomSeedAmount = random.nextInt(2);
					seed = drops.get(randomSeedAmount);
				}
			} else if (seed.getType() == Material.NETHER_WART) {
				if (seed.getAmount() >= 4) {
					seed.setAmount(seed.getAmount() - 2);
				} else if (seed.getAmount() == 3) {
					seed.setAmount(2);
				}
			} else if (drops.get(1).getType() == Material.CARROTS || drops.get(1).getType() == Material.POTATOES) {
				if (seed.getAmount() >= 2) {
					seed.setAmount(seed.getAmount() - 2);
				} else if (seed.getAmount() == 1) {
					seed.setAmount(0);
				}
			}
		}
		// Doubled crop and seed yields during the month of Fructivor
		else if (AranarthUtils.getMonth() == Month.FRUCTIVOR) {
			seed.setAmount(seed.getAmount() * 2);
			if (drops.size() > 1) {
				drops.get(1).setAmount(drops.get(1).getAmount() * 2);
			}
		}

		Ageable crop = (Ageable) block.getBlockData();

		// Auto-replant functionality
		if (e.getPlayer().isSneaking()) {
			e.setCancelled(true);

			// Manually reducing the seed used to replant the crop
			if (seed.getAmount() > 1) {
				seed.setAmount(seed.getAmount() - 1);
			}

			for (ItemStack drop : drops) {
				if (drop != null && drop.getAmount() > 0) {
					block.getWorld().dropItemNaturally(block.getLocation(), drop);
				}
			}
			block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 1.3F, 2.0F);

			// This allows the crop to be set to the seed fortune Level
			crop.setAge(0);

			// mcMMO Herbalism XP gain is lost because of this
			McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(e.getPlayer());
			if (mcmmoPlayer != null) {
				HerbalismManager herbalismManager = new HerbalismManager(mcmmoPlayer);
				HashSet<Block> brokenBlocks = new HashSet<>();
				brokenBlocks.add(e.getBlock());
				herbalismManager.awardXPForPlantBlocks(brokenBlocks);

				// Without this call, there's no way for the crop to actually be re-planted
				block.setBlockData(crop);
				return;
			}
		}
		// Non-sneaking crop destruction
		else {
			e.setCancelled(true);

			for (ItemStack drop : drops) {
				block.getWorld().dropItemNaturally(block.getLocation(), drop);
			}

			// mcMMO Herbalism XP gain is lost because of this
			McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(e.getPlayer());
			if (mcmmoPlayer != null) {
				HerbalismManager herbalismManager = new HerbalismManager(mcmmoPlayer);
				HashSet<Block> brokenBlocks = new HashSet<>();
				brokenBlocks.add(e.getBlock());
				herbalismManager.awardXPForPlantBlocks(brokenBlocks);

				// Without this call, there's no way for the crop to actually be re-planted
				block.setBlockData(crop);
				block.setType(Material.AIR);
				return;
			}
		}
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
	 * @return The number of the crop to be dropped, from 0 to 3.
	 */
	private int wheatBeetrootDropCalculation(int level, boolean isWinterMonth) {
		// This uses the same formula as regular wheat seeds dropping
		Random r = new Random();
		final int bracket = r.nextInt(10) + 1;
		int amountToDrop = 1;

		// 70% chance of getting 1
		// 30% chance of getting 2
		if (level == 1) {
			if (bracket >= 8) {
				amountToDrop = 2;
			}
		}
		// 30% chance of getting 1
		// 70% chance of getting 2
		else if (level == 2) {
			if (bracket >= 4) {
				amountToDrop = 2;
			}
		}
		// 20% chance of getting 1
		// 60% chance of getting 2
		// 20% chance of getting 3
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

	/**
	 * Determines how many carrots, potatoes, or nether wart to be dropped based on Fortune.
	 * @param level The fortune level of the tool.
	 * @param isWinterMonth Confirmation whether the current server month is a winter month.
	 * @return The number of the crop to be dropped, from 0 to 7.
	 */
	private int cropDropCalculation(int level, boolean isWinterMonth) {
		Random r = new Random();
		final int bracket = r.nextInt(10) + 1;
		int amountToDrop = 1;
		int baseAmount = 2;

		// 3 to 5
		if (level == 1) {
			baseAmount = 3;
		}
		// 4 to 6
		else if (level == 2) {
			baseAmount = 4;
		}
		// 5 to 7
		else if (level == 3) {
			baseAmount = 5;
		}
		amountToDrop = r.nextInt(3) + baseAmount;

		// Removes drops partially in winter months
		if (isWinterMonth) {
			int amountToReduce = r.nextInt(3);
			amountToDrop = amountToDrop - amountToReduce;
		}
		return amountToDrop;
	}
}