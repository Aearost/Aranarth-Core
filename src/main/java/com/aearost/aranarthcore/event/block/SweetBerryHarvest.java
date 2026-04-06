package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies the seasonal yield multiplier when a player harvests sweet berries.
 */
public class SweetBerryHarvest {

	public void execute(PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND) {
			Block block = e.getClickedBlock();
			if (block == null || block.getType() != Material.SWEET_BERRY_BUSH) {
				return;
			}

			if (!(block.getBlockData() instanceof Ageable bush)) {
				return;
			}

			// Berries are only harvestable at ages 2 and 3
			if (bush.getAge() < 2) {
				return;
			}

			if (AranarthUtils.isSpawnLocation(block.getLocation())) {
				return;
			}

			Player player = e.getPlayer();
			Dominion blockDominion = DominionUtils.getDominionOfChunk(block.getChunk());
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

			e.setCancelled(true);

			// Increased yields when age is 3
			int min = bush.getAge() == 3 ? 2 : 1;
			int max = bush.getAge() == 3 ? 3 : 2;
			int vanillaAmount = min + ThreadLocalRandom.current().nextInt(max - min + 1);

			Month month = AranarthUtils.getMonth();
			double multiplier = CropUtils.getCropYieldMultiplier(month, Material.SWEET_BERRIES);
			boolean isWinterMonth = DateUtils.isWinterMonth(month) || month == Month.IGNIVOR;
			int boostMultiplier = AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST) ? 2 : 1;

			double scaled = vanillaAmount * multiplier * boostMultiplier;
			int finalAmount = Math.max(1, isWinterMonth ? (int) scaled : (int) Math.ceil(scaled));

			ItemStack berries = new ItemStack(Material.SWEET_BERRIES, finalAmount);
			CropUtils.updateSeedLore(berries, block.getWorld());
			block.getWorld().dropItemNaturally(block.getLocation(), berries);

			// Reset bush to age 1 (vanilla post-harvest state)
			bush.setAge(1);
			block.setBlockData(bush);

			block.getWorld().playSound(block.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 1.0F);
		}
	}
}
