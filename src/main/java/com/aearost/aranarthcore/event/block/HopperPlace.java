package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class HopperPlace implements Listener {

	public HopperPlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents the placing of a hopper under a player shop chest.
	 * @param e The event.
	 */
	@EventHandler
	public void onHopperPlace(final BlockPlaceEvent e) {
		ItemStack item = e.getItemInHand();
		if (item.getType() == Material.HOPPER) {
			Location location = e.getBlockPlaced().getLocation();
			Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
			if (isChest(locationAbove.getBlock().getType())) {
				Location locationAboveChest = new Location(location.getWorld(), location.getX(), location.getY() + 2, location.getZ());
				PlayerShop playerShop = AranarthUtils.getShop(locationAboveChest);

				if (playerShop != null) {
					e.setCancelled(true);
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place a hopper under a player shop!"));
				}
			}
		}
	}

	private boolean isChest(Material type) {
		return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
	}

}
