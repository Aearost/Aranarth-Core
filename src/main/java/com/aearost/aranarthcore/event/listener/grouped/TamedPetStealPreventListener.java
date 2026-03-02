package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class TamedPetStealPreventListener implements Listener {

	public TamedPetStealPreventListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	private boolean isAttemptingToLeash = false;

	/**
	 * Deals with preventing players from leashing animals that are not their own.
	 * @param e The event.
	 */
	@EventHandler
	public void onMobLeash(PlayerLeashEntityEvent e) {
		Player player = e.getPlayer();
		if (e.getEntity() instanceof Tameable tameable) {
			if (!tameable.getOwner().getUniqueId().equals(player.getUniqueId())) {
				isAttemptingToLeash = true;
				e.setCancelled(true);
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(tameable.getOwner().getUniqueId());
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot leash &e"
						+ aranarthPlayer.getNickname() + "'s &e" + ChatUtils.getFormattedItemName(e.getEntity().getType().name())));
			}
		}
	}

	@EventHandler
	public void onHorseMount(VehicleEnterEvent e) {
		if (e.getEntered() instanceof Player player) {
			if (e.getVehicle() instanceof LivingEntity mob) {
				if (mob instanceof Tameable tameable) {
					if (tameable.getOwner() != null) {
						if (!tameable.getOwner().getUniqueId().equals(player.getUniqueId())) {
							e.setCancelled(true);
							if (!isAttemptingToLeash) {
								AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(tameable.getOwner().getUniqueId());
								player.sendMessage(ChatUtils.chatMessage("&cYou cannot ride &e"
										+ aranarthPlayer.getNickname() + "'s &e" + ChatUtils.getFormattedItemName(mob.getType().name())));
							} else {
								isAttemptingToLeash = false;
							}
						}
					}
				}
			}
		}
	}
}
