package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handles the interacting with a player shop.
 */
public class ShopInteract {

	public void execute(PlayerInteractEvent e) {
		// Left or right-clicking the sign, including placing and breaking
		if (e.getClickedBlock().getType().name().endsWith("_SIGN")) {
			Player player = e.getPlayer();
			AranarthPlayer clickUser = AranarthUtils.getPlayer(player.getUniqueId());
			Location signLocation = e.getClickedBlock().getLocation();
			Location locationBelow = new Location(signLocation.getWorld(),
					signLocation.getBlockX(), signLocation.getBlockY() - 1, signLocation.getBlockZ());
			Shop shop = ShopUtils.getShopFromLocation(signLocation);

			// Player shop
			if (isChest(locationBelow.getBlock().getType()) && shop != null && AranarthUtils.getPlayer(shop.getUuid()) != null) {
				if (shop != null) {
					e.setCancelled(true);

					AranarthPlayer shopUser = AranarthUtils.getPlayer(shop.getUuid());

					if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
						// If editing your own shop
						if (shop.getUuid().equals(player.getUniqueId())) {
							e.setCancelled(false);
							return;
						}

						// Enables bulk mode for the purchase
						if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
							shop = ShopUtils.getBulkShop(shop, player, true);
						}
						// The user is just toggling the bulk purchase mode
						else if (clickUser.getBulkTransactionNum() == 0 && player.isSneaking()) {
							return;
						}

						handleBuyLogic(player, clickUser, shopUser, shop, locationBelow);
					} else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
						// If editing your own shop
						if (shop.getUuid().equals(player.getUniqueId())) {
							e.setCancelled(false);
							return;
						}

						// Enables bulk mode for the sale
						if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
							shop = ShopUtils.getBulkShop(shop, player, false);
						}
						// The user is just toggling the bulk sale mode
						else if (clickUser.getBulkTransactionNum() == 0 && player.isSneaking()) {
							return;
						}

						handleSellLogic(e, player, clickUser, shopUser, shop, locationBelow);
					}
				} else {
					if (player.isSneaking()) {
						if (e.getClickedBlock().getState() instanceof Sign sign) {
							player.openSign(sign);
						}
					}
				}
			}
			// Server shop
			// If the clicked block is a sign but the block below is not a chest
			else {
				if (shop != null) {
					if (shop.getUuid() == null) {
						e.setCancelled(true);
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

						if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
							// If editing a server shop
							if (aranarthPlayer.getCouncilRank() == 3 && player.isSneaking()) {
								if (e.getClickedBlock().getState() instanceof Sign sign) {
									player.openSign(sign);
								}
								return;
							}

							// Enables bulk mode for the purchase
							if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
								shop = ShopUtils.getBulkShop(shop, player, true);
							}
							// The user is just toggling the bulk purchase mode
							else if (clickUser.getBulkTransactionNum() == 0 && player.isSneaking()) {
								return;
							}

							handleBuyLogic(player, clickUser, null, shop, null);
						} else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
							// Enables bulk mode for the sale
							if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
								shop = ShopUtils.getBulkShop(shop, player, false);
							}
							// The user is just toggling the bulk sale mode
							else if (clickUser.getBulkTransactionNum() == 0 && player.isSneaking()) {
								return;
							}
							if (player.getGameMode() != GameMode.CREATIVE) {
								handleSellLogic(e, player, clickUser, null, shop, null);
							} else {
								e.setCancelled(false);
								ShopUtils.removeShop(shop);
								player.sendMessage(ChatUtils.chatMessage("&7You have destroyed this shop"));
								player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 0.1F);
							}
						}
					}
				} else {
					if (player.isSneaking()) {
						if (e.getClickedBlock().getState() instanceof Sign sign) {
							player.openSign(sign);
						}
					}
				}
			}
		}
		// Left or right-clicking the chest, including opening and breaking
		else if (isChest(e.getClickedBlock().getType())) {
			// Gets both locations if it is a double chest
			BlockState state = e.getClickedBlock().getState();
			Container container = (Container) state;
			Location[] locations = new Location[2];
			if (container.getInventory().getHolder() instanceof DoubleChest doubleChest) {
				Chest leftChest = (Chest) doubleChest.getLeftSide();
				Chest rightChest = (Chest) doubleChest.getRightSide();
				locations[0] = leftChest.getLocation();
				locations[1] = rightChest.getLocation();
			} else {
				locations[0] = e.getClickedBlock().getLocation();
			}

			Shop location1Shop = ShopUtils.getShopFromLocation(locations[0].getBlock().getRelative(BlockFace.UP).getLocation());
			Shop location2Shop = null;
			if (ShopUtils.getShopFromLocation(locations[1]) != null) {
				location2Shop = ShopUtils.getShopFromLocation(locations[1].getBlock().getRelative(BlockFace.UP).getLocation());
			}

			if (location1Shop != null || (locations[1] != null && location2Shop != null)) {
				if (!location1Shop.getUuid().equals(e.getPlayer().getUniqueId()) || (location2Shop != null && !location2Shop.getUuid().equals(e.getPlayer().getUniqueId()))) {

					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
					if (!aranarthPlayer.getIsInAdminMode()) {
						// Prevents other players from destroying or opening the chest
						e.setCancelled(true);
						if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
							e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's shop!"));
						} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
							e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot open someone else's player shop chest!"));
						}
					}
				}
			}
		}
	}

	/**
	 * Handles all logic involving buying from a shop.
	 * @param player The player buying from the shop.
	 * @param clickUser The user that clicked the shop sign.
	 * @param shopUser The user that owns the shop.
	 * @param shop The shop.
	 * @param locationBelow The location below the shop sign.
	 */
	private void handleBuyLogic(Player player, AranarthPlayer clickUser, AranarthPlayer shopUser, Shop shop, Location locationBelow) {
		// Buy and edit logic
		if (shop.getBuyPrice() > 0) {
			if (clickUser.getBalance() >= shop.getBuyPrice()) {
				boolean isPlayerShop = locationBelow != null;
				Inventory chestInventory = null;
				if (isPlayerShop) {
					BlockState state = locationBelow.getBlock().getState();
					Container container = (Container) state;
					chestInventory = container.getInventory();
					if (chestInventory.getHolder() instanceof DoubleChest doubleChest) {
						chestInventory = doubleChest.getInventory(); // Get the full 54 slot inventory
					}
				}

				if (isPlayerShop) {
					// Verifies that there is enough quantity in the chest's inventory
					// Cycles through the chest's inventory starting from end to beginning
					HashMap<Boolean, ItemStack[]> result = checkIfContentsHasShopItems(chestInventory.getContents(), shop);
					if (result.containsKey(true)) {
						chestInventory.clear();
						chestInventory.setContents(result.get(true));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThere is not enough inventory in this shop!"));
						return;
					}
				}

				// Verifies there is enough space in the player's inventory to add the items
				int spaceForShopItemInPlayerInventory = 0;
				for (ItemStack inventoryItem : player.getInventory().getStorageContents()) {
					if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
						spaceForShopItemInPlayerInventory += 64;
						continue;
					}

					if (inventoryItem.isSimilar(shop.getItem())) {
						spaceForShopItemInPlayerInventory += inventoryItem.getMaxStackSize() - inventoryItem.getAmount();
					}
				}
				if (spaceForShopItemInPlayerInventory < shop.getQuantity()) {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough space for this!"));
					clickUser.setBulkTransactionNum(-1);
					AranarthUtils.setPlayer(player.getUniqueId(), clickUser);
					return;
				}

				DecimalFormat df = new DecimalFormat("0.00");

				// Logic to update balances and chest inventory
				clickUser.setBalance(clickUser.getBalance() - shop.getBuyPrice());
				if (shopUser != null) {
					shopUser.setBalance(shopUser.getBalance() + shop.getBuyPrice());
				}

				// Logic to add items to player's inventory
				ItemStack itemToAdd = shop.getItem().clone();
				itemToAdd.setAmount(shop.getQuantity());
				HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(itemToAdd);
				for (Integer index : remainder.keySet()) {
					player.getWorld().dropItemNaturally(player.getLocation(), remainder.get(index));
				}
				String itemname = ChatUtils.getFormattedItemName(shop.getItem().getType().name());
				if (shop.getItem().hasItemMeta()) {
					if (shop.getItem().getItemMeta().hasDisplayName()) {
						itemname = shop.getItem().getItemMeta().getDisplayName();
					}
				}

				player.sendMessage(ChatUtils.chatMessage(
						"&7You have purchased &e" + shop.getQuantity() + " " + itemname
								+ ChatUtils.translateToColor(" &7for &6$" + df.format(shop.getBuyPrice()))));
				clickUser.setBulkTransactionNum(-1);
				AranarthUtils.setPlayer(player.getUniqueId(), clickUser);

				if (!remainder.isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&e" + remainder.size() + " " + itemname
							+ ChatUtils.translateToColor(" &7was dropped on the ground!")));
				}

				// If the shop owner is online
				if (isPlayerShop) {
					if (Bukkit.getPlayer(shop.getUuid()) != null) {
						Player shopPlayer = Bukkit.getPlayer(shop.getUuid());
						shopPlayer.sendMessage(
								ChatUtils.chatMessage("&e" + player.getName() + " &7has purchased &e" + shop.getQuantity() + " "
										+ itemname + ChatUtils.translateToColor(" &7for &6$" + df.format(shop.getBuyPrice()))));
					}
				}
			} else {
				if (clickUser.getBulkTransactionNum() <= 0 && player.isSneaking()) {
					return;
				}
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough money to buy this!"));
			}
		}
	}

	/**
	 * Handles all logic involving selling to a shop.
	 * @param e The event.
	 * @param player The player selling the shop.
	 * @param clickUser The user that clicked the shop sign.
	 * @param shopUser The user that owns the shop.
	 * @param shop The shop.
	 * @param locationBelow The location below the shop sign.
	 */
	private void handleSellLogic(PlayerInteractEvent e, Player player, AranarthPlayer clickUser, AranarthPlayer shopUser, Shop shop, Location locationBelow) {
		if (shop.getSellPrice() > 0) {
			boolean isPlayerShop = locationBelow != null;

			if (isPlayerShop) {
				if (shopUser.getBalance() < shop.getSellPrice()) {
					player.sendMessage(ChatUtils.chatMessage("&cThis player does not have enough money to sell this!"));
					return;
				}
			}

			Inventory chestInventory = null;
			Container copyOfChest = null;

			if (isPlayerShop) {
				BlockState state = locationBelow.getBlock().getState();
				Container container = (Container) state;
				copyOfChest = (Container) container.copy();
				chestInventory = container.getInventory();
				if (chestInventory.getHolder() instanceof DoubleChest doubleChest) {
					chestInventory = doubleChest.getInventory(); // Get the full 54 slot inventory
				}

				// Verifies there is enough space in the chest's inventory to add the items
				int spaceForShopItemInChestInventory = 0;
				for (ItemStack chestItem : copyOfChest.getInventory().getStorageContents()) {
					if (chestItem == null || chestItem.getType() == Material.AIR) {
						spaceForShopItemInChestInventory += 64;
						continue;
					}

					if (chestItem.isSimilar(shop.getItem())) {
						spaceForShopItemInChestInventory += chestItem.getMaxStackSize() - chestItem.getAmount();
					}
				}
				if (spaceForShopItemInChestInventory < shop.getQuantity()) {
					player.sendMessage(ChatUtils.chatMessage("&cThere is no space remaining in the chest!"));
					clickUser.setBulkTransactionNum(-1);
					AranarthUtils.setPlayer(player.getUniqueId(), clickUser);
					return;
				}
			}

			// Verifies the player has the items
			Inventory playerInventory = player.getInventory();
			HashMap<Boolean, ItemStack[]> result = checkIfContentsHasShopItems(playerInventory.getContents(), shop);

			if (result.containsKey(true)) {
				DecimalFormat df = new DecimalFormat("0.00");

				// Logic to update balances and chest inventory
				clickUser.setBalance(clickUser.getBalance() + shop.getSellPrice());
				if (shopUser != null) {
					shopUser.setBalance(shopUser.getBalance() - shop.getSellPrice());
					ItemStack shopItem = shop.getItem().clone();
					shopItem.setAmount(shop.getQuantity());
					chestInventory.addItem(shopItem);
				}

				// Logic to remove items from the player's inventory
				playerInventory.clear();
				playerInventory.setContents(result.get(true));

				String itemname = ChatUtils.getFormattedItemName(shop.getItem().getType().name());
				if (shop.getItem().hasItemMeta()) {
					if (shop.getItem().getItemMeta().hasDisplayName()) {
						itemname = shop.getItem().getItemMeta().getDisplayName();
					}
				}

				player.sendMessage(ChatUtils.chatMessage(
						"&7You have sold &e" + shop.getQuantity() + " " + itemname
						+ ChatUtils.translateToColor(" &7for &6$" + df.format(shop.getSellPrice()))));
				clickUser.setBulkTransactionNum(-1);
				AranarthUtils.setPlayer(player.getUniqueId(), clickUser);

				// If the shop owner is online
				if (isPlayerShop) {
					if (Bukkit.getPlayer(shop.getUuid()) != null) {
						Player shopPlayer = Bukkit.getPlayer(shop.getUuid());
						shopPlayer.sendMessage(
								ChatUtils.chatMessage("&e" + player.getName() + " &7has sold you &e" + shop.getQuantity() + " "
										+  itemname + ChatUtils.translateToColor(" &7for &6$" + df.format(shop.getSellPrice()))));
					}
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough of this item!"));
			}
		} else {
			if (clickUser.getBulkTransactionNum() == 1 && player.isSneaking()) {
				return;
			}
			if (shop.getUuid() != null) {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's shop!"));
			} else {
				if (AranarthUtils.getPlayer(player.getUniqueId()).getCouncilRank() < 3) {
					player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy a server shop!"));
				}
			}
		}
	}

	/**
	 * Verifies if the contents contains the full amount needed from the shop.
	 * @param inventory The contents to be verified.
	 * @param playerShop The player shop being interacted with.
	 * @return Confirmation if the contents contain the full amount from the shop.
	 */
	private HashMap<Boolean, ItemStack[]> checkIfContentsHasShopItems(ItemStack[] inventory, Shop playerShop) {
		boolean hasInventory = false;
		int summedQuantityOfItem = 0;
		ArrayList<Integer> indexesWithItem = new ArrayList<>();

		// Avoids reference errors
		List<ItemStack> items = new ArrayList<>();
		for (ItemStack is : inventory) {
			if (is == null) {
				items.add(null);
				continue;
			} else {
				ItemStack clone = is.clone();
				items.add(clone);
			}
		}
		ItemStack[] contents = items.toArray(new ItemStack[0]);

		for (int i = contents.length - 1; i >= 0; i--) {
			if (contents[i] != null && contents[i].isSimilar(playerShop.getItem())) {
				// If the first single slot of the item in the chest contains enough inventory
				if (contents[i].getAmount() >= playerShop.getQuantity() && summedQuantityOfItem == 0) {
					int newAmount = contents[i].getAmount() - playerShop.getQuantity();

					// Updates the chest's inventory
					contents[i].setAmount(newAmount);
					hasInventory = true;
					break;
				}
				// If more than one slot is needed
				else {
					// If the combined amount of slots has enough
					if (summedQuantityOfItem + contents[i].getAmount() >= playerShop.getQuantity()) {
						// Clears accumulated slots of the chest
						for (Integer index : indexesWithItem) {
							contents[index] = null;
						}
						int newAmount = contents[i].getAmount() - (playerShop.getQuantity() - summedQuantityOfItem);
						contents[i].setAmount(newAmount);
						hasInventory = true;
						break;
					} else {
						summedQuantityOfItem += contents[i].getAmount();
						indexesWithItem.add(i);
					}
				}
			}
		}
		HashMap<Boolean, ItemStack[]> results = new HashMap<>();
		results.put(hasInventory, contents);
		return results;
	}


	/**
	 * Determines if the clicked block is a chest OR A trapped chest.
	 * @param type The type of material.
	 * @return Confirmation of whether the block is a chest OR A trapped chest.
	 */
	private boolean isChest(Material type) {
		return type == Material.CHEST || type == Material.TRAPPED_CHEST;
	}


}
