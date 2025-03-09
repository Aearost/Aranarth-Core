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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

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
								Inventory chestInventory = container.getInventory();
								if (chestInventory.getHolder() instanceof DoubleChest doubleChest) {
									chestInventory = doubleChest.getInventory(); // Get the full 54 slot inventory
								}
								ItemStack[] chestContents = chestInventory.getContents();
								boolean chestHasInventory = false;
								int summedQuantityOfItem = 0;

								ArrayList<Integer> indexesWithItem = new ArrayList<>();

								// Verifies that there is enough quantity in the chest's inventory
								// Cycles through the chest's inventory starting from end to beginning
								for (int i = chestContents.length - 1; i > 0; i--) {
									if (chestContents[i] != null && chestContents[i].isSimilar(playerShop.getItem())) {
										// If the first single slot in the chest contains enough inventory
										if (chestContents[i].getAmount() >= playerShop.getQuantity() && summedQuantityOfItem == 0) {
											int newAmount = chestContents[i].getAmount() - playerShop.getQuantity();

											// Updates the chest's inventory
											chestContents[i].setAmount(newAmount);
											chestHasInventory = true;
											break;
										}
										// If more than one slot is needed
										else {
											// If the combined amount of slots has enough
											if (summedQuantityOfItem + chestContents[i].getAmount() >= playerShop.getQuantity()) {

												// Clears accumulated slots of the chest
												for (Integer index : indexesWithItem) {
													chestContents[index] = null;
												}
												int newAmount = chestContents[i].getAmount() - (playerShop.getQuantity() - summedQuantityOfItem);
												chestContents[i].setAmount(newAmount);
												chestHasInventory = true;
												break;
											} else {
												summedQuantityOfItem += chestContents[i].getAmount();
												indexesWithItem.add(i);
											}
										}
									}
								}

								if (chestHasInventory) {
									DecimalFormat df = new DecimalFormat("0.00");

									// Logic to update balances and chest inventory
									clickUser.setBalance(clickUser.getBalance() - playerShop.getBuyPrice());
									shopUser.setBalance(shopUser.getBalance() + playerShop.getBuyPrice());
									chestInventory.clear();
									chestInventory.setContents(chestContents);

									// Logic to add items to player's inventory
									ItemStack itemToAdd = playerShop.getItem().clone();
									itemToAdd.setAmount(playerShop.getQuantity());
									HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(itemToAdd);
									for (Integer index : remainder.keySet()) {
										player.getWorld().dropItemNaturally(player.getLocation(), remainder.get(index));
									}

									player.sendMessage(ChatUtils.chatMessage(
											"&7You have purchased &e" + playerShop.getQuantity()
													+ " " + ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())));
									if (!remainder.isEmpty()) {
										player.sendMessage(ChatUtils.chatMessage("&e" + remainder.size() + " "
												+ ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())
												+ ChatUtils.translateToColor(" &7was dropped on the ground!")));
									}

									// If the shop owner is online
									if (Bukkit.getPlayer(playerShop.getUuid()) != null) {
										Player shopPlayer = Bukkit.getPlayer(playerShop.getUuid());
										shopPlayer.sendMessage(
												ChatUtils.chatMessage("&e" + player.getName() + " &7has purchased &e" + playerShop.getQuantity() + " "
														+ ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())
														+ ChatUtils.translateToColor(" &7for &6$" + df.format(playerShop.getBuyPrice()))));
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
