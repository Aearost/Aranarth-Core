package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ArenaItemDrops implements Listener {

	public ArenaItemDrops(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents all items from being dropped in the arena world.
	 * 
	 * This includes items dropped by destroying a block, as well as players
	 * dropping items, including on their death. The item will simply not spawn.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onArenaItemDrop(final ItemSpawnEvent e) {
		if (e.getLocation().getWorld().getName().toLowerCase().equals("arena")) {
			if (e.getEntity().getItemStack().getType() != Material.IRON_INGOT
					&& e.getEntity().getItemStack().getType() != Material.ARROW) {
				e.setCancelled(true);
			}
		}
	}

}
