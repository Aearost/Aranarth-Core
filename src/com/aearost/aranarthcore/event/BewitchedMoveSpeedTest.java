package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import com.aearost.aranarthcore.AranarthCore;

public class BewitchedMoveSpeedTest implements Listener {

	public BewitchedMoveSpeedTest(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with displaying a chat message when a wandering trader spawns nearby a player.
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onMinecartMove(final VehicleMoveEvent e) {
		if (e.getVehicle() instanceof Minecart) {
			Minecart minecart = (Minecart) e.getVehicle();
//			System.out.println(minecart.getVelocity().length());
		} else {
//			System.out.println("???");
		}
		
	}

}
