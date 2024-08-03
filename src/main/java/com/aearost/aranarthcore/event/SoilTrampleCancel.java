package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.aearost.aranarthcore.AranarthCore;

import java.util.Objects;

public class SoilTrampleCancel implements Listener {

	public SoilTrampleCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	
	/**
	 * Prevents crops from being trampled by a player.
	 * @param e The event.
	 */
	@EventHandler
	public void onTrample(final PlayerInteractEvent e) {
		Block block = e.getClickedBlock();
		
		if (e.getAction() == Action.PHYSICAL && Objects.requireNonNull(block).getType() == Material.FARMLAND) {
			e.setCancelled(true);
		}
			
	}
	
	
	/**
	 * Prevents crops from being trampled by mobs
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onTrample(EntityInteractEvent e) {
		Block block = e.getBlock();
		
		if (block.getType() == Material.FARMLAND) {
			e.setCancelled(true);
		}
			
	}
}
