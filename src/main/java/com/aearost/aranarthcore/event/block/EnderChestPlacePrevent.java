package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class EnderChestPlacePrevent implements Listener {

	public EnderChestPlacePrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents ender chests from being placed in creative.
	 * @param e The event.
	 */
	@EventHandler
	public void onEnderChestPlate(final BlockPlaceEvent e) {
		if (e.getBlockPlaced().getType() == Material.ENDER_CHEST) {
			if (e.getPlayer().getWorld().getName().equalsIgnoreCase("creative")) {
				if (!e.getPlayer().isOp()) {
					e.setCancelled(true);
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place an enderchest in creative!"));
					for (int i = 0; i < 100; i++) {
						Bukkit.getLogger().info(e.getPlayer().getName() + " placed an enderchest in creative!");
					}
				}
			}
		}
		
	}

}
