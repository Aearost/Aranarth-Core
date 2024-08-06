package com.aearost.aranarthcore.event.world;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ArenaMeltPrevent implements Listener {

	public ArenaMeltPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents blocks in the arena world from melting.
	 * @param e The event.
	 */
	@EventHandler
	public void onArenaBlockMelt(final BlockFadeEvent e) {
		if (e.getBlock().getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
			Material material = e.getBlock().getType();
			if (material == Material.ICE || material == Material.PACKED_ICE || material == Material.BLUE_ICE ||
					material == Material.SNOW || material == Material.SNOW_BLOCK) {
				e.setCancelled(true);
			}
		}
	}
}
