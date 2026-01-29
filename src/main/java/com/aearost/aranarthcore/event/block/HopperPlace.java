package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents the placing of a hopper under a locked container that the user is not the owner of.
 */
public class HopperPlace {
	public void execute(BlockPlaceEvent e) {
		ItemStack item = e.getItemInHand();
		Location location = e.getBlockPlaced().getLocation();
		Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
		if (AranarthUtils.isContainerBlock(locationAbove.getBlock())) {
			LockedContainer lockedContainer = AranarthUtils.getLockedContainerAtBlock(locationAbove.getBlock());
			if (lockedContainer != null) {
				if (!lockedContainer.getOwner().equals(e.getPlayer().getUniqueId())) {
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place a hopper here!"));
					e.setCancelled(true);
				}
			}
		}
	}
}
