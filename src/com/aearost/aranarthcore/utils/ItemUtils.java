package com.aearost.aranarthcore.utils;

import java.util.HashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.items.HomePad;
import com.aearost.aranarthcore.items.Item;

public class ItemUtils {

	private final static HashMap<String, ItemStack> itemsToItemStack = new HashMap<String, ItemStack>();

	public ItemUtils() {
		initializeItemsToItemStack();
	}

	/**
	 * Initializes all items into ItemStacks to the itemsToItemStack HashMap.
	 */
	private void initializeItemsToItemStack() {
		// Basic Items
		itemsToItemStack.put(Item.HOMEPAD.name(), HomePad.getHomePad());
	}

	public static ItemStack getItem(String itemName) {
		Item i = Item.valueOf(itemName.toUpperCase());
		return itemsToItemStack.get(i.name());
	}

	// Might need tweaking when used as it was translated to work for Teas
	public static String getItemName(ItemStack is) {
		String itemName = is.getItemMeta().getDisplayName();
		if (itemName.startsWith("&")) {
			itemName = itemName.substring(2);
		}
		itemName = itemName.toUpperCase();
		itemName = itemName.replace(" ", "_");
		return itemName;
	}
	
	/**
	 * Handles giving the target an item.
	 * 
	 * @param itemToAdd
	 * @param target
	 * @param sender
	 * @return
	 */
	public static boolean giveItem(ItemStack itemToAdd, Player target, CommandSender sender) {
		if (target != null) {
			ItemStack copyForHasSpace = itemToAdd.clone();
			int remainder = ItemUtils.addToInventory(target, copyForHasSpace);
			// If the whole ItemStack was added
			if (remainder == 0) {
				return sendMessages(itemToAdd, target, sender, 0);
			}
			// If none of the ItemStack was added
			else if (remainder == -1) {
				return sendMessages(itemToAdd, target, sender, -1);
			}
			// If some but not all was added, and being the quantity that was added
			else {
				return sendMessages(itemToAdd, target, sender, remainder);
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessageError("That player is not online!"));
		}
		return false;
	}
	/*
	 * Returns 0 if the entire ItemStack was added.
	 * Returns -1 if none of the ItemStack fit.
	 * Returns the remainder of what could not be fit, and adds what could.
	 */
	
	/**
	 * Sends the respective messages to the respective location based on the
	 * remainingAmount.
	 * 
	 * @param itemToGive
	 * @param target
	 * @param sender
	 * @param remainingAmount The remaining amount of addToInventory().
	 * @return Whether to successfully execute the command or not.
	 */
	public static boolean sendMessages(ItemStack itemToGive, Player target, CommandSender sender, int remainingAmount) {
		// If the the sender gave themselves the item
		if (sender instanceof Player) {
			Player senderAsPlayer = (Player) sender;
			String itemName = itemToGive.getItemMeta().getDisplayName();

			// If the sender is also the target
			if (senderAsPlayer.getName().equals(target.getName())) {
				// If the whole ItemStack was added
				if (remainingAmount == 0) {
					target.sendMessage(ChatUtils
							.chatMessage("&6You have been given &a" + itemToGive.getAmount() + " " + itemName + "&6!"));
					return true;
				}
				// If some but not all was added, and being the quantity that was added
				else if (remainingAmount > 0) {
					int amountGiven = itemToGive.getAmount() - remainingAmount;
					target.sendMessage(
							ChatUtils.chatMessage("&6You have been given &a" + amountGiven + " " + itemName + "&6!"));
					target.sendMessage(
							ChatUtils.chatMessage("&a" + remainingAmount + " " + itemName + " &6was thrown away!"));
					return true;
				}
				// If none of the ItemStack was added
				else {
					target.sendMessage(ChatUtils.chatMessage("&cYou do not have enough space for that!"));
					return false;
				}
			}
		}

		// If someone else (including console) gave them the item
		String itemName = itemToGive.getItemMeta().getDisplayName();
		if (remainingAmount == 0) {
			target.sendMessage(ChatUtils
					.chatMessage("&6You have been given &a" + itemToGive.getAmount() + " " + itemName + "&6!"));
			sender.sendMessage(ChatUtils.chatMessage("&e" + target.getName() + " &6has been given &a"
					+ itemToGive.getAmount() + " " + itemName + "&6!"));
			return true;
		} else if (remainingAmount > 0) {
			int amountGiven = itemToGive.getAmount() - remainingAmount;
			target.sendMessage(
					ChatUtils.chatMessage("&6You have been given &a" + amountGiven + " " + itemName + "&6!"));
			sender.sendMessage(ChatUtils.chatMessage(
					"&e" + target.getName() + " &6has been given &a" + amountGiven + " " + itemName + "&6!"));
			sender.sendMessage(ChatUtils.chatMessage("&a" + remainingAmount + " " + itemName + " &6was thrown away!"));
			return true;
		} else {
			sender.sendMessage(
					ChatUtils.chatMessage("&7" + target.getName() + " &cdoes not have enough space for that!"));
			return false;
		}
	}

	/**
	 * Adds the ItemStack to the player's inventory.
	 * 
	 * Returns 0 if the entire ItemStack was added.
	 * Returns -1 if none of the ItemStack was added.
	 * If some but not all was added, and being the quantity that was added.
	 * 
	 * @param player
	 * @param itemToAdd
	 * @return
	 */
	public static int addToInventory(Player player, ItemStack itemToAdd) {
		int amount = itemToAdd.getAmount();

		// Prioritizes filling up non-full stacks of the item in the player's inventory
		ItemStack[] inventory = player.getInventory().getStorageContents();
		for (ItemStack is : inventory) {
			if (is != null) {
				if (amount == 0) {
					return 0;
				}
				// Fill up an empty stack until it's full while removing one amount each
				// iteration
				if (is.getItemMeta().getDisplayName().equals(itemToAdd.getItemMeta().getDisplayName())) {
					while (amount > 0) {
						if (is.getAmount() < 64) {
							is.setAmount(is.getAmount() + 1);
							amount--;
						} else {
							break;
						}
					}
				}
			}
		}

		// Prioritizes filling up empty inventory slots
		ItemStack is = itemToAdd.clone();
		while (amount > 0) {
			// When there is inventory space
			if (player.getInventory().firstEmpty() != -1) {
				if (amount > 64) {
					is = itemToAdd.clone();
					is.setAmount(64);
					player.getInventory().addItem(is);
					amount = amount - 64;
				} else {
					is = itemToAdd.clone();
					is.setAmount(amount);
					player.getInventory().addItem(is);
					return 0;
				}
				// Some was placed in the inventory, but not all
			} else if (amount < itemToAdd.getAmount()) {
				return amount;
				// No space in the inventory
			} else {
				return -1;
			}
		}
		return 0;
	}
	
}
