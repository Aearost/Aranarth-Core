package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
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
		System.out.println("A");
		Entity entity = e.getEntity();
		if (entity instanceof Tameable) {
			System.out.println("B");
			if (isPlayerCausedDamage(e.getCause())) {
				System.out.println("C");
				Tameable pet = (Tameable) entity;
				if (Objects.nonNull(pet.getOwner())) {
					System.out.println("D");
					if (pet.getOwner() instanceof OfflinePlayer) {
						System.out.println("E");
						if (Objects.nonNull(e.getDamageSource().getCausingEntity())
								&& e.getDamageSource().getCausingEntity() instanceof Player) {
							System.out.println("F");
							e.setCancelled(true);
							OfflinePlayer owner = (OfflinePlayer) pet.getOwner();
							Player attacker = (Player) e.getDamageSource().getCausingEntity();
							
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
	}
	
	private boolean isPlayerCausedDamage(DamageCause cause) {
		System.out.println("Cause is: " + cause.name());
		return (cause == DamageCause.ENTITY_ATTACK) || (cause == DamageCause.ENTITY_SWEEP_ATTACK)
				|| (cause == DamageCause.PROJECTILE);
	}
}
