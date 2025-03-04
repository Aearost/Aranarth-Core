package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerShopInteract implements Listener {

	public PlayerShopInteract(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the interacting with a player shop.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerShopInteract(final PlayerInteractEvent e) {
		if (e.getClickedBlock() != null && isChest(e.getClickedBlock().getType())) {
			Location chestLocation = e.getClickedBlock().getLocation();
			Location locationAbove = new Location(chestLocation.getWorld(),
					chestLocation.getBlockX(), chestLocation.getBlockY() + 1, chestLocation.getBlockZ());
			if (isSign(locationAbove.getBlock().getType())) {
				PlayerShop playerShop = AranarthUtils.getShop(locationAbove);
				if (playerShop != null) {
					if (!playerShop.getUuid().toString().equals(e.getPlayer().getUniqueId().toString())) {
						e.setCancelled(true);
						Player player = e.getPlayer();
						AranarthPlayer clickUser = AranarthUtils.getPlayer(player.getUniqueId());
						AranarthPlayer shopUser = AranarthUtils.getPlayer(playerShop.getUuid());

						// Handles logic to buy/sell
						Sign sign = (Sign) locationAbove.getBlock().getState();
						if (sign.getSide(Side.FRONT).getLine(3).startsWith("Buy")) {
							if (clickUser.getBalance() >= playerShop.getBuyPrice()) {
								clickUser.setBalance(clickUser.getBalance() - playerShop.getBuyPrice());
								shopUser.setBalance(shopUser.getBalance() + playerShop.getBuyPrice());
								player.sendMessage(ChatUtils.chatMessage(
										"&7You have purchased &e" + playerShop.getQuantity()
												+ " " + ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())));
								// If the shop owner is online
								if (Bukkit.getPlayer(playerShop.getUuid()) != null) {
									Player shopPlayer = Bukkit.getPlayer(playerShop.getUuid());
									shopPlayer.sendMessage(
											ChatUtils.chatMessage(
													"&e" + player.getName() + " &7has purchased &e" + playerShop.getQuantity()
															+ " " + ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase()))
															+ " &7for &e$" + playerShop.getBuyPrice());
								}
							}
						} else if (sign.getSide(Side.FRONT).getLine(3).startsWith("Sell")) {
							if (shopUser.getBalance() >= playerShop.getSellPrice()) {
								clickUser.setBalance(clickUser.getBalance() + playerShop.getSellPrice());
								shopUser.setBalance(shopUser.getBalance() - playerShop.getSellPrice());
								player.sendMessage(ChatUtils.chatMessage(
										"&7You have sold &e" + playerShop.getQuantity()
												+ " " + ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())));
								// If the shop owner is online
								if (Bukkit.getPlayer(playerShop.getUuid()) != null) {
									Player shopPlayer = Bukkit.getPlayer(playerShop.getUuid());
									shopPlayer.sendMessage(
											ChatUtils.chatMessage(
													"&e" + player.getName() + " &7has sold &e" + playerShop.getQuantity()
															+ " " + ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase()))
															+ " &7for &e$" + playerShop.getSellPrice());
								}
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with this shop..."));
						}
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
