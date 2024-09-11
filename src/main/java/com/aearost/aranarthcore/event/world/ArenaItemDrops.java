package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ArenaItemDrops implements Listener {

	public ArenaItemDrops(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents all items from being dropped in the arena world.
	 * @param e The event.
	 */
	@EventHandler
	public void onArenaItemDrop(final ItemSpawnEvent e) {
		if (e.getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
			if (e.getEntity().getItemStack().getType() != Material.IRON_INGOT
					&& e.getEntity().getItemStack().getType() != Material.ARROW) {
				e.setCancelled(true);
			}
		}
	}

}
