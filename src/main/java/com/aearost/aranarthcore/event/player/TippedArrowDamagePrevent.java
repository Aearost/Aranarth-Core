package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

/**
 * Prevents damage from being taken from tipped arrows that apply a positive potion effect.
 */
public class TippedArrowDamagePrevent {
	public void execute(EntityDamageEvent e) {
		if (e.getDamageSource().getDirectEntity() instanceof Arrow arrow) {
			if (e.getDamageSource().getCausingEntity() instanceof Player) {
				if (e.getEntity() instanceof Player player) {
					if (arrow.getItemStack().hasItemMeta()) {
						if (arrow.getItemStack().getItemMeta() instanceof PotionMeta potionMeta) {
							boolean shouldPotionDamage = checkIfPotionShouldDamage(potionMeta.getBasePotionType());
							// If it is a positive effect, do not damage and apply the effect
							if (!shouldPotionDamage) {
								e.setCancelled(true);
                                for (PotionEffect effect : potionMeta.getBasePotionType().getPotionEffects()) {
									PotionEffect newEffect = new PotionEffect(
											effect.getType(), effect.getDuration() / 8, effect.getAmplifier());
									AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
									aranarthPlayer.setHitByTippedArrow(true);
									AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
									player.addPotionEffect(newEffect);
								}
								e.getDamageSource().getDirectEntity().remove();
							}
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
		return type.name().contains("AWKWARD") || type.name().contains("HARMING") ||type.name().contains("MUNDANE") || type.name().contains("TURTLE_MASTER") ||
				type.name().contains("OOZING") || type.name().contains("POISON") ||type.name().contains("SLOWNESS") || type.name().contains("WIND_CHARGED") ||
				type.name().contains("THICK") || type.name().contains("WEAKNESS") || type.name().contains("WEAVING") || type.name().contains("INFESTED");
	}
}
