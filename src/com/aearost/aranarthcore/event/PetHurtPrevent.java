package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class PetHurtPrevent implements Listener {

	public PetHurtPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from hurting pets (tamed animals).
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPetHurt(final EntityDamageEvent e) {
		Entity entity = e.getEntity();
		if (entity instanceof Tameable) {
			if (isPlayerCausedDamage(e.getCause())) {
				Tameable pet = (Tameable) entity;
				if (Objects.nonNull(pet.getOwner())) {
					if (pet.getOwner() instanceof Player) {
						if (Objects.nonNull(e.getDamageSource().getCausingEntity())
								&& e.getDamageSource().getCausingEntity() instanceof Player) {
							e.setCancelled(true);
							Player owner = (Player) pet.getOwner();
							Player attacker = (Player) e.getDamageSource().getCausingEntity();
							
							if (owner.getUniqueId().equals(attacker.getUniqueId())) {
								attacker.sendMessage(ChatUtils.chatMessage("&7You cannot hurt your own &7"
										+ ChatUtils.getFormattedItemName(pet.getType().name())));
							} else {
								attacker.sendMessage(ChatUtils.chatMessage("&7You cannot hurt &e" + owner.getDisplayName() + "'s &7"
										+ ChatUtils.getFormattedItemName(pet.getType().name())));
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
