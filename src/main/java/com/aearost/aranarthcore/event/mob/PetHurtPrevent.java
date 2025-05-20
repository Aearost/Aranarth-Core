package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Objects;

/**
 * Prevents players from hurting pets (tamed animals).
 */
public class PetHurtPrevent {
	public void execute(EntityDamageEvent e) {
		Entity entity = e.getEntity();
		if (isPlayerCausedDamage(e.getCause())) {
			Tameable pet = (Tameable) entity;
			if (Objects.nonNull(pet.getOwner())) {
				if (pet.getOwner() instanceof OfflinePlayer) {
					if (Objects.nonNull(e.getDamageSource().getCausingEntity())
							&& e.getDamageSource().getCausingEntity() instanceof Player attacker) {
						e.setCancelled(true);
						OfflinePlayer owner = (OfflinePlayer) pet.getOwner();

						if (owner.getUniqueId().equals(attacker.getUniqueId())) {
							attacker.sendMessage(ChatUtils.chatMessage("&7You cannot hurt your own &7"
									+ ChatUtils.getFormattedItemName(pet.getType().name()) + "!"));
						} else {
							attacker.sendMessage(ChatUtils.chatMessage("&7You cannot hurt &e" + AranarthUtils.getNickname(owner) + "'s &7"
									+ ChatUtils.getFormattedItemName(pet.getType().name()) + "!"));
						}
					}
				}
			}
		}
	}
	
	private boolean isPlayerCausedDamage(DamageCause cause) {
		return (cause == DamageCause.ENTITY_ATTACK) || (cause == DamageCause.ENTITY_SWEEP_ATTACK)
				|| (cause == DamageCause.PROJECTILE);
	}
}
