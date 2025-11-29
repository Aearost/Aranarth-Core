package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;

public class PlayerShopCreateListener implements Listener {

	public PlayerShopCreateListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the creation or updating of a player shop.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerShopCreate(final SignChangeEvent e) {
		String[] lines = e.getLines();
		Player player = e.getPlayer();
		Block sign = e.getBlock();

		// Only allow shop creation in Survival worlds
		if (!player.getWorld().getName().startsWith("world") && !player.getWorld().getName().startsWith("smp")) {
			player.sendMessage(ChatUtils.chatMessage("&cYou cannot create a shop here!"));
			return;
		}

		// If placing a Player Shop
		if (ChatUtils.stripColorFormatting(lines[0]).equals("[Shop]")) {
			int[] validSignFormatResult = validSignFormat(lines, player, true);
			// If all the lines were entered correctly
			if (validSignFormatResult[1] == 0 && validSignFormatResult[2] == 0 && validSignFormatResult[3] == 0) {
				// Verifies there is a sign on top, a chest underneath, and at least one item in the first slot of the chest
				if (isValidChestFormat(player, sign)) {
					String[] priceParts = ChatUtils.stripColorFormatting(lines[2]).split(" ");
					// Price check
					if (priceParts[0].equalsIgnoreCase("B")) {
						// Only buying
						if (priceParts.length == 2) {
							ShopUtils.createOrUpdateShop(e, player, getShopItem(sign), getShopQuantity(lines[1]), getDecimalShopPrice(priceParts[1]), 0);
						}
						// Both buying and selling
						else if (priceParts.length == 5) {
							ShopUtils.createOrUpdateShop(e, player, getShopItem(sign), getShopQuantity(lines[1]), getDecimalShopPrice(priceParts[1]), getDecimalShopPrice(priceParts[4]));
						}
					} else {
						ShopUtils.createOrUpdateShop(e, player, getShopItem(sign), getShopQuantity(lines[1]), 0, getDecimalShopPrice(priceParts[1]));
					}
				} else {
					// Forceful display of the shop in error
					displayInvalidFields(e, new int[] { 1, 0, 0, 0}, true);
				}
			} else {
				displayInvalidFields(e, validSignFormatResult, true);
			}
		}
		// If placing a Server Shop
		else if (ChatUtils.stripColorFormatting(lines[0]).equals("[Server Shop]")) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() == 3) {
				int[] validSignFormatResult = validSignFormat(lines, player, false);
				// If all the lines were entered correctly
				if (validSignFormatResult[1] == 0 && validSignFormatResult[2] == 0 && validSignFormatResult[3] == 0) {
					ItemStack heldItem = player.getInventory().getItemInMainHand();
					if (heldItem != null && heldItem.getType() != Material.AIR) {
						String[] priceParts = ChatUtils.stripColorFormatting(lines[2]).split(" ");
						// Price check
						if (priceParts[0].equalsIgnoreCase("B")) {
							// Only buying
							if (priceParts.length == 2) {
								ShopUtils.createOrUpdateShop(e, null, heldItem, getShopQuantity(lines[1]), getDecimalShopPrice(priceParts[1]), 0);
							}
							// Both buying and selling
							else if (priceParts.length == 5) {
								ShopUtils.createOrUpdateShop(e, null, heldItem, getShopQuantity(lines[1]), getDecimalShopPrice(priceParts[1]), getDecimalShopPrice(priceParts[4]));
							}
						} else {
							ShopUtils.createOrUpdateShop(e, null, heldItem, getShopQuantity(lines[1]), 0, getDecimalShopPrice(priceParts[1]));
						}
					} else {
						// Forceful display of the shop in error
						displayInvalidFields(e, new int[] { 1, 0, 0, 0}, false);
						player.sendMessage(ChatUtils.chatMessage("&cYou are not holding an item!"));
					}
				} else {
					displayInvalidFields(e, validSignFormatResult, false);
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot create a server shop!"));
				e.setLine(1, "");
				e.setLine(2, "");
				e.setLine(3, "");
			}
		}
		else {
			// Remove if the shop previously existed and now was changed
			if (ShopUtils.getShopFromLocation(e.getBlock().getLocation()) != null) {
				ShopUtils.removeShop(player.getUniqueId(), e.getBlock().getLocation());
				player.sendMessage(ChatUtils.chatMessage("&7You have destroyed this shop"));
				e.setLine(0, ChatUtils.stripColorFormatting(e.getLine(0)));
				e.setLine(1, ChatUtils.stripColorFormatting(e.getLine(1)));
				e.setLine(2, ChatUtils.stripColorFormatting(e.getLine(2)));
				e.setLine(3, ChatUtils.stripColorFormatting(e.getLine(3)));
			}
		}
	}

	/**
	 * Validates that the format of the shop sign is correct.
	 * @param lines The lines of the sign.
	 * @param player The player making the change to the sign.
	 * @param isPlayerShop Whether it is a player or server shop.
	 * @return Confirmation whether the sign is following the valid format or not.
	 */
	private int[] validSignFormat(String[] lines, Player player, boolean isPlayerShop) {
		int[] invalidLines = new int[] { 0, 0, 0, 0 };

		// Quantity check
		int shopQuantityResult = getShopQuantity(ChatUtils.stripColorFormatting(lines[1]));
		if (shopQuantityResult == 0) {
			player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid quantity!"));
			invalidLines[1] = 1;
		} else if (shopQuantityResult == -1) {
			player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax for quantity!"));
			invalidLines[1] = 1;
		}

		String[] priceParts = ChatUtils.stripColorFormatting(lines[2]).split(" ");
		String incorrectPrice = "&cIncorrect syntax for the price!";
		// Price check
		if (priceParts[0].equalsIgnoreCase("B")) {
			// Only buying
			if (priceParts.length == 2) {
				if (getDecimalShopPrice(priceParts[1]) == 0) {
					player.sendMessage(ChatUtils.chatMessage(incorrectPrice));
					invalidLines[2] = 1;
				}
			}
			// Both buying and selling
			else if (priceParts.length == 5) {
				// Verify buy price
				if (getDecimalShopPrice(priceParts[1]) == 0) {
					player.sendMessage(ChatUtils.chatMessage(incorrectPrice));
					invalidLines[2] = 1;
				}

				// Verify separator character
				if (!priceParts[2].equals("|")) {
					player.sendMessage(ChatUtils.chatMessage(incorrectPrice));
					invalidLines[2] = 1;
				}

				// Verify keyword to Sell
				if (!priceParts[3].equalsIgnoreCase("S")) {
					player.sendMessage(ChatUtils.chatMessage(incorrectPrice));
					invalidLines[2] = 1;
				}

				// Verify sell price
				if (getDecimalShopPrice(priceParts[4]) == 0) {
					player.sendMessage(ChatUtils.chatMessage(incorrectPrice));
					invalidLines[2] = 1;
				}
			}
			else {
				player.sendMessage(ChatUtils.chatMessage(incorrectPrice));
				invalidLines[2] = 1;
			}
		} else if (priceParts[0].equalsIgnoreCase("S")) {
			// Only selling
			if (getDecimalShopPrice(priceParts[1]) == 0) {
				player.sendMessage(ChatUtils.chatMessage(incorrectPrice));
				invalidLines[2] = 1;
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage(incorrectPrice));
			invalidLines[2] = 1;
		}

		// Username check
		if (isPlayerShop) {
			if (!ChatUtils.stripColorFormatting(lines[3]).equalsIgnoreCase(player.getName())) {
				player.sendMessage(ChatUtils.chatMessage("&cThat is not your username!"));
				invalidLines[3] = 1;
			}
		}

		return invalidLines;
    }

	/**
	 * Provides the quantity of the shop based on the content of the sign.
	 * Returns -1 if something is wrong with the syntax.
	 * Returns 0 if there is an incorrect value entered as the number.
	 * Returns >0 based on the quantity entered.
	 *
	 * @param line The line for the shop quantity.
	 * @return The quantity of the shop item.
	 */
	private int getShopQuantity(String line) {
		String[] quantityLineParts = line.split(" ");
		if (quantityLineParts.length != 2) {
			return -1;
		}

		if (ChatUtils.stripColorFormatting(quantityLineParts[0]).equalsIgnoreCase("qty")) {
			int quantity = 0;
			try {
				quantity = Integer.parseInt(quantityLineParts[1]);
				if (quantity <= 0) {
					throw new NumberFormatException();
				}
				return quantity;
			} catch (NumberFormatException ex) {
				return 0;
			}
		} else {
			return -1;
		}
	}

	/**
	 * Provides the price of the shop based on the content of the sign.
	 * @param price The line for the shop price.
	 * @return The price of the shop item.
	 */
	private double getDecimalShopPrice(String price) {
		try {
			double priceAsDouble = Double.parseDouble(price);
			if (priceAsDouble <= 0) {
				throw new NumberFormatException();
			}
			DecimalFormat df = new DecimalFormat("0.00");
			return Double.parseDouble(df.format(priceAsDouble));
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	/**
	 * Validates whether the chest format is correct for a player shop.
	 * @param player The player who is making the change to the sign.
	 * @param sign The sign of the shop.
	 * @return Confirmation whether the chest is correctly placed for the shop.
	 */
	private boolean isValidChestFormat(Player player, Block sign) {
		if (!isBlockBelowChest(sign)) {
			player.sendMessage(ChatUtils.chatMessage("&cYou do not have a chest for the shop!"));
			return false;
		}

		if (getShopItem(sign) == null) {
			player.sendMessage(ChatUtils.chatMessage("&cThe first slot of the chest must contain an item!"));
			return false;
		}

		// Will confirm only if all checks are valid
		return true;
	}

	/**
	 * Determines if the block below is a chest, trapped chest, or barrel.
	 * @param sign The sign above the block.
	 * @return Confirmation of whether the block is a chest, trapped chest, or barrel.
	 */
	private boolean isBlockBelowChest(Block sign) {
		Location signLocation = sign.getLocation();
		Location blockBelowLocation = new Location(signLocation.getWorld(),
				signLocation.getBlockX(), signLocation.getBlockY() - 1, signLocation.getBlockZ());

		return signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.CHEST
				|| signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.BARREL
				|| signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.TRAPPED_CHEST;
	}

	/**
	 * Provides a single quantity of the item of the shop.
	 * @param sign The sign of the shop.
	 * @return The single quantity of the item for the shop.
	 */
	private ItemStack getShopItem(Block sign) {
		Location signLocation = sign.getLocation();
		Location blockBelowSign = new Location(signLocation.getWorld(),
				signLocation.getBlockX(), signLocation.getBlockY() - 1, signLocation.getBlockZ());

		// Gets the first item in the chest and uses this as the item
		BlockState state = blockBelowSign.getBlock().getState();
		Container container = (Container) state;
		ItemStack firstItem = container.getInventory().getContents()[0];
		if (firstItem != null) {
			ItemStack shopItem = firstItem.clone();
			shopItem.setAmount(1);
			return shopItem;
		} else {
			return null;
		}
	}

	/**
	 * Handles the logic to highlight the parts of the lines of the sign if incorrect input is provided.
	 * @param e The event.
	 * @param incorrectLines The lines number that were entered incorrectly that will be highlighted.
	 * @param isPlayerShop Confirmation whether the sign is for a player or server shop.
	 */
	private void displayInvalidFields(SignChangeEvent e, int[] incorrectLines, boolean isPlayerShop) {
		if (isPlayerShop) {
			e.setLine(0, ChatUtils.translateToColor("&4&l[Shop]"));
		} else {
			e.setLine(0, ChatUtils.translateToColor("&4&l[Server Shop]"));
		}

		if (incorrectLines[1] == 1) {
			e.setLine(1, ChatUtils.translateToColor("&4&l" + ChatUtils.stripColorFormatting(e.getLine(1))));
		}
		if (incorrectLines[2] == 1) {
			e.setLine(2, ChatUtils.translateToColor("&4&l" + ChatUtils.stripColorFormatting(e.getLine(2))));
		}
		if (incorrectLines[3] == 1) {
			e.setLine(3, ChatUtils.translateToColor("&4&l" + ChatUtils.stripColorFormatting(e.getLine(3))));
		}
	}

}
