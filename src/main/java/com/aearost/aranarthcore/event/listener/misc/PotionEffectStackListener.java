package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class PotionEffectStackListener implements Listener {

	public PotionEffectStackListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds functionality to stack potion effects with limits depending on the effect.
	 * @param e The event.
	 */
	@EventHandler
	public void onPotionAdd(EntityPotionEffectEvent e) {

		// Prevents a new potion effect from being called if the effect already is applied
		if (e.getAction() == Action.ADDED && e.getCause() == Cause.PLUGIN && e.getOldEffect() != null) {
			return;
		}

		if (e.getEntity() instanceof LivingEntity entity) {
			if (e.getCause() == Cause.PLUGIN && DateUtils.isWinterMonth(AranarthUtils.getMonth())) {
				if (e.getEntity() instanceof Player player) {
					if (AranarthUtils.isWearingArmorType(player, "scorched")) {
						// If it is the slowness applied by winter months
						boolean applyingWithoutExistingSlowness = e.getOldEffect() == null
								&& e.getNewEffect() != null
								&& e.getNewEffect().getType() == PotionEffectType.SLOWNESS;
						boolean applyingWithExistingSlowness = e.getOldEffect() != null
								&& e.getOldEffect().getType() == PotionEffectType.SLOWNESS
								&& e.getNewEffect() != null
								&& e.getNewEffect().getType() == PotionEffectType.SLOWNESS;

						if (applyingWithoutExistingSlowness || applyingWithExistingSlowness) {
							e.setCancelled(true);
							return;
						}
					}
				}
			}

            // If the player currently has that same effect and is re-applying it
			if (Objects.nonNull(e.getOldEffect()) && Objects.nonNull(e.getNewEffect())) {
				PotionEffect oldEffect = e.getOldEffect();
				PotionEffect newEffect = e.getNewEffect();

				int stackedAmplifier = 0;
				if (oldEffect.getAmplifier() == 0 && newEffect.getAmplifier() == 0) {
					stackedAmplifier = 1;
				} else if (oldEffect.getAmplifier() == 0 && newEffect.getAmplifier() > 0) {
					stackedAmplifier = newEffect.getAmplifier() + 1;
				} else if (oldEffect.getAmplifier() > 0 && newEffect.getAmplifier() == 0) {
					stackedAmplifier = oldEffect.getAmplifier() + 1;
				} else if (oldEffect.getAmplifier() > 0 && newEffect.getAmplifier() > 0) {
					stackedAmplifier = oldEffect.getAmplifier() + newEffect.getAmplifier() + 1;
				}

				// Do not apply armor trim or beacon effects if player already has the effect
				// If the effect is coming from an arrow, allow it to stack
				if (e.getCause() == Cause.PLUGIN || e.getCause() == Cause.BEACON) {
					if (entity instanceof Player player) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						if (aranarthPlayer.isHitByTippedArrow()) {
							Bukkit.getLogger().info("Hit by tipped arrow");
							aranarthPlayer.setHitByTippedArrow(false);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						} else {
							return;
						}
					} else {
						return;
					}
				}
				// Adds amplifier restrictions based on the potion's type
				stackedAmplifier = determineEffectAmplifierRestriction(stackedAmplifier, newEffect.getType());
				// This will call the event recursively
				entity.addPotionEffect(new PotionEffect(newEffect.getType(), newEffect.getDuration(), stackedAmplifier));
			}
			// It is a new potion effect, only needed to disable the hit by tipped arrow variable
			else {
				if (entity instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (aranarthPlayer.isHitByTippedArrow()) {
						aranarthPlayer.setHitByTippedArrow(false);
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

						// Must be applied manually
						if (e.getNewEffect().getType() == PotionEffectType.INSTANT_HEALTH) {
							if (e.getNewEffect().getAmplifier() == 0) {
								double newHealth = player.getHealth() + 4;
								if (newHealth > 20) {
									newHealth = 20;
								}
								player.setHealth(newHealth);
							} else {
								double newHealth = player.getHealth() + 8;
								if (newHealth > 20) {
									newHealth = 20;
								}
								player.setHealth(newHealth);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Limits the amplifier level of each effect based on certain potion types.
	 * All types inevitably will be restricted to an amplifier of 10.
	 * @param calculatedAmplifier The amplifier value that was calculated.
	 * @param type The effect type.
	 * @return The amplifier after it has been capped accordingly.
	 */
	private int determineEffectAmplifierRestriction(int calculatedAmplifier, PotionEffectType type) {
		 if (type == PotionEffectType.ABSORPTION) {
				if (calculatedAmplifier >= 5) {
					calculatedAmplifier = 4;
				}
		} else if (type == PotionEffectType.HEALTH_BOOST) {
			if (calculatedAmplifier >= 5) {
				calculatedAmplifier = 4;
			}
		} else if (type == PotionEffectType.LEVITATION) {
			if (calculatedAmplifier >= 3) {
				calculatedAmplifier = 2;
			}
		} else if (type == PotionEffectType.REGENERATION) {
			if (calculatedAmplifier >= 3) {
				calculatedAmplifier = 2;
			}
		} else if (type == PotionEffectType.RESISTANCE) {
			if (calculatedAmplifier >= 3) {
				calculatedAmplifier = 2;
			}
		} else if (type == PotionEffectType.SLOWNESS) {
			if (calculatedAmplifier >= 5) {
				calculatedAmplifier = 4;
			}
		} else if (type == PotionEffectType.STRENGTH) {
			if (calculatedAmplifier >= 3) {
				calculatedAmplifier = 2;
			}
		} else if (type == PotionEffectType.WEAKNESS) {
			if (calculatedAmplifier >= 3) {
				calculatedAmplifier = 2;
			}
		} else if (type == PotionEffectType.WITHER) {
			 if (calculatedAmplifier >= 3) {
				 calculatedAmplifier = 2;
			 }
		} else if (type == PotionEffectType.DARKNESS) {
			 if (calculatedAmplifier >= 5) {
				 calculatedAmplifier = 4;
			 }
		} else {
			if (calculatedAmplifier >= 10) {
				calculatedAmplifier = 9;
			}
		}
		
		return calculatedAmplifier;
	}
	
}
