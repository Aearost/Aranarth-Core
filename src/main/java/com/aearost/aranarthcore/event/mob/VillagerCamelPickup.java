package com.aearost.aranarthcore.event.mob;

import org.bukkit.Bukkit;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.aearost.aranarthcore.AranarthCore;

public class VillagerCamelPickup implements Listener {

	public VillagerCamelPickup(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Opens a GUI to view a villager's inventory when right-clicked while sneaking.
	 * @param e The event.
	 */
	@EventHandler
	public void onVillagerClick(final PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof Villager) {
			Player player = e.getPlayer();
			
			if (player.isInsideVehicle()) {
				Entity mount = player.getVehicle();
				if (player.getVehicle() instanceof Camel) {
					mount.addPassenger(e.getRightClicked());
				}
			}
		}
	}
}
