package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Prevents the placement of Sugarcane Blocks.
 */
public class SugarcaneBlockPlace {
	
	public void execute(BlockPlaceEvent e) {
		e.setCancelled(true);
		e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place a Block of Sugarcane!"));
	}

}
