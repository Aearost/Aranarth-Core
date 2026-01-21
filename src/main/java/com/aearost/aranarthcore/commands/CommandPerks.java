package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.UUID;

/**
 * Applies or removes a perk that a player has.
 */
public class CommandPerks {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.perk.modify")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}
		}

		// Lists the player's perks
		if (args.length == 2) {
			UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
			if (AranarthUtils.getPlayer(uuid) != null) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
				HashMap<Perk, Integer> perks = aranarthPlayer.getPerks();
				sender.sendMessage(ChatUtils.translateToColor("&8      - - - &e" + aranarthPlayer.getNickname() + "&e's &6&lPerks &8- - -"));
				sender.sendMessage(ChatUtils.translateToColor("&6Compressor: &e" + perks.get(Perk.COMPRESSOR)));
				sender.sendMessage(ChatUtils.translateToColor("&6Randomizer: &e" + perks.get(Perk.RANDOMIZER)));
				sender.sendMessage(ChatUtils.translateToColor("&6Blacklist: &e" + perks.get(Perk.BLACKLIST)));
				sender.sendMessage(ChatUtils.translateToColor("&6Tables: &e" + perks.get(Perk.TABLES)));
				sender.sendMessage(ChatUtils.translateToColor("&6ItemName: &e" + perks.get(Perk.ITEMNAME)));
				sender.sendMessage(ChatUtils.translateToColor("&6Chat: &e" + perks.get(Perk.CHAT)));
				sender.sendMessage(ChatUtils.translateToColor("&6Shulker: &e" + perks.get(Perk.SHULKER)));
				sender.sendMessage(ChatUtils.translateToColor("&6Inventory: &e" + perks.get(Perk.INVENTORY)));
				sender.sendMessage(ChatUtils.translateToColor("&6Homes: &e" + perks.get(Perk.HOMES)));
				sender.sendMessage(ChatUtils.translateToColor("&6ItemFrame: &e" + perks.get(Perk.ITEMNAME)));
				sender.sendMessage(ChatUtils.translateToColor("&6BlueFire: &e" + perks.get(Perk.BLUEFIRE)));
				sender.sendMessage(ChatUtils.translateToColor("&6Discord: &e" + perks.get(Perk.DISCORD)));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found"));
				return true;
			}
		}

		if (args.length < 4) {
			// To increase the amount of homes by 3
			if (args.length == 3) {
				if (args[2].equals("homes")) {
					UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
					if (AranarthUtils.getPlayer(uuid) != null) {
						if (isValidPerk(args[2].toLowerCase())) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
							HashMap<Perk, Integer> perks = aranarthPlayer.getPerks();

							if (perks.get(Perk.HOMES) <= 12) {
								perks.put(Perk.HOMES, perks.get(Perk.HOMES) + 3);

								aranarthPlayer.setPerks(perks);
								AranarthUtils.setPlayer(uuid, aranarthPlayer);
								if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
									Player player = Bukkit.getPlayer(uuid);
									PermissionUtils.evaluatePlayerPermissions(player);
								}

								String message = "&e" + aranarthPlayer.getNickname() + " &7has purchased the &4&lAdditional 3 Homes &7perk!";
								Bukkit.broadcastMessage(ChatUtils.chatMessage(message));
								DiscordUtils.donationNotification(message, uuid, Color.CYAN);

								return true;
							} else {
								sender.sendMessage(ChatUtils.chatMessage("&cThis player already has 15 homes!"));
								return true;
							}
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThis is not a valid perk!"));
							return true;
						}
					} else {
						sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found"));
						return true;
					}
				}
			}
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac perks <player> <perk> <value>"));
			return true;
		} else {
			UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
			if (AranarthUtils.getPlayer(uuid) != null) {
				if (isValidPerk(args[2].toLowerCase())) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
					// compressor_randomizer_blacklist_tables_itemname_chat_shulker_inventory_homes_itemframe_bluefire_discord
					HashMap<Perk, Integer> perks = aranarthPlayer.getPerks();
					if (args[2].equals("compressor") || args[2].equals("randomizer") || args[2].equals("blacklist")
							|| args[2].equals("tables") || args[2].equals("itemname") || args[2].equals("chat")
							|| args[2].equals("shulker") || args[2].equals("inventory") || args[2].equals("itemframe")
							|| args[2].equals("bluefire")) {
						if (args[3].equals("0") || args[3].equals("1")) {
							// Updates the perk value based on the input
							perks.put(Perk.valueOf(args[2].toUpperCase()), Integer.parseInt(args[3]));

							String perk = "";
							switch (args[2]) {
								case "compressor" -> perk = "&6&lCompressor";
								case "randomizer" -> perk = "&a&lRandomizer";
								case "blacklist" -> perk = "&8&lBlacklist";
								case "tables" -> perk = "&6&lTables";
								case "itemname" -> perk = "&c&lItem Name";
								case "chat" -> perk = "&e&lColored Chat";
								case "shulker" -> perk = "&5&lShulker Assist";
								case "inventory" -> perk = "&3&lInventory Assist";
								case "itemframe" -> perk = "&f&lInvisible Item Frames";
								case "bluefire" -> perk = "&b&lBlue Fire";
							}

							aranarthPlayer.setPerks(perks);
							AranarthUtils.setPlayer(uuid, aranarthPlayer);

							// Enables all materials to be compressed by default
							if (args[2].equalsIgnoreCase("compressor") && args[3].equals("1")) {
								AranarthUtils.compressAllMaterials(uuid);
							}

							if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
								Player player = Bukkit.getPlayer(uuid);
								PermissionUtils.evaluatePlayerPermissions(player);
							}

							if (!args[3].equals("0")) {
								String message = "&e" + aranarthPlayer.getNickname() + " &7has purchased the " + perk + " &7perk!";
								Bukkit.broadcastMessage(ChatUtils.chatMessage(message));
								DiscordUtils.donationNotification(ChatUtils.stripColorFormatting(message), uuid, Color.CYAN);
							}
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThat is not an appropriate value!"));
						}
						return true;
					} else if (args[2].equals("homes")) {
						if (args[3].equals("0") || args[3].equals("3") || args[3].equals("6") || args[3].equals("9")
								|| args[3].equals("12") || args[3].equals("15")) {
							perks.put(Perk.HOMES, Integer.parseInt(args[3]));
							aranarthPlayer.setPerks(perks);
							AranarthUtils.setPlayer(uuid, aranarthPlayer);
							if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
								Player player = Bukkit.getPlayer(uuid);
								PermissionUtils.evaluatePlayerPermissions(player);
							}

							if (!args[3].equals("0")) {
								String message = "&e" + aranarthPlayer.getNickname() + " &7has purchased the &4&lAdditional 3 Homes &7perk!";
								Bukkit.broadcastMessage(ChatUtils.chatMessage(message));
								DiscordUtils.donationNotification(ChatUtils.stripColorFormatting(message), uuid, Color.CYAN);
							}
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThat is not an appropriate value!"));
						}
						return true;
					} else if (args[2].equals("discord")) {
						perks.put(Perk.DISCORD, Integer.parseInt(args[3]));
						aranarthPlayer.setPerks(perks);
						AranarthUtils.setPlayer(uuid, aranarthPlayer);

						DiscordUtils.updateDiscordRole(Bukkit.getOfflinePlayer(uuid), aranarthPlayer);

						if (!args[3].equals("0")) {
							String message = "&e" + aranarthPlayer.getNickname() + " &7has purchased the &5&lDiscord Chat &7perk!";
							Bukkit.broadcastMessage(ChatUtils.chatMessage(message));
							DiscordUtils.donationNotification(ChatUtils.stripColorFormatting(message), uuid, Color.CYAN);
						}

						return true;
					} else {
						sender.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with this sub-command..."));
						return true;
					}
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cThis is not a valid perk!"));
					return true;
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found"));
				return true;
			}
		}
	}

	private static boolean isValidPerk(String perk) {
		return perk.equals("compressor") || perk.equals("randomizer") || perk.equals("blacklist") || perk.equals("tables")
				|| perk.equals("itemname") || perk.equals("chat") || perk.equals("shulker") || perk.equals("inventory")
				|| perk.equals("homes") || perk.equals("itemframe") || perk.equals("bluefire") || perk.equals("discord");
	}

}
