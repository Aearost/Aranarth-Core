package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Centralizes all functionality relating to dominions.
 */
public class CommandDominion {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		// Shorthand of /ac dominion home
		if (args.length == 1) {
			if (sender instanceof Player player) {
				if (player.hasPermission("aranarth.dominion.home")) {
					teleportToDominionHome(player);
					return true;
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to create a dominion!"));
					return true;
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
				return true;
			}
		} else {
			if (args.length >= 2) {
				if (sender instanceof Player player) {
					Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
					if (dominion == null) {
						if (args[1].equalsIgnoreCase("create")) {
							if (player.hasPermission("aranarth.dominion.create")) {
								createDominion(args, player);
								return true;
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to create a dominion!"));
								return true;
							}
						}
					} else {
						if (args[1].equalsIgnoreCase("add")) {

						}
						else if (args[1].equalsIgnoreCase("remove")) {

						}
						else if (args[1].equalsIgnoreCase("disband")) {
							disbandDominion(dominion, player);
							return true;
						}
						else if (args[1].equalsIgnoreCase("claim")) {
							player.sendMessage(ChatUtils.chatMessage(DominionUtils.claimChunk(player)));
							return true;
						}
						else if (args[1].equalsIgnoreCase("unclaim")) {
							player.sendMessage(ChatUtils.chatMessage(DominionUtils.unclaimChunk(player)));
							return true;
						}
						else if (args[1].equalsIgnoreCase("balance")) {
							player.sendMessage(ChatUtils.chatMessage(dominion.getName() + "&7's balance is &e$" + dominion.getBalance()));
							return true;
						}
						else if (args[1].equalsIgnoreCase("home")) {
							if (player.hasPermission("aranarth.dominion.home")) {
								teleportToDominionHome(player);
								return true;
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to create a dominion!"));
								return true;
							}
						}
					}

					// The following work regardless of the player's dominion
					if (args[1].equalsIgnoreCase("who")) {
						if (!args[2].isEmpty()) {
							UUID uuid = AranarthUtils.getUUIDFromUsername(args[2]);
							if (uuid != null) {
								Dominion searchedPlayerDominion = DominionUtils.getPlayerDominion(uuid);
								if (searchedPlayerDominion != null) {
									player.sendMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getPlayer(uuid).getNickname() + "&7 is in the Dominion of &e" + searchedPlayerDominion.getName()));
								} else {
									player.sendMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getPlayer(uuid).getNickname() + "&7 is not in a Dominion!"));
								}
								return true;
							} else {
								player.sendMessage(ChatUtils.chatMessage("&c" + args[2] + " could not be found!"));
								return true;
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYou must enter a player's username!"));
							return true;
						}
					}
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Teleports the player to their dominion's home.
	 * @param player The player.
	 */
	private static void teleportToDominionHome(Player player) {
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		if (dominion != null) {
			player.teleport(dominion.getDominionHome());
			player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
			player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + dominion.getName()));
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Creates a new dominion.
	 *
	 * @param args   The arguments of the command.
	 * @param player The player that executed the command.
	 */
	private static void createDominion(String[] args, Player player) {
		if (args.length >= 3) {
			if (args[2].matches("^[^\"\\n\\r\\t#&]+$")) {
				// Ensures the player is not in a dominion
				if (DominionUtils.getPlayerDominion(player.getUniqueId()) == null) {
					Dominion dominionOfChunk = DominionUtils.getDominionOfChunk(player.getLocation().getChunk());
					// Ensures the chunk is not already claimed
					if (dominionOfChunk == null) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						if (aranarthPlayer.getBalance() >= 5000) {
							if (player.getWorld().getName().startsWith("world")) {
								List<UUID> members = new ArrayList<>();
								members.add(player.getUniqueId());
								Location loc = player.getLocation();
								List<Chunk> chunks = new ArrayList<>();
								chunks.add(player.getLocation().getChunk());

								DominionUtils.createDominion(new Dominion(args[2], player.getUniqueId(), members, loc.getWorld().getName(), chunks, 50, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), 5000));
								Bukkit.broadcastMessage(ChatUtils.chatMessage(AranarthUtils.getNickname(player) + " &7has created the Dominion of &e" + args[2]));
								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1F, 1.5F);
								}
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou can only create a Dominion in Survival!"));
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYou must have at least $5000 to afford this!"));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&e" + dominionOfChunk.getName() + " &calready owns this chunk!"));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou are already in a Dominion!"));
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cThat name is invalid!"));
			}
		}
	}

	/**
	 * Disbands an existing dominion.
	 * @param dominion The dominion being disbanded.
	 * @param player The player attempting to disband the dominion.
	 */
	private static void disbandDominion(Dominion dominion, Player player) {
		if (dominion != null) {
			if (dominion.getOwner().equals(player.getUniqueId())) {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The Dominion of &e" + dominion.getName() + " &7has been disbanded"));
				DominionUtils.disbandDominion(dominion);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cOnly the owner can disband the Dominion!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

}
