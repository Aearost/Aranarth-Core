package com.aearost.aranarthcore.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

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
					target.sendMessage(ChatUtils.chatMessageError("You do not have enough space for that!"));
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
	
	/**
     * Converts the player inventory to a String array of Base64 strings. First string is the content and second string is the armor.
     * 
     * @author graywolf336 https://gist.github.com/graywolf336/8153678
     * @param playerInventory to turn into an array of strings.
     * @return Array of strings: [ main content, armor content ]
     * @throws IllegalStateException
     */
    public static String[] playerInventoryToBase64(PlayerInventory playerInventory) throws IllegalStateException {
    	//get the main content part, this doesn't return the armor
    	String content = toBase64(playerInventory);
    	String armor = itemStackArrayToBase64(playerInventory.getArmorContents());
    	
    	return new String[] { content, armor };
    }
    
    /**
     * 
     * A method to serialize an {@link ItemStack} array to Base64 String.
     * 
     * <p />
     * 
     * Based off of {@link #toBase64(Inventory)}.
     * 
     * @author graywolf336 https://gist.github.com/graywolf336/8153678
     * @param items to turn into a Base64 String.
     * @return Base64 string of the items.
     * @throws IllegalStateException
     */
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
    	try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // Write the size of the inventory
            dataOutput.writeInt(items.length);
            
            // Save every element in the list
            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }
            
            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }
    
    /**
     * A method to serialize an inventory to Base64 string.
     * 
     * <p />
     * 
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     * 
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     * 
     * @author graywolf336 https://gist.github.com/graywolf336/8153678
     * @param inventory to serialize
     * @return Base64 string of the provided inventory
     * @throws IllegalStateException
     */
    public static String toBase64(Inventory inventory) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // Write the size of the inventory
            dataOutput.writeInt(inventory.getSize());
            
            // Save every element in the list
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }
            
            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }
    
    /**
     * 
     * A method to get an {@link Inventory} from an encoded, Base64, string.
     * 
     * <p />
     * 
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     * 
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     * 
     * @author graywolf336 https://gist.github.com/graywolf336/8153678
     * @param data Base64 string of data containing an inventory.
     * @return Inventory created from the Base64 string.
     * @throws IOException
     */
    public static Inventory fromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());
    
            // Read the serialized inventory
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }
            
            dataInput.close();
            return inventory;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
    
    /**
     * Gets an array of ItemStacks from Base64 string.
     * 
     * <p />
     * 
     * Base off of {@link #fromBase64(String)}.
     * 
     * @param data Base64 string to convert to ItemStack array.
     * @return ItemStack array created from the Base64 string.
     * @throws IOException
     */
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
    	try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
    
            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
            	items[i] = (ItemStack) dataInput.readObject();
            }
            
            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}
