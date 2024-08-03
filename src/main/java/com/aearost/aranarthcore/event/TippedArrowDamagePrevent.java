package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionType;

import com.aearost.aranarthcore.AranarthCore;

public class TippedArrowDamagePrevent implements Listener {

	public TippedArrowDamagePrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents damage from being taken from tipped arrows that apply a positive potion effect.
	 * @param e The event.
	 */
	@EventHandler
	public void onArrowHitPlayer(final ProjectileHitEvent e) {
		if (Objects.nonNull(e.getHitEntity())) {
			if (e.getHitEntity() instanceof Player) {
                if (e.getEntity() instanceof Arrow arrow) {
                    if (Objects.nonNull(arrow.getBasePotionType())) {
						boolean shouldPotionDamage = checkIfPotionShouldDamage(arrow.getBasePotionType());
						if (!shouldPotionDamage) {
							e.setCancelled(true);
							
							// Will need to determine how to create a PotionEffect from a PotionType
							// Difficulties are when the potion effects are from mcMMO
							// PotionEffect exists but PotionType does not as i.e Absorpion is not a potion in vanilla
//							player.addPotionEffect(null);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Verifies the potion type if it should damage the player.
	 * @param type The type of potion.
	 * @return Confirmation of whether the potion would do damage.
	 */
	private boolean checkIfPotionShouldDamage(PotionType type) {
		return (type == PotionType.AWKWARD || type == PotionType.HARMING || type == PotionType.MUNDANE
				|| type == PotionType.OOZING || type == PotionType.POISON || type == PotionType.SLOWNESS
				|| type == PotionType.THICK || type == PotionType.WATER || type == PotionType.WEAKNESS);
	}
}
