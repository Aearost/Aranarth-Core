package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.MusicInstrument;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.util.concurrent.ThreadLocalRandom;

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
		PotionEffect oldEffect = e.getOldEffect();
		PotionEffect newEffect = e.getNewEffect();
		// Prevents a new potion effect from being called if the effect already is applied
		// Potions will stack but other forms of applied effects i.e bending passives or beacons will not
		// Potentially could add manual checks i.e if it's a beacon, stack logic and ignore below, preventing infinite recursive calls
		if (e.getAction() == Action.ADDED && e.getCause() == Cause.PLUGIN) {
			return;
		}

		if (newEffect != null && newEffect.getType().getCategory() == PotionEffectTypeCategory.HARMFUL) {
			// Do not apply mining fatigue from Elder guardians
			if (e.getEntity() instanceof Player player) {
				if (AranarthUtils.isWearingArmorType(player, "aquatic")) {
					if (newEffect.getType() == PotionEffectType.MINING_FATIGUE && e.getCause() == Cause.ATTACK) {
						e.setCancelled(true);
					}
				}

				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				Long lastUsed = aranarthPlayer.getHorns().get(MusicInstrument.DREAM_GOAT_HORN);
				// If the effect of the horn is still active, prevent new effects from being added
				if (lastUsed != null && (lastUsed + 5000 > System.currentTimeMillis())) {
					e.setCancelled(true);
					return;
				}
			}
		}

		if (e.getEntity() instanceof LivingEntity entity) {
			if (e.getCause() == Cause.PLUGIN && DateUtils.isWinterMonth(AranarthUtils.getMonth())) {
				if (e.getEntity() instanceof Player player) {
					if (AranarthUtils.isWearingArmorType(player, "scorched")) {
						// If it is the slowness applied by winter months
						boolean applyingWithoutExistingSlowness = oldEffect == null
								&& newEffect != null
								&& newEffect.getType() == PotionEffectType.SLOWNESS;
						boolean applyingWithExistingSlowness = oldEffect != null
								&& oldEffect.getType() == PotionEffectType.SLOWNESS
								&& newEffect != null
								&& newEffect.getType() == PotionEffectType.SLOWNESS;

						if (applyingWithoutExistingSlowness || applyingWithExistingSlowness) {
							e.setCancelled(true);
							return;
						}
					}
				}
			}

			// Do not proceed if there is not a new effect being applied
			if (newEffect == null) {
				// Allow proceeding only for instant health arrows
				if (e.getCause() != Cause.ARROW && oldEffect != null && oldEffect.getType() != PotionEffectType.INSTANT_HEALTH) {
					return;
				}
			}
			newEffect = applyStrigavorBuff(e);

            // If the player currently has that same effect and is re-applying it
			if (oldEffect != null && newEffect != null) {
				int stackedAmplifier = getStackedAmplifier(oldEffect, newEffect);

				// Do not apply aranarthium armor or beacon effects if player already has the effect
				// If the effect is coming from an arrow, allow it to stack
				if (e.getCause() == Cause.PLUGIN || e.getCause() == Cause.BEACON) {
					if (entity instanceof Player player) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						if (aranarthPlayer.isHitByTippedArrow()) {
							aranarthPlayer.setHitByTippedArrow(false);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						} else if (aranarthPlayer.isUsingGoatHorn()) {
							aranarthPlayer.setUsingGoatHorn(false);
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
			} else {
				// Must disable the hit by tipped arrow variable and manually apply instant health
				if (entity instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (aranarthPlayer.isHitByTippedArrow()) {
						aranarthPlayer.setHitByTippedArrow(false);
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

						// Must be applied manually
						if (oldEffect.getType() == PotionEffectType.INSTANT_HEALTH) {
							double newHealth = player.getHealth();
							if (oldEffect.getAmplifier() == 0) {
								newHealth += 4;
							} else if (oldEffect.getAmplifier() == 1) {
								newHealth += 8;
							}
							// Strigavor buffed it to be amplified III
							else {
								newHealth += 12;
							}

							if (newHealth > 20) {
								newHealth = 20;
							}
							player.setHealth(newHealth);
							return;
						}
					}
				}

				if (newEffect != null) {
					int stackedAmplifier = getStackedAmplifier(oldEffect, newEffect);
					// Adds amplifier restrictions based on the potion's type
					stackedAmplifier = determineEffectAmplifierRestriction(stackedAmplifier, newEffect.getType());
					// This will call the event recursively
					entity.addPotionEffect(new PotionEffect(newEffect.getType(), newEffect.getDuration(), stackedAmplifier));
				}
			}
		}
	}

	/**
	 * Provides the new amplified stacking the old and new potion effect of the same type.
	 * @param oldEffect The old effect.
	 * @param newEffect The new effect being applied.
	 * @return The new amplifier of the new effect to be used.
	 */
	private int getStackedAmplifier(PotionEffect oldEffect, PotionEffect newEffect) {
		int stackedAmplifier = 0;
		int oldAmplifier = oldEffect == null ? 0 : oldEffect.getAmplifier();
		int newAmplifier = newEffect == null ? 0 : newEffect.getAmplifier();

		if (oldAmplifier == 0 && newAmplifier == 0) {
			stackedAmplifier = 1;
		} else if (oldAmplifier == 0 && newAmplifier > 0) {
			stackedAmplifier = newAmplifier + 1;
		} else if (oldAmplifier > 0 && newAmplifier == 0) {
			stackedAmplifier = oldAmplifier + 1;
		} else if (oldAmplifier > 0 && newAmplifier > 0) {
			stackedAmplifier = oldAmplifier + newAmplifier + 1;
		}
		return stackedAmplifier;
	}

	/**
	 * Buffs to the potion effect during the month of Strigavor.
	 * 25% chance of increasing the amplifier by 1, and 25% chance of increasing the duration by 1.5x.
	 * @param e The event.
	 * @return The potentially buffed potion effect to be applied as the new effect.
	 */
	private PotionEffect applyStrigavorBuff(EntityPotionEffectEvent e) {
		PotionEffect newEffect = e.getNewEffect();
		if (shouldAttemptStrigavorBuff(e)) {
			int chance = ThreadLocalRandom.current().nextInt(100);
			// Amplify the effect
			if (chance < 25) {
				return new PotionEffect(newEffect.getType(), newEffect.getDuration(), newEffect.getAmplifier() + 1);
			}
			// Amplify the duration
			else if (chance < 50) {
				return new PotionEffect(newEffect.getType(), (int) (newEffect.getDuration() * 1.5), newEffect.getAmplifier());
			}
		}
		// Effect will remain the same
		return newEffect;
	}

	/**
	 * Determines whether the Strigavor month potion bonus should be applied for this event.
	 * Beneficial effects are enhanced during the day and harmful effects at night.
	 * @param e The event.
	 * @return Whether the Strigavor bonus should be applied.
	 */
	private boolean shouldAttemptStrigavorBuff(EntityPotionEffectEvent e) {
		if (AranarthUtils.getMonth() != Month.STRIGAVOR) {
			return false;
		}

		Cause cause = e.getCause();
		if (cause != Cause.POTION_DRINK && cause != Cause.POTION_SPLASH && cause != Cause.AREA_EFFECT_CLOUD) {
			return false;
		}

		String worldName = e.getEntity().getWorld().getName();
		if (worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource")) {
			long worldTime = e.getEntity().getWorld().getTime();
			boolean isDaytime = worldTime < 13000;
			PotionEffectTypeCategory category = e.getNewEffect().getType().getCategory();
			boolean isBeneficial = category == PotionEffectTypeCategory.BENEFICIAL;

			return (isBeneficial && isDaytime) || (!isBeneficial && !isDaytime) || category == PotionEffectTypeCategory.NEUTRAL;
		} else {
			return false;
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
		 if (type == org.bukkit.potion.PotionEffectType.ABSORPTION) {
				if (calculatedAmplifier >= 5) {
					calculatedAmplifier = 4;
				}
		} else if (type == org.bukkit.potion.PotionEffectType.HEALTH_BOOST) {
			if (calculatedAmplifier >= 5) {
				calculatedAmplifier = 4;
			}
		} else if (type == org.bukkit.potion.PotionEffectType.LEVITATION) {
			if (calculatedAmplifier >= 3) {
				calculatedAmplifier = 2;
			}
		} else if (type == org.bukkit.potion.PotionEffectType.REGENERATION) {
			if (calculatedAmplifier >= 3) {
				calculatedAmplifier = 2;
			}
		} else if (type == org.bukkit.potion.PotionEffectType.RESISTANCE) {
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
