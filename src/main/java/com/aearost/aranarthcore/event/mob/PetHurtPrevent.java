package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
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

						OfflinePlayer owner = (OfflinePlayer) pet.getOwner();
						// Prevent the player from attacking their own pet anywhere
						if (owner.getUniqueId().equals(attacker.getUniqueId())) {
							e.setCancelled(true);
							attacker.sendMessage(ChatUtils.chatMessage("&7You cannot hurt your own &7"
									+ ChatUtils.getFormattedItemName(pet.getType().name()) + "!"));
							return;
						}
						// Prevent damage to pets that are sitting in the chunk of any Dominion
						else {
							Dominion chunkDominion = DominionUtils.getDominionOfChunk(e.getEntity().getLocation().getChunk());
							if (chunkDominion != null) {
								if (pet instanceof Sittable sittable) {
									if (sittable.isSitting()) {
										e.setCancelled(true);
										attacker.sendMessage(ChatUtils.chatMessage("&7You cannot hurt &e" + AranarthUtils.getNickname(owner) + "'s &7"
												+ ChatUtils.getFormattedItemName(pet.getType().name()) + "!"));
										return;
									}
								}
							}
						}

						// Un-sits the pet once they're attacked
						if (pet instanceof Sittable sittable) {
							if (sittable.isSitting()) {
								sittable.setSitting(false);
							}
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
