package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Prevents blocks in the arena world's spawn from being destroyed.
 */
public class ArenaBlockBreak {
	public void execute(final BlockBreakEvent e) {
		if (!e.getPlayer().getName().equalsIgnoreCase("Aearost")) {
			int x = e.getBlock().getX();
			int y = e.getBlock().getY();
			int z = e.getBlock().getZ();

			if ((x >= -4 && x <= 4) && (y >= 100 && y <= 111) && (z >= -4 && z <=4)) {
				e.setCancelled(true);
				e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot break this!"));
			}
		}
	}
}
