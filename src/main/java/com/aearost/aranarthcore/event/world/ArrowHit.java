package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
					cloud.setDuration(100);
					cloud.setRadiusPerTick(-0.03F);
					cloud.setWaitTime(10);
					cloud.setReapplicationDelay(20);

					cloud.clearCustomEffects();
					cloud.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);
				}

				// 60% chance of breaking
				if (new Random().nextInt(10) >= 4) {
					e.getEntity().remove();
				}
			} else if (type.equals("explosive")) {
				if (block != null) {
					if (arrow.getShooter() instanceof Player shooter) {
						Dominion shooterDominion = DominionUtils.getPlayerDominion(shooter.getUniqueId());
						Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
						if (chunkDominion == null
								|| (shooterDominion != null && shooterDominion.getLeader().equals(chunkDominion.getLeader()))) {
							int radius = 1; // 3x3 area (1 block in each direction)
							int power = 5;
							Location center = block.getLocation();

							for (int x = -radius; x <= radius; x++) {
								for (int y = -radius; y <= radius; y++) {
									for (int z = -radius; z <= radius; z++) {
										Location loc = center.clone().add(x, y, z);
										Block iteratedBlock = loc.getBlock();

										if (iteratedBlock.getType().isAir()) {
											continue;
										}

										double distance = center.distance(loc);

										// Make it spherical instead of cube
										if (distance > radius + 0.5) {
											continue;
										}

										float blastResistance = iteratedBlock.getType().getBlastResistance();

										// TNT power is normally 4.0
										// We simulate block survival chance based on resistance
										float resistanceFactor = blastResistance / 5.0f;

										// If resistance is too high/unbreaking
										if (resistanceFactor > power) {
											continue;
										}

										// Random chance like TNT
										float random = new Random().nextFloat();
										if (random * power > resistanceFactor) {
											center.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
											iteratedBlock.breakNaturally();
										}
									}
								}
							}
							center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
							center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
						} else {
							shooter.sendMessage(ChatUtils.chatMessage("&cYou are not in the Dominion of &e" + chunkDominion.getName()));
						}
						arrow.remove();
					}
				}
			} else if (type.equals("lightning")) {
				if (AranarthUtils.getWeather() == Weather.RAIN || AranarthUtils.getWeather() == Weather.THUNDER) {
					Location loc = arrow.getLocation();
					loc.getWorld().strikeLightning(loc);
					arrow.remove();
				}
			} else if (type.equals("rooting")) {
				if (e.getHitEntity() != null) {
					e.getHitEntity().getPersistentDataContainer().set(ARROW, PersistentDataType.STRING, "rooting");
					if (e.getHitEntity() instanceof Player player) {
						player.sendMessage(ChatUtils.chatMessage("#964B00You have been rooted!"));
					}

					// Removes the attribute after 30 seconds
					new BukkitRunnable() {
						@Override
						public void run() {
							e.getHitEntity().getPersistentDataContainer().remove(ARROW);
						}
					}.runTaskLater(AranarthCore.getInstance(), 30);
				}
			} else if (type.equals("gust")) {
				Location loc = arrow.getLocation();
				loc.add(0, 1, 0);
				WindCharge charge = (WindCharge) loc.getWorld().spawnEntity(loc, EntityType.WIND_CHARGE);
				charge.explode();
				arrow.remove();

				Location impact = arrow.getLocation().clone();

				// Radius from the landed location of the arrow
				for (Entity entity : arrow.getLocation().getNearbyEntities(1, 1, 1)) {
					if (!(entity instanceof LivingEntity)) {
						continue;
					}

					Vector direction = entity.getLocation().toVector().subtract(impact.toVector()).normalize();
					direction.multiply(2.5); // Changes the movement and direction
					direction.setY(1); // Lifts the entity slightly
					entity.setVelocity(direction);
					entity.setFallDistance(0); // Prevents weird fall damage
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
