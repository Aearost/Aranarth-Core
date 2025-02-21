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
import org.bukkit.event.block.BlockBreakEvent;

public class PlayerShopDestroy implements Listener {

	public PlayerShopDestroy(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the deletion of a player shop.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerShopDestroy(final BlockBreakEvent e) {
		if (isSign(e.getBlock().getType())) {
			Location destroyedSignLocation = e.getBlock().getLocation();
			Location blockLocationBelow = new Location(destroyedSignLocation.getWorld(),
					destroyedSignLocation.getBlockX(),
					destroyedSignLocation.getBlockY() - 1,
					destroyedSignLocation.getBlockZ());
			if (isChestBlock(blockLocationBelow.getBlock().getType())) {
				if (AranarthUtils.isShop(blockLocationBelow)) {
					PlayerShop playerShop = AranarthUtils.getShop(blockLocationBelow);
					if (e.getPlayer().getUniqueId() == playerShop.getUuid()) {
						AranarthUtils.removeShop(e.getPlayer().getUniqueId(), blockLocationBelow);
					} else {
						e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's shop!"));
					}
				}
			}
		}
		else if (isChestBlock(e.getBlock().getType())) {
			Location destroyedChestLocation = e.getBlock().getLocation();
			Location blockLocationAbove = new Location(destroyedChestLocation.getWorld(),
					destroyedChestLocation.getBlockX(),
					destroyedChestLocation.getBlockY() + 1,
					destroyedChestLocation.getBlockZ());
			if (isSign(blockLocationAbove.getBlock().getType())) {
				if (AranarthUtils.isShop(destroyedChestLocation)) {
					PlayerShop playerShop = AranarthUtils.getShop(destroyedChestLocation);
					if (e.getPlayer().getUniqueId() == playerShop.getUuid()) {
						AranarthUtils.removeShop(e.getPlayer().getUniqueId(), destroyedChestLocation);
					} else {
						e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's shop!"));
					}
				}
			}
		}
	}

	private boolean isSign(Material type) {
		return type.name().toLowerCase().endsWith("sign");
	}

	private boolean isChestBlock(Material type) {
		return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
	}

}
