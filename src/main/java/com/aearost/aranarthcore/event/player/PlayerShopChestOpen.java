package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Prevents a player from opening a player shop chest that is not theirs.
 */
public class PlayerShopChestOpen {
	public void execute(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null) {
			Location chestLocation = e.getClickedBlock().getLocation();
			Location locationAbove = new Location(chestLocation.getWorld(),
					chestLocation.getBlockX(), chestLocation.getBlockY() + 1, chestLocation.getBlockZ());
			if (isSign(locationAbove.getBlock().getType())) {
				PlayerShop playerShop = AranarthUtils.getShop(locationAbove);
				if (playerShop != null) {
					if (!playerShop.getUuid().toString().equals(e.getPlayer().getUniqueId().toString())) {
						e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou do not own this player shop chest!"));
						e.setCancelled(true);
					}
				}
			}
		}
	}

	/**
	 * Determines if the clicked block is a sign.
	 * @param type The type of material.
	 * @return Confirmation of whether the block is a sign.
	 */
	private boolean isSign(Material type) {
		return type.name().toLowerCase().endsWith("sign");
	}
}
