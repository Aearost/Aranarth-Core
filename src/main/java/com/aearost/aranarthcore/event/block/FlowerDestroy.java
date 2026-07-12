package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Drops extra flowers during the month of Florivor.
 */
public class FlowerDestroy {
	public void execute(BlockBreakEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		if (AranarthUtils.isFlower(e.getBlock().getType())) {
			boolean isEligible = mcMMO.getChunkManager().isEligible(e.getBlock());
			if (isEligible) {
				String worldName = e.getBlock().getLocation().getWorld().getName();
				if (worldName.startsWith("world") || AranarthUtils.isSmpWorld(worldName) || worldName.startsWith("resource")) {
					Location loc = e.getBlock().getLocation();

					// 50% chance of flowers doubling
					if (new Random().nextBoolean()) {
						e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(e.getBlock().getType()));
					}

					McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(e.getPlayer());
					if (mcmmoPlayer != null) {
						HerbalismManager herbalismManager = new HerbalismManager(mcmmoPlayer);
						herbalismManager.processHerbalismBlockBreakEvent(e);
					}
				}
			}
		}
	}
}
