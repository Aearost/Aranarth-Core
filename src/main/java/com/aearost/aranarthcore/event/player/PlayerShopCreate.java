package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
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
	 * Handles the creation or updating of a player shop.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerShopCreate(final SignChangeEvent e) {
		String[] lines = e.getLines();
		Player player = e.getPlayer();
		Block sign = e.getBlock();

		// If placing a Player Shop
		if (ChatUtils.stripColorFormatting(lines[0]).equals("[Shop]")) {
			// Verifies that the sign follows the shop format
			if (isValidSignFormat(lines, player, true)) {
				// Verifies there is a sign on top, a chest underneath, and at least one item in the first slot of the chest
				if (isValidChestFormat(player, sign)) {
					if (ChatUtils.stripColorFormatting(lines[3]).startsWith("Buy")) {
						createOrUpdateShop(e, player, getShopItem(sign), getShopQuantity(lines[2]), getShopPrice(lines[3]), 0);
					} else {
						createOrUpdateShop(e, player, getShopItem(sign), getShopQuantity(lines[2]), 0, getShopPrice(lines[3]));
					}
					return;
				}
			}
			clearLines(e, true);
		}
		// If placing a Server Shop
		else if (ChatUtils.stripColorFormatting(lines[0]).equals("[Server Shop]")) {
			if (player.getName().equals("Aearost")) {
				if (isValidSignFormat(lines, player, false)) {

                    if (ChatUtils.stripColorFormatting(lines[2]).startsWith("Buy")) {
						createOrUpdateShop(e, null, getShopItemFromLine(lines[3]), getShopQuantity(lines[1]), getShopPrice(lines[2]), 0);
					} else {
						createOrUpdateShop(e, null, getShopItemFromLine(lines[3]), getShopQuantity(lines[1]), 0, getShopPrice(lines[2]));
					}
					return;
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot create a server shop!"));
			}
			clearLines(e, false);
		}
		else {
			// Remove if the shop previously existed and now was changed
			if (AranarthUtils.getShop(e.getBlock().getLocation()) != null) {
				AranarthUtils.removeShop(player.getUniqueId(), e.getBlock().getLocation());
				player.sendMessage(ChatUtils.chatMessage("&7You have destroyed this shop"));
				e.setLine(0, ChatUtils.stripColorFormatting(e.getLine(0)));
				e.setLine(1, ChatUtils.stripColorFormatting(e.getLine(1)));
				e.setLine(2, ChatUtils.stripColorFormatting(e.getLine(2)));
				e.setLine(3, ChatUtils.stripColorFormatting(e.getLine(3)));
			}
		}
	}

	/**
	 * Verifies the format of the sign is accurate.
	 * @param lines The lines of the sign.
	 * @param player The player making the change to the sign.
	 * @param isPlayerShop Whether it is a player or server shop.
	 * @return Confirmation whether the sign is following the valid format or not.
	 */
	private boolean isValidSignFormat(String[] lines, Player player, boolean isPlayerShop) {
		if (isPlayerShop) {
			if (!ChatUtils.stripColorFormatting(lines[1]).equals(player.getName())) {
				player.sendMessage(ChatUtils.chatMessage("&cThat is not your username!"));
				return false;
			}

			int shopQuantityResult = getShopQuantity(ChatUtils.stripColorFormatting(lines[2]));
			if (shopQuantityResult == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid quantity!"));
				return false;
			} else if (shopQuantityResult == -1) {
				player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax for quantity!"));
				return false;
			}

			if (ChatUtils.stripColorFormatting(lines[3]).startsWith("Buy")) {
				if (getShopPrice(ChatUtils.stripColorFormatting(lines[3])) == 0) {
					player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid buying price!"));
					return false;
				} else if (getShopPrice(ChatUtils.stripColorFormatting(lines[3])) == -1) {
					player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax for buying price!"));
					return false;
				}
			} else if (ChatUtils.stripColorFormatting(lines[3]).startsWith("Sell")) {
				if (getShopPrice(ChatUtils.stripColorFormatting(lines[3])) == 0) {
					player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid selling price!"));
					return false;
				} else if (getShopPrice(ChatUtils.stripColorFormatting(lines[3])) == -1) {
					player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax for selling price!"));
					return false;
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou must either buy or sell here!"));
				return false;
			}
			// Will confirm only if all checks are valid
			return true;
		}
		// This is a server shop being created by Aearost
		else {
			int shopQuantityResult = getShopQuantity(ChatUtils.stripColorFormatting(lines[1]));
			if (shopQuantityResult == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid quantity!"));
				return false;
			} else if (shopQuantityResult == -1) {
				player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax for quantity!"));
				return false;
			}

			if (ChatUtils.stripColorFormatting(lines[2]).startsWith("Buy")) {
				if (getShopPrice(ChatUtils.stripColorFormatting(lines[2])) == 0) {
					player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid buying price!"));
					return false;
				} else if (getShopPrice(ChatUtils.stripColorFormatting(lines[2])) == -1) {
					player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax for buying price!"));
					return false;
				}
			} else if (ChatUtils.stripColorFormatting(lines[2]).startsWith("Sell")) {
				if (getShopPrice(ChatUtils.stripColorFormatting(lines[2])) == 0) {
					player.sendMessage(ChatUtils.chatMessage("&cThat is an invalid selling price!"));
					return false;
				} else if (getShopPrice(ChatUtils.stripColorFormatting(lines[2])) == -1) {
					player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax for selling price!"));
					return false;
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou must either buy or sell here!"));
				return false;
			}

			ItemStack shopItemFromLine = getShopItemFromLine(lines[3]);
			if (shopItemFromLine == null) {
				player.sendMessage(ChatUtils.chatMessage("&cThere is no item with this name!"));
				return false;
			} else if (shopItemFromLine.getType() == Material.AIR) {
				player.sendMessage(ChatUtils.chatMessage("&cThere is more than one item with this criteria!"));
				return false;
			}

			// Will confirm only if all checks are valid
			return true;
		}
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

		if (ChatUtils.stripColorFormatting(quantityLineParts[0]).equals("QTY")) {
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
	 * @param line The line for the shop price.
	 * @return The price of the shop item.
	 */
	private double getShopPrice(String line) {
		String[] priceLineParts = line.split(" ");
		if (priceLineParts.length != 2) {
			return -1;
		}

		int price = 0;
		try {
			price = Integer.parseInt(priceLineParts[1]);
			if (price <= 0) {
				throw new NumberFormatException();
			}
			DecimalFormat df = new DecimalFormat("0.00");
			return Double.parseDouble(df.format(price));
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

	private ItemStack getShopItemFromLine(String line) {
		Material materialForShop = null;
		int matchingCriteriaCounter = 0;
		try {
			// Only supports vanilla items with no metadata
			for (Material material : Material.values()) {
				// If the name is identical
				if (material.name().toLowerCase().equals(ChatUtils.stripColorFormatting(line.toLowerCase()))) {
					return new ItemStack(material, 1);
				}
				// If the name isn't fully defined, make sure there's only one item matching it
				else if (material.name().toLowerCase().startsWith(ChatUtils.stripColorFormatting(line.toLowerCase()))) {
					materialForShop = material;
					matchingCriteriaCounter++;
				}
			}

			// Validates that only one item matches the line content
			if (matchingCriteriaCounter == 1) {
				return new ItemStack(materialForShop, 1);
			} else {
				return new ItemStack(Material.AIR);
			}
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * Handles the logic to clear the lines of the sign if incorrect input is provided.
	 * @param e The event.
	 * @param isPlayerShop Confirmation whether the sign is for a player or server shop.
	 */
	private void clearLines(SignChangeEvent e, boolean isPlayerShop) {
		if (isPlayerShop) {
			e.setLine(0, ChatUtils.translateToColor("&4&l[Shop]"));
		} else {
			e.setLine(0, ChatUtils.translateToColor("&4&l[Server Shop]"));
		}
		e.setLine(1, "");
		e.setLine(2, "");
		e.setLine(3, "");
	}

	/**
	 * Handles the logic of creating or updating a shop.
	 * @param e The event.
	 * @param player The player who created the shop.
	 * @param shopItem The item being bought or sold in the shop.
	 * @param quantity The quantity of the item being bought or sold in the shop.
	 * @param buyPrice The price to buy the item from the shop.
	 * @param sellPrice The price to sell the item to the shop.
	 */
	private void createOrUpdateShop(SignChangeEvent e, Player player, ItemStack shopItem, int quantity, double buyPrice, double sellPrice) {
		HashMap<UUID, List<PlayerShop>> shops = AranarthUtils.getShops();
		if (shops == null) {
			shops = new HashMap<>();
		}

		List<PlayerShop> playerShops = null;
		UUID uuid = null;
		if (player != null) {
			uuid = player.getUniqueId();
		}

		playerShops = shops.get(uuid);
		if (playerShops == null) {
			playerShops = new ArrayList<>();
		}

		Block sign = e.getBlock();
		PlayerShop existingShop = AranarthUtils.getShop(sign.getLocation());
		PlayerShop newShop = null;
		newShop = new PlayerShop(uuid, e.getBlock().getLocation(), shopItem, quantity, buyPrice, sellPrice);

		// If the shop exists, remove it
		if (existingShop != null) {
			AranarthUtils.removeShop(uuid, sign.getLocation());
		}

		AranarthUtils.addShop(uuid, newShop);
		if (player != null) {
			e.setLine(0, ChatUtils.translateToColor("&6&l[Shop]"));
			e.setLine(1, ChatUtils.translateToColor("&0&l" + e.getLines()[1]));
			e.setLine(2, ChatUtils.translateToColor("&0&l" + e.getLines()[2]));
			e.setLine(3, ChatUtils.translateToColor("&0&l" + e.getLines()[3]));
		} else {
			e.setLine(0, ChatUtils.translateToColor("&6&l[Server Shop]"));
			e.setLine(1, ChatUtils.translateToColor("&0&l" + e.getLines()[1]));
			e.setLine(2, ChatUtils.translateToColor("&0&l" + e.getLines()[2]));
		}

		if (existingShop == null) {
			e.getPlayer().sendMessage(ChatUtils.chatMessage("&7You have created a new shop!"));
		} else {
			e.getPlayer().sendMessage(ChatUtils.chatMessage("&7You have updated this shop!"));
		}
		e.getPlayer().playSound(e.getPlayer(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
	}

}
