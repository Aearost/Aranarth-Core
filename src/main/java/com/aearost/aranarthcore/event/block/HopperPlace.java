package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents the placing of a hopper under a player shop chest.
 */
public class HopperPlace {
	public void execute(BlockPlaceEvent e) {
		ItemStack item = e.getItemInHand();
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

	private boolean isChest(Material type) {
		return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
	}

}
