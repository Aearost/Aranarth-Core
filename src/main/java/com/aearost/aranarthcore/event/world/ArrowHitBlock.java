package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

import static com.aearost.aranarthcore.objects.CustomKeys.ARROW;

/**
 * Overrides behaviour of custom arrows hitting blocks.
 */
public class ArrowHitBlock {
	public void execute(final ProjectileHitEvent e) {
		Block block = e.getHitBlock();
		if (e.getEntity() instanceof Arrow arrow) {
			if (arrow.getPersistentDataContainer().has(ARROW)) {
				arrow.setItemStack(AranarthUtils.getArrowFromType(arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING)));
				if (arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING).equals("obsidian")) {
					// 60% chance of breaking
					if (new Random().nextInt(10) >= 4) {
						e.getEntity().remove();
					}
				}
			}

			if (arrow.getShooter() instanceof Player shooter) {
				if (block.getType().name().endsWith("GLASS") || block.getType().name().endsWith("GLASS_PANE")
						|| block.getType() == Material.ICE) {
					Dominion shooterDominion = DominionUtils.getPlayerDominion(shooter.getUniqueId());
					Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
					if (chunkDominion == null || (shooterDominion == null && chunkDominion == null)
							|| shooterDominion.getLeader().equals(chunkDominion.getLeader())) {
						boolean isIce = block.getType() == Material.ICE;

						new BukkitRunnable() {
							@Override
							public void run() {
								block.breakNaturally();
								block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
								// Will replace the ice with water without this
								if (isIce) {
									block.setType(Material.AIR);
								}
							}
						}.runTaskLater(AranarthCore.getInstance(), 1L);
					}
				}
			}

		}
	}
}
