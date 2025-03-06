package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

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
		if (e.getClickedBlock() != null && isSign(e.getClickedBlock().getType())) {
			Location signLocation = e.getClickedBlock().getLocation();
			Location locationBelow = new Location(signLocation.getWorld(),
					signLocation.getBlockX(), signLocation.getBlockY() - 1, signLocation.getBlockZ());
			if (isChest(locationBelow.getBlock().getType())) {
				PlayerShop playerShop = AranarthUtils.getShop(signLocation);
				if (playerShop != null) {
					if (!playerShop.getUuid().toString().equals(e.getPlayer().getUniqueId().toString())) {
						e.setCancelled(true);

						Player player = e.getPlayer();
						AranarthPlayer clickUser = AranarthUtils.getPlayer(player.getUniqueId());
						AranarthPlayer shopUser = AranarthUtils.getPlayer(playerShop.getUuid());

						// Handles logic to buy/sell
						if (playerShop.getBuyPrice() > 0 && playerShop.getSellPrice() == 0) {
							if (clickUser.getBalance() >= playerShop.getBuyPrice()) {
								BlockState state = locationBelow.getBlock().getState();
								Container container = (Container) state;
								Inventory inventory = container.getInventory();
								if (inventory.getHolder() instanceof DoubleChest doubleChest) {
									inventory = doubleChest.getInventory(); // Get the full 54 slot inventory
								}
								ItemStack[] contents = inventory.getContents();
								boolean hasEnoughQuantity = false;
								int summedQuantityOfItem = 0;
								ArrayList<Integer> indexesWithItem = new ArrayList<>();
								// Cycles through the chest's inventory starting from end to beginning
								// If a given stack does not have enough, it will sum it with the next stack
								for (int i = contents.length; i > 0; i--) {
									if (contents[i].isSimilar(playerShop.getItem())) {
										// If the slot in the chest contains enough inventory
										if (contents[i].getAmount() >= playerShop.getQuantity()) {
											int newAmount = contents[i].getAmount() - playerShop.getQuantity();
											contents[i].setAmount(newAmount);
											hasEnoughQuantity = true;
											inventory.clear();
											inventory.setContents(contents);
										}
										// If more than one slot is needed
										else {
											if (summedQuantityOfItem + contents[i].getAmount() >= playerShop.getQuantity()) {
												for (Integer index : indexesWithItem) {
													contents[index] = null;
												}
												int newAmount = contents[i].getAmount() - (playerShop.getQuantity() - summedQuantityOfItem);
												contents[i].setAmount(newAmount);
												hasEnoughQuantity = true;
												inventory.clear();
												inventory.setContents(contents);
											} else {
												summedQuantityOfItem += contents[i].getAmount();
												indexesWithItem.add(i);
											}
										}
									}
								}

								if (hasEnoughQuantity) {
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
								} else {
									player.sendMessage(ChatUtils.chatMessage("&cThere is not enough inventory in this shop!"));
								}


							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough money to buy this!"));
							}
						} else if (playerShop.getSellPrice() > 0 && playerShop.getBuyPrice() == 0) {
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
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cThis player does not have enough money to sell this!"));
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with this shop..."));
						}
					}
				}
			}
		} else if (e.getClickedBlock() != null && isChest(e.getClickedBlock().getType())) {
			PlayerShop playerShop = AranarthUtils.getShop(e.getClickedBlock().getLocation());
			if (playerShop != null) {
				Player player = e.getPlayer();
				AranarthPlayer shopAranarthPlayer = AranarthUtils.getPlayer(playerShop.getUuid());
				AranarthPlayer clickedAranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				if (!shopAranarthPlayer.getUsername().equals(clickedAranarthPlayer.getUsername())) {
					// Prevents other players from destroying or opening the chest
					e.setCancelled(true);
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
