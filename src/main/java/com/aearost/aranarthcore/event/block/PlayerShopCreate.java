package com.aearost.aranarthcore.event.block;

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
		if (ChatUtils.stripColorFormatting(lines[0]).equals("[Shop]")) {
			Block sign = e.getBlock();

			// Verifies that the sign follows the shop format
			if (isValidSignFormat(lines, player)) {

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
			clearLines(e);
		} else {
			// Remove if the shop previously existed and now was changed
			if (AranarthUtils.isShop(e.getBlock().getLocation())) {
				AranarthUtils.removeShop(player.getUniqueId(), e.getBlock().getLocation());
				player.sendMessage(ChatUtils.chatMessage("&7You have destroyed this shop"));
				e.setLine(0, ChatUtils.stripColorFormatting(e.getLine(0)));
				e.setLine(1, ChatUtils.stripColorFormatting(e.getLine(1)));
				e.setLine(2, ChatUtils.stripColorFormatting(e.getLine(2)));
				e.setLine(3, ChatUtils.stripColorFormatting(e.getLine(3)));
			}
		}
	}

	private boolean isValidSignFormat(String[] lines, Player player) {
		if (!isUsernameMatching(ChatUtils.stripColorFormatting(lines[1]), player)) {
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

	private boolean isUsernameMatching(String line, Player player) {
		return line.equals(player.getName());
	}

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

	private boolean isBlockBelowChest(Block sign) {
		Location signLocation = sign.getLocation();
		Location blockBelowLocation = new Location(signLocation.getWorld(),
				signLocation.getBlockX(), signLocation.getBlockY() - 1, signLocation.getBlockZ());

		return signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.CHEST
				|| signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.BARREL
				|| signLocation.getWorld().getBlockAt(blockBelowLocation).getType() == Material.TRAPPED_CHEST;
	}

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

	private void clearLines(SignChangeEvent e) {
		e.setLine(0, ChatUtils.translateToColor("&4&l[Shop]"));
		e.setLine(1, "");
		e.setLine(2, "");
		e.setLine(3, "");
	}

	private void createOrUpdateShop(SignChangeEvent e, Player player, ItemStack shopItem, int quantity, double buyPrice, double sellPrice) {
		HashMap<UUID, List<PlayerShop>> shops = AranarthUtils.getShops();
		if (shops == null) {
			shops = new HashMap<>();
		}

		List<PlayerShop> playerShops = shops.get(player.getUniqueId());
		if (playerShops == null) {
			playerShops = new ArrayList<>();
		}

		Block sign = e.getBlock();
		PlayerShop existingShop = AranarthUtils.getShop(player.getUniqueId(), sign.getLocation());
		PlayerShop newShop = new PlayerShop(player.getUniqueId(), e.getBlock().getLocation(), shopItem, quantity, buyPrice, sellPrice);

		// If the shop exists, remove it
		if (existingShop != null) {
			AranarthUtils.removeShop(player.getUniqueId(), sign.getLocation());
		}
		AranarthUtils.addShop(player.getUniqueId(), newShop);
		e.setLine(0, ChatUtils.translateToColor("&6&l[Shop]"));
		e.setLine(1, ChatUtils.translateToColor("&0&l" + e.getLines()[1]));
		e.setLine(2, ChatUtils.translateToColor("&0&l" + e.getLines()[2]));
		e.setLine(3, ChatUtils.translateToColor("&0&l" + e.getLines()[3]));

		if (existingShop == null) {
			player.sendMessage(ChatUtils.chatMessage("&7You have created a new shop!"));
		} else {
			player.sendMessage(ChatUtils.chatMessage("&7You have updated this shop!"));
		}
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
	}

}
