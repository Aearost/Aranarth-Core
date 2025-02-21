package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerShopCreate implements Listener {

	public PlayerShopCreate(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the creation of a player shop.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerShopCreate(final SignChangeEvent e) {
		for (int i = 0; i < e.getLines().length; i++) {

//			[Shop]
//			Aearost
//			QTY 15
//			Buy 10

			if (e.getLines()[i].equals(ChatUtils.stripColorFormatting("[Shop]"))) {
				// Editing an existing shop
				if (AranarthUtils.isShop(e.getBlock().getLocation())) {
					PlayerShop shop = AranarthUtils.getShop(e.getBlock().getLocation());
					Player player = e.getPlayer();
					if (player.getName().equals(e.getLines()[1])) {
						String[] line3Parts = e.getLines()[2].split(" ");
						if (line3Parts[0].equals("QTY")) {
							int quantity = 0;
							try {
								quantity = Integer.parseInt(line3Parts[1]);
								if (quantity <= 0) {
									throw new NumberFormatException();
								}
							} catch (NumberFormatException ex) {
								player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid quantity!"));
								clearLines(e);
							}

							String[] line4Parts = e.getLines()[3].split(" ");
							if (line4Parts[0].equals("Buy")) {
								int buyPrice = 0;
								try {
									buyPrice = Integer.parseInt(line4Parts[1]);
									if (buyPrice <= 0) {
										throw new NumberFormatException();
									}
									DecimalFormat df = new DecimalFormat("0.00");
									buyPrice = Integer.parseInt(df.format(buyPrice));
								} catch (NumberFormatException ex) {
									player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid quantity!"));
									clearLines(e);
								}
								Location signLocation = e.getBlock().getLocation();
								Location blockBelowLocation = new Location(signLocation.getWorld(), signLocation.getBlockX(),
										signLocation.getBlockY() - 1, signLocation.getBlockZ());
								if (isBlockBelowChest(signLocation, blockBelowLocation)) {
									// Gets the first item in the chest and uses this as the item
									BlockState state = blockBelowLocation.getBlock().getState();
									Container container = (Container) state;
									ItemStack firstItem = container.getInventory().getContents()[0];
									if (firstItem != null) {
										Bukkit.getLogger().info("First slot is: " + firstItem.getType().name());
										ItemStack shopItem = firstItem.clone();
										shopItem.setAmount(1);

										// If everything is good, create a new shop
										PlayerShop newShop = new PlayerShop(player.getUniqueId(), blockBelowLocation, shopItem, quantity, buyPrice, 0);

										HashMap<UUID, List<PlayerShop>> shops = AranarthUtils.getShops();
										if (shops == null) {
											shops = new HashMap<>();
										}

										List<PlayerShop> playerShops = shops.get(player.getUniqueId());
										if (playerShops == null) {
											playerShops = new ArrayList<>();
										}

										// Updating an existing shop
										if (AranarthUtils.isShop(shop.getLocation())) {
											int index = 0;
											for (PlayerShop storedShop : playerShops) {
												if (storedShop.getLocation().getBlockX() == shop.getLocation().getBlockX()
													&& storedShop.getLocation().getBlockY() == shop.getLocation().getBlockY()
													&& storedShop.getLocation().getBlockZ() == shop.getLocation().getBlockZ()) {
													break;
												}
												index++;
											}

											playerShops.set(index, newShop);
										} else {
											playerShops.add(newShop);
										}
										e.setLine(0, ChatUtils.translateToColor("&6&l[Shop]"));
										e.setLine(1, ChatUtils.translateToColor("&0&l" + player.getName()));
										player.sendMessage(ChatUtils.chatMessage("&7You have created a new shop!"));
									}
								}
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax for Buy!"));
								clearLines(e);
							}

						} else {
							player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax for QTY!"));
							clearLines(e);
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("This is not your username!"));
						clearLines(e);
					}
				}
			}
		}
	}

	private void clearLines(SignChangeEvent e) {
		e.setLine(0, ChatUtils.translateToColor("&4&l[Shop]"));
		e.setLine(1, "");
		e.setLine(2, "");
		e.setLine(3, "");
	}

	private boolean isBlockBelowChest(Location signLocation, Location blockBelowLocation) {
		return signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.CHEST
				|| signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.BARREL
				|| signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.TRAPPED_CHEST;
	}

}
