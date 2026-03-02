package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

import static com.aearost.aranarthcore.objects.CustomKeys.ARROW;

/**
 * Overrides behaviour of custom arrows hitting blocks.
 */
public class ArrowHit {
	public void execute(final ProjectileHitEvent e) {
		Arrow arrow = (Arrow) e.getEntity();
		Block block = e.getHitBlock();

		if (arrow.getPersistentDataContainer().has(ARROW)) {
			arrow.setItemStack(AranarthUtils.getArrowFromType(arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING)));
			String type = arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
			if (type.equals("obsidian") || type.equals("dragon")) {
				boolean isSpawningBreath = false;
				if (type.equals("dragon")) {
					// 20% chance of spawning dragon's breath cloud
					if (block != null) {
						if (new Random().nextInt(5) == 0) {
							isSpawningBreath = true;
						}
					}
					// Will always spawn dragon's breath
					else if (e.getHitEntity() != null) {
						isSpawningBreath = true;
					}
				}


				if (isSpawningBreath) {
					AreaEffectCloud cloud = (AreaEffectCloud) arrow.getWorld().spawnEntity(
							arrow.getLocation(), EntityType.AREA_EFFECT_CLOUD);

					cloud.setParticle(Particle.ENTITY_EFFECT, Color.fromRGB(170, 0, 255));

					cloud.setRadius(3.0F);
					cloud.setDuration(50);
					cloud.setRadiusPerTick(-0.1F);
					cloud.setWaitTime(10);
					cloud.setReapplicationDelay(20);

					cloud.clearCustomEffects();
					cloud.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);
				}

				// 60% chance of breaking
				if (new Random().nextInt(10) >= 4) {
					e.getEntity().remove();
				}
			}
		}

		if (block != null) {
			if (arrow.getShooter() instanceof Player shooter) {
				if (block.getType().name().endsWith("GLASS") || block.getType().name().endsWith("GLASS_PANE")
						|| block.getType() == Material.ICE) {
					Dominion shooterDominion = DominionUtils.getPlayerDominion(shooter.getUniqueId());
					Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
					if (chunkDominion == null || (shooterDominion == null && chunkDominion == null)
							|| (shooterDominion != null && shooterDominion.getLeader().equals(chunkDominion.getLeader()))) {
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
