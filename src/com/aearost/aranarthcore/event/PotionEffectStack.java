package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.aearost.aranarthcore.AranarthCore;

public class PotionEffectStack implements Listener {

	public PotionEffectStack(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds functionality to stack potion effects with limits depending on the effect.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPotionAdd(final EntityPotionEffectEvent e) {
		// Prevents recursive call from calling recursively again
		if (e.getAction() == Action.ADDED && e.getCause() == Cause.PLUGIN) {
			return;
		}
		
		if (e.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) e.getEntity();
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
				// Potential way to accomplish would be by adding a HashMap<PotionEffectType, boolean>
				// in AranarthPlayer which tracks if that player has the effect from a beacon
				// It would take extra validation to enable/disable
				if (e.getCause() == Cause.PLUGIN || e.getCause() == Cause.BEACON) {
					return;
				}
				
				// Adds amplifier restrictions based on the potion's type
				stackedAmplifier = determineEffectAmplifierRestriction(stackedAmplifier, newEffect.getType());
				
				// This will call the event recursively
				entity.addPotionEffect(new PotionEffect(newEffect.getType(), newEffect.getDuration(), stackedAmplifier));
			}
		}
	}
	
	/**
	 * Limits the amplifier level of each effect based on certain potion types.
	 * All types inevitably will be restricted to a6n amplifier of 10.
	 * 
	 * @param calculatedAmplifier
	 * @param type
	 * @return
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
			if (calculatedAmplifier >= 5) {
				calculatedAmplifier = 4;
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
		} else {
			if (calculatedAmplifier >= 10) {
				calculatedAmplifier = 9;
			}
		}
		
		return calculatedAmplifier;
	}
	
}
