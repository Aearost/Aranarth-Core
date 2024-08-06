package com.aearost.aranarthcore.event.block;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class SugarcaneBlockPlace implements Listener {

	public SugarcaneBlockPlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents the placement of Sugarcane Blocks.
	 * @param e The event.
	 */
	@EventHandler
	public void onSugarcaneBlockPlace(final BlockPlaceEvent e) {
		ItemStack item = e.getItemInHand();
		if (item.getType() == Material.BAMBOO_BLOCK && item.getItemMeta().hasLore()) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatUtils.chatMessageError("You cannot place a Block of Sugarcane!"));
		}
		
	}

}
