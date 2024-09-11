package com.aearost.aranarthcore.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Provides a large variety of utility methods for everything related to items and inventory.
 */
public class ItemUtils {
	
	/**
	 * Handles giving the target an item.
	 * 
	 * @param itemToAdd The item to be added.
	 * @param target The player that the item will be given to.
	 * @param sender The one who executed the command.
	 */
	public static void giveItem(ItemStack itemToAdd, Player target, CommandSender sender) {
		if (target != null) {
			ItemStack copyForHasSpace = itemToAdd.clone();
			int remainder = ItemUtils.addToInventory(target, copyForHasSpace);
			sendMessages(itemToAdd, target, sender, remainder);
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
		}
	}
	
	/**
	 * Sends the respective messages to the respective user based on the remainingAmount.
	 *
	 * @param itemToGive The item to be added.
	 * @param target The player that the item will be given to.
	 * @param sender The one who executed the command.
	 * @param remainingAmount The remaining amount of addToInventory().
	 */
	public static void sendMessages(ItemStack itemToGive, Player target, CommandSender sender, int remainingAmount) {
		if (Objects.nonNull(itemToGive.getItemMeta())) {
			String itemName = itemToGive.getItemMeta().getDisplayName();

			// If the sender gave themselves the item
			if (sender instanceof Player senderAsPlayer) {
				// If the sender is also the target
				if (senderAsPlayer.getName().equals(target.getName())) {
					// If the whole ItemStack was added
					if (remainingAmount == 0) {
						target.sendMessage(ChatUtils
								.chatMessage("&6You have been given &a" + itemToGive.getAmount() + " " + itemName + "&6!"));
					}
					// If some but not all was added, and being the quantity that was added
					else if (remainingAmount > 0) {
						int amountGiven = itemToGive.getAmount() - remainingAmount;
						target.sendMessage(
								ChatUtils.chatMessage("&6You have been given &a" + amountGiven + " " + itemName + "&6!"));
						target.sendMessage(
								ChatUtils.chatMessage("&a" + remainingAmount + " " + itemName + " &6was thrown away!"));
					}
					// If none of the ItemStack was added
					else {
						target.sendMessage(ChatUtils.chatMessage("&cYou do not have enough space for that!"));
					}
				}
			}

			// If someone else (including console) gave them the item
			if (remainingAmount == 0) {
				target.sendMessage(ChatUtils
						.chatMessage("&6You have been given &a" + itemToGive.getAmount() + " " + itemName + "&6!"));
				sender.sendMessage(ChatUtils.chatMessage("&e" + target.getName() + " &6has been given &a"
						+ itemToGive.getAmount() + " " + itemName + "&6!"));
			} else if (remainingAmount > 0) {
				int amountGiven = itemToGive.getAmount() - remainingAmount;
				target.sendMessage(
						ChatUtils.chatMessage("&6You have been given &a" + amountGiven + " " + itemName + "&6!"));
				sender.sendMessage(ChatUtils.chatMessage(
						"&e" + target.getName() + " &6has been given &a" + amountGiven + " " + itemName + "&6!"));
				sender.sendMessage(ChatUtils.chatMessage("&a" + remainingAmount + " " + itemName + " &6was thrown away!"));
			} else {
				sender.sendMessage(
						ChatUtils.chatMessage("&7" + target.getName() + " &cdoes not have enough space for that!"));
			}
		}
	}

	/**
	 * Adds the ItemStack to the player's inventory.
	 * Returns 0 if the entire ItemStack was added.
	 * Returns -1 if none of the ItemStack was added.
	 * If some but not all was added, and being the quantity that was added.
	 *
	 * @param player The player to have the item added.
	 * @param itemToAdd The item to be added.
	 * @return A value confirming whether all, some, or none of the inventory was added.
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
				// Fill up an empty stack until it's full while removing one amount each iteration
				if (Objects.nonNull(is.getItemMeta()) && Objects.nonNull(itemToAdd.getItemMeta())) {
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
		}

		// Prioritizes filling up empty inventory slots
		ItemStack is = itemToAdd.clone();
		while (amount > 0) {
			// When there is inventory space
			if (player.getInventory().firstEmpty() != -1) {
				is = itemToAdd.clone();
				if (amount > 64) {
					is.setAmount(64);
					player.getInventory().addItem(is);
					amount = amount - 64;
				} else {
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
     * @author graywolf336 <a href="https://gist.github.com/graywolf336/8153678">graywolf336 GitHub</a>
     * @param playerInventory to turn into an array of strings.
     * @return Array of strings: [ main content, armor content ].
     * @throws IllegalStateException Thrown exception when converting to base64.
     */
    public static String[] playerInventoryToBase64(PlayerInventory playerInventory) throws IllegalStateException {
    	//get the main content part, this doesn't return the armor
    	String content = toBase64(playerInventory);
    	String armor = itemStackArrayToBase64(playerInventory.getArmorContents());
    	
    	return new String[] { content, armor };
    }
    
    /**
     * A method to serialize an {@link ItemStack} array to Base64 String.
     * <p />
     * 
     * Based off of {@link #toBase64(Inventory)}.
     * 
     * @author graywolf336 <a href="https://gist.github.com/graywolf336/8153678">graywolf336 GitHub</a>
     * @param items to turn into a Base64 String.
     * @return Base64 string of the items.
     * @throws IllegalStateException Thrown exception when converting to base64.
     */
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
    	try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // Write the size of the inventory
            dataOutput.writeInt(items.length);
            
            // Save every element in the list
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            
            // Serialize that array
            dataOutput.close();
            
            String encodedInventory = Base64Coder.encodeLines(outputStream.toByteArray());
            String noNewlineT = encodedInventory.replaceAll("\t","dEfG1hIjK2LmN3o");
            String noNewlineN = noNewlineT.replaceAll("\n","9bYnTpRqWs1x2zC");
            return noNewlineN.replaceAll("\r","8hUjIkLo7pYt6rS");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }
    
    /**
     * A method to serialize an inventory to Base64 string.
     * <p />
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     * 
     * @author graywolf336 <a href="https://gist.github.com/graywolf336/8153678">graywolf336 GitHub</a>
     * @param inventory to serialize.
     * @return Base64 string of the provided inventory.
     * @throws IllegalStateException Thrown exception when converting to base64.
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
            String encodedInventory = Base64Coder.encodeLines(outputStream.toByteArray());
            String noNewlineT = encodedInventory.replaceAll("\t","dEfG1hIjK2LmN3o");
            String noNewlineN = noNewlineT.replaceAll("\n","9bYnTpRqWs1x2zC");
            return noNewlineN.replaceAll("\r","8hUjIkLo7pYt6rS");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }
    
    /**
     * 
     * A method to get an {@link Inventory} from an encoded, Base64, string.
     * <p />
     * 
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     * 
     * @author graywolf336 <a href="https://gist.github.com/graywolf336/8153678">graywolf336 GitHub</a>
     * @param data Base64 string of data containing an inventory.
     * @return Inventory created from the Base64 string.
     * @throws IOException Thrown exception when converting from base64.
     */
    public static Inventory fromBase64(String data) throws IOException {
    	
        String noNewlineT = data.replaceAll("dEfG1hIjK2LmN3o","\t");
        String noNewlineN = noNewlineT.replaceAll("9bYnTpRqWs1x2zC","\n");
        String noNewlines = noNewlineN.replaceAll("8hUjIkLo7pYt6rS","\r");
    	
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(noNewlines));
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
     * <p />
     * 
     * Base off of {@link #fromBase64(String)}.
     * 
     * @param data Base64 string to convert to ItemStack array.
     * @return ItemStack array created from the Base64 string.
     * @throws IOException Thrown exception when converting from base64.
     */
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
    	String noNewlineT = data.replaceAll("dEfG1hIjK2LmN3o","\t");
        String noNewlineN = noNewlineT.replaceAll("9bYnTpRqWs1x2zC","\n");
        String noNewlines = noNewlineN.replaceAll("8hUjIkLo7pYt6rS","\r");
        
    	try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(noNewlines));
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
