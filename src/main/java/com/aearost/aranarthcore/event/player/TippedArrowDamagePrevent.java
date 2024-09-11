package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

public class TippedArrowDamagePrevent implements Listener {

	public TippedArrowDamagePrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents damage from being taken from tipped arrows that apply a positive potion effect.
	 * @param e The event.
	 */
	@EventHandler
	public void onArrowHitPlayer(final EntityDamageEvent e) {
		if (e.getDamageSource().getDirectEntity() instanceof Arrow arrow) {
			if (e.getDamageSource().getCausingEntity() instanceof Player) {
				if (e.getEntity() instanceof Player player) {
					if (arrow.getItem().hasItemMeta()) {
						if (arrow.getItem().getItemMeta() instanceof PotionMeta potionMeta) {
							boolean shouldPotionDamage = checkIfPotionShouldDamage(potionMeta.getBasePotionType());
							// If it is a positive effect, do not damage and apply the effect
							if (!shouldPotionDamage) {
								e.setCancelled(true);
                                for (PotionEffect effect : potionMeta.getBasePotionType().getPotionEffects()) {
									PotionEffect newEffect = new PotionEffect(
											effect.getType(), effect.getDuration() / 8, effect.getAmplifier());
									AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
									aranarthPlayer.setIsHitByTippedArrow(true);
									AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
									player.addPotionEffect(newEffect);
									AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
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
		return (type == PotionType.AWKWARD || type == PotionType.HARMING || type == PotionType.MUNDANE
				|| type == PotionType.OOZING || type == PotionType.POISON || type == PotionType.SLOWNESS
				|| type == PotionType.THICK || type == PotionType.WATER || type == PotionType.WEAKNESS
				|| type == PotionType.TURTLE_MASTER || type == PotionType.WIND_CHARGED || type == PotionType.WEAVING
				|| type == PotionType.INFESTED
		);
	}
}
