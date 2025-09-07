package com.aearost.aranarthcore.event.player;

import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;

/**
 * Prevents damage from being taken from tipped arrows that apply a positive potion effect.
 */
public class SpecialArrowDamageEffects {
	public void execute(EntityDamageEvent e) {
		if (e.getDamageSource().getDirectEntity() instanceof Arrow arrow) {
			if (e.getDamageSource().getDamageType() == DamageType.ARROW) {
				if (arrow.getPersistentDataContainer().has(ARROW)) {
					if (arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING).equals("iron")
						|| arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING).equals("gold")) {
						e.setDamage(e.getDamage() + 2);
					}
				}
			}
		}
	}
}
