package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.CompressedCobblestone;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CompressedCobblestonePlaceCancel implements Listener {

	public CompressedCobblestonePlaceCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a new entry to the players HashMap if the player is not being tracked.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onHomePadPlace(final BlockPlaceEvent e) {
		
		ItemStack item = e.getItemInHand();
		if (item.isSimilar((CompressedCobblestone.getCompressedCobblestone()))) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place compressed cobblestone!"));
		}
		
	}

}
