package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Handles the interacting with a player shop.
 */
public class ShopInteract {

	public void execute(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null && e.getClickedBlock().getType().name().endsWith("_SIGN")) {
			Player player = e.getPlayer();
			AranarthPlayer clickUser = AranarthUtils.getPlayer(player.getUniqueId());
			Location signLocation = e.getClickedBlock().getLocation();
			Location locationBelow = new Location(signLocation.getWorld(),
					signLocation.getBlockX(), signLocation.getBlockY() - 1, signLocation.getBlockZ());
			Shop playerShop = ShopUtils.getShopFromLocation(signLocation);

			// Player shop
			if (isChest(locationBelow.getBlock().getType())) {
				if (playerShop != null) {
					e.setCancelled(true);
					AranarthPlayer shopUser = AranarthUtils.getPlayer(playerShop.getUuid());

					if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
						// If editing your own shop
						if (playerShop.getUuid().equals(player.getUniqueId())) {
							e.setCancelled(false);
							return;
						}

						handleBuyLogic(player, clickUser, shopUser, playerShop, locationBelow);
					} else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
						handleSellLogic(player, clickUser, shopUser, playerShop, locationBelow);
					}

				}
			}
			// Server shop
			// If the clicked block is a sign but the block below is not a chest
			else {
				if (playerShop != null) {
					if (playerShop.getUuid() == null) {
						e.setCancelled(true);
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

						if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
							// If editing a server shop
							if (aranarthPlayer.getCouncilRank() == 3 && player.isSneaking()) {
								e.setCancelled(false);
								return;
							}

							handleBuyLogic(player, clickUser, null, playerShop, null);
						} else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
							handleSellLogic(player, clickUser, null, playerShop, null);
						}
					}
				}
			}
		} else if (e.getClickedBlock() != null && isChest(e.getClickedBlock().getType())) {
			Bukkit.getLogger().info("What?");
			Shop playerShop = ShopUtils.getShopFromLocation(e.getClickedBlock().getLocation());
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

	/**
	 * Handles all logic involving buying from a shop.
	 * @param player The player buying from the shop.
	 * @param clickUser The user that clicked the shop sign.
	 * @param shopUser The user that owns the shop.
	 * @param playerShop The shop.
	 * @param locationBelow The location below the shop sign.
	 */
	private void handleBuyLogic(Player player, AranarthPlayer clickUser, AranarthPlayer shopUser, Shop playerShop, Location locationBelow) {
		// Buy and edit logic
		if (playerShop.getBuyPrice() > 0) {
			if (clickUser.getBalance() >= playerShop.getBuyPrice()) {
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
					HashMap<Boolean, ItemStack[]> result = checkIfContentsHasShopItems(chestInventory.getContents(), playerShop);
					if (result.containsKey(true)) {
						chestInventory.clear();
						chestInventory.setContents(result.get(true));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThere is not enough inventory in this shop!"));
						return;
					}
				}

				DecimalFormat df = new DecimalFormat("0.00");

				// Logic to update balances and chest inventory
				clickUser.setBalance(clickUser.getBalance() - playerShop.getBuyPrice());
				if (shopUser != null) {
					shopUser.setBalance(shopUser.getBalance() + playerShop.getBuyPrice());
				}

				// Logic to add items to player's inventory
				ItemStack itemToAdd = playerShop.getItem().clone();
				itemToAdd.setAmount(playerShop.getQuantity());
				HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(itemToAdd);
				for (Integer index : remainder.keySet()) {
					player.getWorld().dropItemNaturally(player.getLocation(), remainder.get(index));
				}

				player.sendMessage(ChatUtils.chatMessage(
						"&7You have purchased &e" + playerShop.getQuantity()
								+ " " + ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())
								+ ChatUtils.translateToColor(" &7for &6$" + df.format(playerShop.getBuyPrice()))));
				if (!remainder.isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&e" + remainder.size() + " "
							+ ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())
							+ ChatUtils.translateToColor(" &7was dropped on the ground!")));
				}

				// If the shop owner is online
				if (isPlayerShop) {
					if (Bukkit.getPlayer(playerShop.getUuid()) != null) {
						Player shopPlayer = Bukkit.getPlayer(playerShop.getUuid());
						shopPlayer.sendMessage(
								ChatUtils.chatMessage("&e" + player.getName() + " &7has purchased &e" + playerShop.getQuantity() + " "
										+ ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())
										+ ChatUtils.translateToColor(" &7for &6$" + df.format(playerShop.getBuyPrice()))));
					}
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough money to buy this!"));
			}
		}
	}

	/**
	 * Handles all logic involving selling to a shop.
	 * @param player The player selling the shop.
	 * @param clickUser The user that clicked the shop sign.
	 * @param shopUser The user that owns the shop.
	 * @param playerShop The shop.
	 * @param locationBelow The location below the shop sign.
	 */
	private void handleSellLogic(Player player, AranarthPlayer clickUser, AranarthPlayer shopUser, Shop playerShop, Location locationBelow) {
		if (playerShop.getSellPrice() > 0) {
			boolean isPlayerShop = locationBelow != null;

			if (isPlayerShop) {
				if (shopUser.getBalance() < playerShop.getSellPrice()) {
					player.sendMessage(ChatUtils.chatMessage("&cThis player does not have enough money to sell this!"));
					return;
				}
			}

			Inventory chestInventory = null;
			Container copyOfChest = null;
			boolean chestHasSpace = false;

			if (isPlayerShop) {
				BlockState state = locationBelow.getBlock().getState();
				Container container = (Container) state;
				copyOfChest = (Container) container.copy();
				chestInventory = container.getInventory();
				if (chestInventory.getHolder() instanceof DoubleChest doubleChest) {
					chestInventory = doubleChest.getInventory(); // Get the full 54 slot inventory
				}

				// Verifies that there is enough quantity in the chest's inventory
				HashMap<Integer, ItemStack> extraItems = copyOfChest.getInventory().addItem();
				if (extraItems.isEmpty()) {
					chestHasSpace = true;
				}
			}

			// Verifies the player has the items
			Inventory playerInventory = player.getInventory();
			HashMap<Boolean, ItemStack[]> result = checkIfContentsHasShopItems(playerInventory.getContents(), playerShop);

			if (result.containsKey(true)) {
				if (isPlayerShop) {
					if (!chestHasSpace) {
						player.sendMessage(ChatUtils.chatMessage("&cThere is no space remaining in the chest!"));
						return;
					}
				}

				DecimalFormat df = new DecimalFormat("0.00");

				// Logic to update balances and chest inventory
				clickUser.setBalance(clickUser.getBalance() + playerShop.getSellPrice());
				if (shopUser != null) {
					shopUser.setBalance(shopUser.getBalance() - playerShop.getSellPrice());
					ItemStack shopItem = playerShop.getItem().clone();
					shopItem.setAmount(playerShop.getQuantity());
					chestInventory.addItem(shopItem);
				}

				// Logic to remove items from the player's inventory
				playerInventory.clear();
				playerInventory.setContents(result.get(true));

				player.sendMessage(ChatUtils.chatMessage(
						"&7You have sold &e" + playerShop.getQuantity()
								+ " " + ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase()))
						+ ChatUtils.translateToColor(" &7for &6$" + df.format(playerShop.getSellPrice())));

				// If the shop owner is online
				if (isPlayerShop) {
					if (Bukkit.getPlayer(playerShop.getUuid()) != null) {
						Player shopPlayer = Bukkit.getPlayer(playerShop.getUuid());
						shopPlayer.sendMessage(
								ChatUtils.chatMessage("&e" + player.getName() + " &7has sold you &e" + playerShop.getQuantity() + " "
										+ ChatUtils.getFormattedItemName(playerShop.getItem().getType().name().toLowerCase())
										+ ChatUtils.translateToColor(" &7for &6$" + df.format(playerShop.getSellPrice()))));
					}
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough of this item!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with this shop..."));
		}
	}

	/**
	 * Verifies if the contents contains the full amount needed from the shop.
	 * @param contents The contents to be verified.
	 * @param playerShop The player shop being interacted with.
	 * @return Confirmation if the contents contain the full amount from the shop.
	 */
	private HashMap<Boolean, ItemStack[]> checkIfContentsHasShopItems(ItemStack[] contents, Shop playerShop) {
		boolean hasInventory = false;
		int summedQuantityOfItem = 0;
		ArrayList<Integer> indexesWithItem = new ArrayList<>();

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
	 * Determines if the clicked block is a chest, trapped chest, or barrel.
	 * @param type The type of material.
	 * @return Confirmation of whether the block is a chest, trapped chest, or barrel.
	 */
	private boolean isChest(Material type) {
		return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
	}


}
