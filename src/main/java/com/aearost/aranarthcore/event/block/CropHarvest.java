package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Allows for the full harvest of a crop and automatic re-plant.
 * Drop quantities are scaled by the current month's yield multiplier,
 * rounding up in warm months and down in cold months.
 * This can be triggered by left-clicking on the crop while sneaking.
 */
public class CropHarvest {

	public void execute(BlockBreakEvent e) {
		if (AranarthUtils.isSpawnLocation(e.getBlock().getLocation())) {
			return;
		}

		Player player = e.getPlayer();
		Dominion blockDominion = DominionUtils.getDominionOfChunk(e.getBlock().getChunk());
		Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());

		if (blockDominion != null) {
			if (playerDominion == null || !playerDominion.isSameDominion(blockDominion)) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				boolean isEnemied = playerDominion != null && playerDominion.isEnemied(blockDominion);
				if (!aranarthPlayer.isInAdminMode() && !isEnemied) {
					e.setCancelled(true);
					return;
				}
			}
		}

		Block block = e.getBlock();

		// Non-Ageable crops have no maturity stages and no replanting behaviour
		if (!(block.getBlockData() instanceof Ageable crop)) {
			handleNonAgeableCrop(e, player, block);
			return;
		}

		// Prevent block destruction if sneaking (protects immature Ageable crops)
		if (player.isSneaking()) {
			e.setCancelled(true);
		}

		// Only apply logic to fully grown crops
		if (!CropUtils.getIsMature(block)) {
			return;
		}

		// Get vanilla fortune-based drops, then scale each by the seasonal yield multiplier
		ArrayList<ItemStack> drops = new ArrayList<>(block.getDrops(player.getInventory().getItemInMainHand()));
		Month month = AranarthUtils.getMonth();
		double multiplier = CropUtils.getCropYieldMultiplier(month, CropUtils.getSeedMaterial(block.getType()));

		// Yield should not go below 1x in the nether
		if (block.getType() == Material.NETHER_WART && block.getWorld().getName().endsWith("_nether")) {
			if (multiplier < 1) {
				multiplier = 1;
			}
		}

		int boostMultiplier = AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST) ? 2 : 1;
		double elvenMultiplier = AranarthUtils.isWearingArmorType(player, "elven") ? 1.25 : 1.0;

		for (ItemStack drop : drops) {
			double scaled = drop.getAmount() * multiplier * boostMultiplier * elvenMultiplier;
			int base = (int) scaled;
			double frac = scaled - base;
			int amount = base + (ThreadLocalRandom.current().nextDouble() < frac ? 1 : 0);
			drop.setAmount(Math.max(1, amount));
		}

        // Auto-replant functionality
		if (player.isSneaking()) {
			// Consume one seed from the drops for replanting
			Material seedMaterial = CropUtils.getSeedMaterial(block.getType());
			for (ItemStack drop : drops) {
				if (drop.getType() == seedMaterial) {
					drop.setAmount(drop.getAmount() - 1);
					break;
				}
			}

			for (ItemStack drop : drops) {
				if (drop.getAmount() > 0) {
					CropUtils.updateSeedLore(drop, block.getWorld());
					block.getWorld().dropItemNaturally(block.getLocation(), drop);
				}
			}
			block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 1.3F, 2.0F);
			crop.setAge(0);

			McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(player);
			if (mcmmoPlayer != null) {
				HerbalismManager herbalismManager = new HerbalismManager(mcmmoPlayer);
				HashSet<Block> brokenBlocks = new HashSet<>();
				brokenBlocks.add(e.getBlock());
				herbalismManager.awardXPForPlantBlocks(brokenBlocks);
				block.setBlockData(crop);
				return;
			}
		}
		// Non-sneaking crop destruction
		else {
			e.setCancelled(true);

			for (ItemStack drop : drops) {
				CropUtils.updateSeedLore(drop, block.getWorld());
				block.getWorld().dropItemNaturally(block.getLocation(), drop);
			}

			McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(player);
			if (mcmmoPlayer != null) {
				HerbalismManager herbalismManager = new HerbalismManager(mcmmoPlayer);
				HashSet<Block> brokenBlocks = new HashSet<>();
				brokenBlocks.add(e.getBlock());
				herbalismManager.awardXPForPlantBlocks(brokenBlocks);
				block.setBlockData(crop);
				block.setType(Material.AIR);
				return;
			}
		}
	}

	/**
	 * Handles scaling for non-Ageable crops (cactus, sugar cane, melon, pumpkin).
	 * These have no maturity stage and no auto-replant behaviour.
	 */
	private void handleNonAgeableCrop(BlockBreakEvent e, Player player, Block block) {
		e.setCancelled(true);

		ArrayList<ItemStack> drops = new ArrayList<>(block.getDrops(player.getInventory().getItemInMainHand()));
		Month month = AranarthUtils.getMonth();
		double multiplier = CropUtils.getCropYieldMultiplier(month, CropUtils.getSeedMaterial(block.getType()));
		int boostMultiplier = AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST) ? 2 : 1;
		double elvenMultiplier = AranarthUtils.isWearingArmorType(player, "elven") ? 1.25 : 1.0;

		for (ItemStack drop : drops) {
			double scaled = drop.getAmount() * multiplier * boostMultiplier * elvenMultiplier;
			int base = (int) scaled;
			double frac = scaled - base;
			int amount = base + (ThreadLocalRandom.current().nextDouble() < frac ? 1 : 0);
			drop.setAmount(Math.max(1, amount));
		}

		for (ItemStack drop : drops) {
			CropUtils.updateSeedLore(drop, block.getWorld());
			block.getWorld().dropItemNaturally(block.getLocation(), drop);
		}

		McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(player);
		if (mcmmoPlayer != null) {
			HerbalismManager herbalismManager = new HerbalismManager(mcmmoPlayer);
			HashSet<Block> brokenBlocks = new HashSet<>();
			brokenBlocks.add(block);
			herbalismManager.awardXPForPlantBlocks(brokenBlocks);
		}

		block.setType(Material.AIR);
	}


}
