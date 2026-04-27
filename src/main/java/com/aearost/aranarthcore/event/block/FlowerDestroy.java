package com.aearost.aranarthcore.event.block;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

		if (isFlower(e.getBlock().getType())) {
			boolean isEligible = mcMMO.getChunkManager().isEligible(e.getBlock());
			if (isEligible) {
				String worldName = e.getBlock().getLocation().getWorld().getName();
				if (worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource")) {
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

	/**
	 * Determines if the destroyed block is a flower.
	 * @param type The Material.
	 * @return Whether the destroyed block is a flower.
	 */
	private boolean isFlower(Material type) {
		return type == Material.DANDELION || type == Material.POPPY || type == Material.BLUE_ORCHID || type == Material.ALLIUM
				|| type == Material.AZURE_BLUET || type == Material.RED_TULIP || type == Material.ORANGE_TULIP
				|| type == Material.WHITE_TULIP || type == Material.PINK_TULIP || type == Material.OXEYE_DAISY
				|| type == Material.CORNFLOWER || type == Material.LILY_OF_THE_VALLEY || type == Material.TORCHFLOWER
				|| type == Material.PITCHER_PLANT || type == Material.CACTUS_FLOWER || type == Material.OPEN_EYEBLOSSOM
				|| type == Material.CLOSED_EYEBLOSSOM || type == Material.WITHER_ROSE || type == Material.PINK_PETALS
				|| type == Material.WILDFLOWERS || type == Material.SPORE_BLOSSOM || type == Material.SUNFLOWER || type == Material.LILAC
				|| type == Material.ROSE_BUSH || type == Material.PEONY || type == Material.CHORUS_FLOWER;
	}
}
