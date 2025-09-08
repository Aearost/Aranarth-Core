package com.aearost.aranarthcore.event.player;

import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

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
						Random random = new Random();
						e.setDamage(e.getDamage() + random.nextInt(2) + 2);
					} else if (arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING).equals("amethyst")) {
						e.setDamage(e.getDamage() + 4);
					} else if (arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING).equals("obsidian")) {
						e.setDamage(e.getDamage() + 10);
					} else if (arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING).equals("diamond")) {
						e.setDamage(e.getDamage() + 12);
					}
				}
			}
		}
	}
}
