package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
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
		
//		if (Objects.isNull(e.getOldEffect())) {
//			System.out.println("Old is null");
//		} else {
//			System.out.println("Old Type: " + e.getOldEffect().getType().getKey().toString());
//			System.out.println("Old Duration: " + e.getOldEffect().getDuration());
//			System.out.println("Old Amplifier: " + e.getOldEffect().getAmplifier());
//		}
//		
//		if (Objects.isNull(e.getNewEffect())) {
//			System.out.println("New is null");
//		} else {
//			System.out.println("New Type: " + e.getNewEffect().getType().getKey().toString());
//			System.out.println("New Duration: " + e.getNewEffect().getDuration());
//			System.out.println("New Amplifier: " + e.getNewEffect().getAmplifier());
//		}
		
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
				
				// Do not apply armor trim effects numerous times
				if (newEffect.getAmplifier() == 2) {
					return;
				}
				
				// Adding restrictions based on each potion effect
				if (newEffect.getType() == PotionEffectType.SPEED) {
					if (stackedAmplifier >= 10) {
						stackedAmplifier = 9;
					}
				}
				entity.removePotionEffect(newEffect.getType());
				entity.addPotionEffect(new PotionEffect(newEffect.getType(), newEffect.getDuration(), stackedAmplifier));
			}
		}
	}
	
}
