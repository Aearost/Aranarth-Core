package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class HorseSwimToggle implements Listener {

	public HorseSwimToggle(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with enabling and disabling the Horse Swim movement
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onHorseSwimToggle(final PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND && (
				e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR)) {
			Player player = e.getPlayer();
			if (player.isInsideVehicle() && player.getVehicle() instanceof Horse) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (aranarthPlayer.getIsHorseSwimEnabled()) {
					player.sendMessage(ChatUtils.chatMessage("&7Your horse will no longer swim."));
					aranarthPlayer.setIsHorseSwimEnabled(false);
				} else {
					player.sendMessage(ChatUtils.chatMessage("&aYour horse will now swim!"));
					aranarthPlayer.setIsHorseSwimEnabled(true);
				}
				AranarthUtils.setPlayer(player, aranarthPlayer);
			}
			
			
		}
	}

}
