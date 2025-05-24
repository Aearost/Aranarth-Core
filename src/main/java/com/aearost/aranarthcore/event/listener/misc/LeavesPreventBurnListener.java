package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;

public class LeavesPreventBurnListener implements Listener {

	public LeavesPreventBurnListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents manually placed leaves from burning.
	 * @param e The event.
	 */
	@EventHandler
	public void onLeavesBurn(BlockIgniteEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("world")) {
			for (BlockFace face : BlockFace.values()) {
				Block relative = e.getBlock().getRelative(face);
				if (relative.getBlockData() instanceof Leaves leaves) {
					if (leaves.isPersistent()) {
						e.setCancelled(true);
						break;
					}
				}
			}
		}
	}
}
