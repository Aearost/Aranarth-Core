package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
				String[] perks = aranarthPlayer.getPerks().split("_");
				sender.sendMessage(ChatUtils.translateToColor("&8      - - - &e" + aranarthPlayer.getNickname() + "&e's &6&lPerks &8- - -"));
				sender.sendMessage(ChatUtils.translateToColor("&6Compressor: &e" + perks[0]));
				sender.sendMessage(ChatUtils.translateToColor("&6Randomizer: &e" + perks[1]));
				sender.sendMessage(ChatUtils.translateToColor("&6Blacklist: &e" + perks[2]));
				sender.sendMessage(ChatUtils.translateToColor("&6Tables: &e" + perks[3]));
				sender.sendMessage(ChatUtils.translateToColor("&6ItemName: &e" + perks[4]));
				sender.sendMessage(ChatUtils.translateToColor("&6Chat: &e" + perks[5]));
				sender.sendMessage(ChatUtils.translateToColor("&6Shulker: &e" + perks[6]));
				sender.sendMessage(ChatUtils.translateToColor("&6Inventory: &e" + perks[7]));
				sender.sendMessage(ChatUtils.translateToColor("&6Homes: &e" + perks[8]));
				sender.sendMessage(ChatUtils.translateToColor("&6ItemFrame: &e" + perks[9]));
				sender.sendMessage(ChatUtils.translateToColor("&6BlueFire: &e" + perks[10]));
				sender.sendMessage(ChatUtils.translateToColor("&6Discord: &e" + perks[11]));
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
							// compressor_randomizer_blacklist_tables_itemname_chat_shulker_inventory_homes_itemframe_bluefire_discord
							String[] perks = aranarthPlayer.getPerks().split("_");
                            switch (perks[8]) {
                                case "0" -> perks[8] = "3";
                                case "3" -> perks[8] = "6";
                                case "6" -> perks[8] = "9";
                                case "9" -> perks[8] = "12";
                                case "12" -> perks[8] = "15";
                            }

							// Updates the perk variable
							String perksAsString = "";
							for (int i = 0; i < perks.length; i++) {
								perksAsString += perks[i];
								if (i < perks.length - 1) {
									perksAsString += "_";
								}
							}
							aranarthPlayer.setPerks(perksAsString);
							AranarthUtils.setPlayer(uuid, aranarthPlayer);
							if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
								Player player = Bukkit.getPlayer(uuid);
								PermissionUtils.evaluatePlayerPermissions(player, false);
							}
							sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + "&7's perks have been updated"));
							return true;
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
					String[] perks = aranarthPlayer.getPerks().split("_");
					if (args[2].equals("compressor") || args[2].equals("randomizer") || args[2].equals("blacklist")
							|| args[2].equals("tables") || args[2].equals("itemname") || args[2].equals("chat")
							|| args[2].equals("shulker") || args[2].equals("inventory") || args[2].equals("itemframe")
							|| args[2].equals("bluefire")) {
						if (args[3].equals("0") || args[3].equals("1")) {
							switch (args[2]) {
								case "compressor" -> perks[0] = args[3];
								case "randomizer" -> perks[1] = args[3];
								case "blacklist" -> perks[2] = args[3];
								case "tables" -> perks[3] = args[3];
								case "itemname" -> perks[4] = args[3];
								case "chat" -> perks[5] = args[3];
								case "shulker" -> perks[6] = args[3];
								case "inventory" -> perks[7] = args[3];
								case "itemframe" -> perks[9] = args[3];
								case "bluefire" -> perks[10] = args[3];
								case "discord" -> perks[11] = args[3];
							}
							// Updates the perk variable
							String perksAsString = "";
							for (int i = 0; i < perks.length; i++) {
								perksAsString += perks[i];
								if (i < perks.length - 1) {
									perksAsString += "_";
								}
							}
							aranarthPlayer.setPerks(perksAsString);
							AranarthUtils.setPlayer(uuid, aranarthPlayer);
							if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
								Player player = Bukkit.getPlayer(uuid);
								PermissionUtils.evaluatePlayerPermissions(player, false);
							}
							sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + "&7's perks have been updated"));
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThat is not an appropriate value!"));
						}
						return true;
					} else if (args[2].equals("homes")) {
						if (args[3].equals("0") || args[3].equals("3") || args[3].equals("6") || args[3].equals("9")
								|| args[3].equals("12") || args[3].equals("15")) {
							perks[8] = args[3];

							// Updates the perk variable
							String perksAsString = "";
							for (int i = 0; i < perks.length; i++) {
								perksAsString += perks[i];
								if (i < perks.length - 1) {
									perksAsString += "_";
								}
							}
							aranarthPlayer.setPerks(perksAsString);
							AranarthUtils.setPlayer(uuid, aranarthPlayer);
							if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
								Player player = Bukkit.getPlayer(uuid);
								PermissionUtils.evaluatePlayerPermissions(player, false);
							}

							sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + "&7's perks have been updated"));
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThat is not an appropriate value!"));
						}
						return true;
					} else if (args[2].equals("discord")) {
						perks[11] = args[3];

						// Updates the perk variable
						String perksAsString = "";
						for (int i = 0; i < perks.length; i++) {
							perksAsString += perks[i];
							if (i < perks.length - 1) {
								perksAsString += "_";
							}
						}
						aranarthPlayer.setPerks(perksAsString);
						AranarthUtils.setPlayer(uuid, aranarthPlayer);

						DiscordUtils.updateDiscordRole(Bukkit.getOfflinePlayer(uuid), aranarthPlayer);
						sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + "&7's perks have been updated"));
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
