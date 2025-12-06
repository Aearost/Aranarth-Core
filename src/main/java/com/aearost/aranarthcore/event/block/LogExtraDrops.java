package com.aearost.aranarthcore.event.block;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.woodcutting.WoodcuttingManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Drops extra logs during the month of Follivor.
 */
public class LogExtraDrops {
	public void execute(BlockBreakEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		if (isLogBlock(e.getBlock().getType())) {
			String worldName = e.getBlock().getLocation().getWorld().getName();
			if (worldName.startsWith("world") || worldName.startsWith("smp")) {
				Location loc = e.getBlock().getLocation();
				boolean isEligible = mcMMO.getChunkManager().isEligible(e.getBlock());
				if (isEligible) {
					// 33% chance of logs doubling
					if (new Random().nextInt(3) == 0) {
						e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(e.getBlock().getType()));
					}

					McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(e.getPlayer());
					if (mcmmoPlayer != null) {
						WoodcuttingManager woodcuttingManager = new WoodcuttingManager(mcmmoPlayer);
						woodcuttingManager.processWoodcuttingBlockXP(e.getBlock());
					}
				}
			}
		}
	}

	/**
	 * Determines if the input Material is a log block.
	 * @param type The Material.
	 * @return Confirmation if the input Material is a log block.
	 */
	private boolean isLogBlock(Material type) {
		if (type.name().endsWith("_LOG") && !type.name().endsWith("_STRIPPED_LOG")) {
			return true;
		} else if (type == Material.CRIMSON_STEM || type == Material.CRIMSON_HYPHAE
				|| type == Material.WARPED_STEM || type == Material.WARPED_HYPHAE) {
			return true;
		}
		return false;
	}
}
