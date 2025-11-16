package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows for the manual assignment of a player's rank.
 * Functions for the rank, saint rank, and council rank.
 */
@SuppressWarnings({"IfCanBeSwitch", "SingleStatementInBlock"})
public class CommandRankSet {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.rankset")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}
		}

		// /ac rankset rank Aearost 4
		if (args.length == 4) {
			if (args[1].equalsIgnoreCase("rank") || args[1].equalsIgnoreCase("saint")
					|| args[1].equalsIgnoreCase("council") || args[1].equalsIgnoreCase("architect")) {
				OfflinePlayer player = null;

				// Does the player exist
				for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
					if (offlinePlayer.getName().equals(args[2])) {
						player = offlinePlayer;
					}
				}

				if (player != null) {
					int rank = -1;
					try {
						rank = Integer.parseInt(args[3]);
					} catch (NumberFormatException e) {
						sender.sendMessage(ChatUtils.chatMessage("&cThat is not a valid number!"));
						return true;
					}

					if (rank < 0) {
						sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a positive number!"));
						return true;
					}

					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					String name = "";
					boolean isSamePlayer = false;
					if (sender instanceof Player senderPlayer) {
						if (senderPlayer.getUniqueId().equals(player.getUniqueId())) {
							isSamePlayer = true;
							PermissionUtils.evaluatePlayerPermissions(senderPlayer, false);
						}
					}

					if (!aranarthPlayer.getNickname().isEmpty()) {
						name = aranarthPlayer.getNickname();
					} else {
						name = player.getName();
					}

					boolean isSuccessful = false;
					// Limited from 0 to 8
					if (args[1].equals("rank")) {
						if (rank <= 8) {
							aranarthPlayer.setRank(rank);
							DiscordUtils.updateRank(player, rank, true);
							isSuccessful = true;
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThere is no rank with this value!"));
						}
					}
					// Limited from 0 to 3
					else if (args[1].equals("saint")) {
						if (rank <= 3) {
							aranarthPlayer.setSaintRank(rank);
							DiscordUtils.updateSaint(player, rank, true);
							isSuccessful = true;
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThere is no rank with this value!"));
						}
					}
					// Limited from 0 to 3
					else if (args[1].equals("council")) {
						if (rank <= 3) {
							aranarthPlayer.setCouncilRank(rank);
							DiscordUtils.updateCouncil(player, rank, true);
							isSuccessful = true;
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThere is no rank with this value!"));
						}
					}
					// Limited from 0 to 1
					else if (args[1].equals("architect")) {
						if (rank <= 1) {
							aranarthPlayer.setArchitectRank(rank);
							DiscordUtils.updateArchitect(player, rank, true);
							isSuccessful = true;
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThere is no rank with this value!"));
						}
					}

					if (isSuccessful) {
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						String rankName = Character.toUpperCase(args[1].charAt(0)) + args[1].substring(1);

						if (isSamePlayer) {
							if (args[1].equals("rank")) {
								sender.sendMessage(ChatUtils.chatMessage("&7Your rank has been updated"));
							} else {
								sender.sendMessage(ChatUtils.chatMessage("&7Your &e" + rankName + " &7rank has been updated"));
							}
						} else {
							if (args[1].equals("rank")) {
								sender.sendMessage(ChatUtils.chatMessage("&e" + name + "&7's rank has been updated!"));
							} else {
								sender.sendMessage(ChatUtils.chatMessage("&e" + name + "&7's &e" + rankName + " &7rank has been updated!"));
							}

							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
									PermissionUtils.evaluatePlayerPermissions(onlinePlayer, false);
									if (args[1].equals("rank")) {
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your rank has been updated"));
									} else {
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your &e" + rankName + " &7rank has been updated"));
									}
								}
							}
						}
					}
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cThat player was not found!"));
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cThat is an invalid rank type!"));
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax! /ac rankset <rank type> <player name> <level>"));
		}
		return true;
	}

}
