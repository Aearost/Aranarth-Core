package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

		if (args.length != 3) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: /ac give <player> <item>"));
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
					fullPathName = "com.aearost.aranarthcore.items.crates." + args[2];
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

				if (instance instanceof AranarthItem item) {
					player.getInventory().addItem(item.getItem());
					player.sendMessage(ChatUtils.chatMessage("&7You have been given a " + item.getName()));
					if (sender instanceof Player playerSender) {
						if (!playerSender.getUniqueId().equals(player.getUniqueId())) {
							sender.sendMessage(ChatUtils.chatMessage("&e" + player.getName() + " &7has been given a " + item.getName()));
						}
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
