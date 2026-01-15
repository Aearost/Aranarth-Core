package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Provides a specified player an AranarthCore item.
 * Dynamically fetches the associated Item object based on the input.
 */
public class CommandGive {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static void onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.give")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return;
			}
		}

		if (args.length < 3) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac give <player> <item> <quantity>"));
			return;
		} else {
			Player player = null;
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.getName().equals(args[1])) {
					player = onlinePlayer;
					break;
				}
			}

			if (player != null) {
				boolean isKey = false;
				String fullPathName = "";
				if (args[2].startsWith("Aranarthium")) {
					fullPathName = "com.aearost.aranarthcore.items.aranarthium.ingots." + args[2];
				} else if (args[2].endsWith("Cluster")) {
					fullPathName = "com.aearost.aranarthcore.items.aranarthium.clusters." + args[2];
				} else if (args[2].endsWith("Helmet") || args[2].endsWith("Chestplate")
						|| args[2].endsWith("Leggings") || args[2].endsWith("Boots")) {
					fullPathName = "com.aearost.aranarthcore.items.aranarthium.armour." + args[2];
				} else if (args[2].startsWith("Arrow")) {
					if (args[2].startsWith("Arrowhead")) {
						fullPathName = "com.aearost.aranarthcore.items.arrowhead." + args[2];
					} else {
						fullPathName = "com.aearost.aranarthcore.items.arrow." + args[2];
					}
				} else if (args[2].startsWith("Key")) {
					isKey = true;
					fullPathName = "com.aearost.aranarthcore.items.key." + args[2];
				} else {
					fullPathName = "com.aearost.aranarthcore.items." + args[2];
				}

				Object instance = null;
				try {
					Class<?> unknownClass = Class.forName(fullPathName);
					instance = unknownClass.getDeclaredConstructor().newInstance();
				} catch (ClassNotFoundException | InvocationTargetException | InstantiationException
						 | IllegalAccessException | NoSuchMethodException e) {
					sender.sendMessage(ChatUtils.chatMessage("&cThere is no item by that name!"));
					return;
				}

				if (instance instanceof AranarthItem aranarthItem) {
					ItemStack item = aranarthItem.getItem();
					int quantity = 1;
					if (args.length >= 4) {
						try {
							quantity = Integer.parseInt(args[3]);
							if (quantity <= 0 || quantity > item.getMaxStackSize()) {
								throw new NumberFormatException();
							}
							item.setAmount(quantity);
						} catch (Exception e) {
							player.sendMessage(ChatUtils.chatMessage("&cThe entered Quantity is invalid!"));
							return;
						}
					}

					player.getInventory().addItem(item);

					ItemMeta meta = item.getItemMeta();
					String itemName = ChatUtils.getFormattedItemName(item.getType().name());
					if (meta != null) {
						if (meta.hasDisplayName()) {
							itemName = meta.getDisplayName();
						}
					}

					player.sendMessage(ChatUtils.chatMessage("&7You have been given " + itemName + " x" + quantity));
					if (sender instanceof Player playerSender) {
						if (!playerSender.getUniqueId().equals(player.getUniqueId())) {
							sender.sendMessage(ChatUtils.chatMessage("&e" + player.getName() + " &7has been given " + itemName + " x" + quantity));
						}
					}

					if (isKey) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						DiscordUtils.donationNotification(player.getName() + " has purchased " + item.getItemMeta().getDisplayName() + " x3", player.getUniqueId(), Color.CYAN);
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThere is no item by that name!"));
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cThat player was not found!"));
			}
		}
	}

}
