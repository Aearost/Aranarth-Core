package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;

public class LeavesPreventBurn implements Listener {

	public LeavesPreventBurn(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents manually placed leaves from burning.
	 * @param e The event.
	 */
	@EventHandler
	public void onLeavesBurn(final BlockIgniteEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("world")) {
			if (e.getBlock().getBlockData() instanceof Leaves leaves) {
				if (leaves.isPersistent()) {
					e.setCancelled(true);
				}
			}
		}
	}
}
