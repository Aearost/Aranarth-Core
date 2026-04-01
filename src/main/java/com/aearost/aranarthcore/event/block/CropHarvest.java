package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import com.aearost.aranarthcore.utils.DateUtils;
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
			if (playerDominion == null || !playerDominion.getLeader().equals(blockDominion.getLeader())) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (!aranarthPlayer.isInAdminMode()) {
					e.setCancelled(true);
					return;
				}
			}
		}

		Block block = e.getBlock();

		// Prevent block destruction if sneaking
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
		boolean isWinterMonth = DateUtils.isWinterMonth(month) || month == Month.IGNIVOR;
		int boostMultiplier = AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST) ? 2 : 1;

		for (ItemStack drop : drops) {
			double scaled = drop.getAmount() * multiplier * boostMultiplier;
			drop.setAmount(Math.max(1, isWinterMonth ? (int) scaled : (int) Math.ceil(scaled)));
		}

		Ageable crop = (Ageable) block.getBlockData();

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
					CropUtils.updateSeedLore(drop);
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
				CropUtils.updateSeedLore(drop);
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


}
