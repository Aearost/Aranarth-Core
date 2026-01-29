package com.aearost.aranarthcore.event.mob;

import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Opens a GUI to view a villager's inventory when right-clicked while sneaking.
 */
public class VillagerCamelPickup {
	public void execute(final PlayerInteractEntityEvent e) {
		Player player = e.getPlayer();

		if (player.isInsideVehicle()) {
			Entity mount = player.getVehicle();
			if (player.getVehicle() instanceof Camel) {
				mount.addPassenger(e.getRightClicked());
			}
		}
	}
}
