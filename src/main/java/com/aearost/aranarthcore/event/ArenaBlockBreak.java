package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class ArenaBlockBreak implements Listener {

	public ArenaBlockBreak(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents blocks in the arena world's spawn from being destroyed.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onArenaBlockBreak(final BlockBreakEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("arena")
				&& !e.getPlayer().getName().equalsIgnoreCase("Aearost")) {
			int x = e.getBlock().getX();
			int y = e.getBlock().getY();
			int z = e.getBlock().getZ();

			if ((x >= -4 && x <= 4) && (y >= 100 && y <= 111) && (z >= -4 && z <=4)) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessageError("You cannot break this!"));
			}
		}

	}
}
