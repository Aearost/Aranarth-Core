package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiShopLocation;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * Allows players to teleport to defined player shops.
 * Can also define and maintain player shops.
 */
public class CommandShop {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			// Display the GUI to teleport
			if (args.length == 1) {
				GuiShopLocation gui = new GuiShopLocation(player, 0);
				gui.openGui();
				return true;
			} else {
				if (args[1].equalsIgnoreCase("create")) {
					if (!player.hasPermission("aranarth.shop.modify")) {
						player.sendMessage(ChatUtils.chatMessage("&cThis player does not have a shop!"));
						return true;
					}

					if (args.length < 3) {
						player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac shop create <username>"));
						return true;
					}

					UUID uuid = AranarthUtils.getUUIDFromUsername(args[2]);
					if (uuid != null) {
						if (AranarthUtils.getShopLocations().containsKey(uuid)) {
							player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + " &calready has a shop location!"));
							return true;
						}

						Location location = player.getLocation();
						location.setX(location.getBlockX() + 0.5);
						location.setY(location.getBlockY() + 0.5);
						location.setZ(location.getBlockZ() + 0.5);
						AranarthUtils.createShopLocation(uuid, location);
						player.sendMessage(ChatUtils.chatMessage("&7A shop has been created for &e" + args[2]));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + " &ccould not be found!"));
					}
					return true;
				} else if (args[1].equalsIgnoreCase("delete")) {
					if (!player.hasPermission("aranarth.warp.modify")) {
						player.sendMessage(ChatUtils.chatMessage("&cThis player does not have a shop!"));
						return true;
					}

					if (args.length < 3) {
						player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac shop create <username>"));
						return true;
					}

					UUID uuid = AranarthUtils.getUUIDFromUsername(args[2]);
					if (uuid != null) {
						if (AranarthUtils.getShopLocations().containsKey(uuid)) {
							AranarthUtils.deleteShopLocation(uuid);
							player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + "'s &7shop has been deleted"));
						} else {
							player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + " &cdoes not have a shop"));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + " &ccould not be found!"));
					}
					return true;
				} else {
					HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
					boolean[] wasShopFound = new boolean[] { false };
					for (UUID uuid : shopLocations.keySet()) {
						String username = AranarthUtils.getUsername(Bukkit.getOfflinePlayer(uuid));
						if (args[1].equalsIgnoreCase(username)) {
							AranarthPlayer shopOwnerPlayer = AranarthUtils.getPlayer(uuid);
							if (shopLocations.get(uuid) != null) {
								wasShopFound[0] = true;
								AranarthUtils.teleportPlayer(player, player.getLocation(), shopLocations.get(uuid), success -> {
									if (success) {
										player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + shopOwnerPlayer.getNickname() + "'s &7shop!"));
									} else {
										player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + shopOwnerPlayer.getNickname() + "'s &cshop!"));
									}
								});
							}
						}
					}

					if (!wasShopFound[0]) {
						player.sendMessage(ChatUtils.chatMessage("&cThis player does not have a shop!"));
					}
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed in-game!"));
			return true;
		}
	}

}
