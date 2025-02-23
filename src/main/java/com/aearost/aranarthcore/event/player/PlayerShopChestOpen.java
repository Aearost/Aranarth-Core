package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerShopChestOpen implements Listener {

	public PlayerShopChestOpen(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents a player from opening a player shop chest that is not theirs.
	 * @param e The event.
	 */
	@EventHandler
	public void onChestOpen(final PlayerInteractEvent e) {
		if (isChest(e.getClickedBlock().getType())) {
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

	private boolean isChest(Material type) {
		return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
	}

	private boolean isSign(Material type) {
		return type.name().toLowerCase().endsWith("sign");
	}
}
