package com.aearost.aranarthcore.event.world;

import org.bukkit.damage.DamageType;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Increases fire tick damage during the month of Ardorvor.
 */
public class FireDamageIncrease {
	public void execute(EntityDamageEvent e) {
		// Tripled when in fire
		if (e.getDamageSource().getDamageType() == DamageType.IN_FIRE) {
			e.setDamage(e.getDamage() * 3);
		}
		// Doubled when on fire
		else if (e.getDamageSource().getDamageType() == DamageType.ON_FIRE) {
			e.setDamage(e.getDamage() * 2);
		}
	}
}
