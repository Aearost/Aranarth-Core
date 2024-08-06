package com.aearost.aranarthcore.event.world;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ArenaGrowPrevent implements Listener {

	public ArenaGrowPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents blocks in the arena world from growing.
	 * @param e The event.
	 */
	@EventHandler
	public void onArenaBlockGrow(final BlockGrowEvent e) {
		if (e.getBlock().getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
			Material material = e.getBlock().getType();
			if (material == Material.VINE || material == Material.CAVE_VINES_PLANT) {
				e.setCancelled(true);
			}
		}
	}
}
