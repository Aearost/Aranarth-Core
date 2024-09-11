package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;

public class CoralDry implements Listener {

	public CoralDry(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents coral blocks from drying out in the survival world.
	 * @param e The event.
	 */
	@EventHandler
	public void onCoralDry(final BlockFadeEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("world") ||
				e.getBlock().getWorld().getName().equalsIgnoreCase("creative")) {
			if (isCoral(e.getBlock().getType())) {
				e.setCancelled(true);
			}
		}
	}
	
	private boolean isCoral(Material block) {
		return block == Material.BRAIN_CORAL|| block == Material.BRAIN_CORAL_BLOCK ||
				block == Material.BRAIN_CORAL_FAN || block == Material.BRAIN_CORAL_WALL_FAN ||
				block == Material.BUBBLE_CORAL || block == Material.BUBBLE_CORAL_BLOCK ||
				block == Material.BUBBLE_CORAL_FAN || block == Material.BUBBLE_CORAL_WALL_FAN ||
				block == Material.FIRE_CORAL || block == Material.FIRE_CORAL_BLOCK ||
				block == Material.FIRE_CORAL_FAN || block == Material.FIRE_CORAL_WALL_FAN ||
				block == Material.HORN_CORAL || block == Material.HORN_CORAL_BLOCK ||
				block == Material.HORN_CORAL_FAN || block == Material.HORN_CORAL_WALL_FAN ||
				block == Material.TUBE_CORAL || block == Material.TUBE_CORAL_BLOCK ||
				block == Material.TUBE_CORAL_FAN || block == Material.TUBE_CORAL_WALL_FAN;
	}
}
