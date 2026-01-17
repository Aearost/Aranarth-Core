package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.text.DecimalFormat;
import java.text.NumberFormat;
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
				if (!player.hasPermission("aranarth.dominion.home")) {
					player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this command!"));
					return true;
				}

				teleportToDominionHome(player);
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
				return true;
			}
		} else {
			if (sender instanceof Player player) {
				Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());

				if (args[1].equalsIgnoreCase("create")) {
					if (!player.hasPermission("aranarth.dominion.create")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this command!"));
						return true;
					}
					createDominion(args, player);
				}
				else if (args[1].equalsIgnoreCase("invite")) {
					invitePlayerToDominion(args, dominion, player);
				}
				else if (args[1].equalsIgnoreCase("accept")) {
					acceptDominionInvite(player);
				}
				else if (args[1].equalsIgnoreCase("leave")) {
					leaveDominion(dominion, player);
				}
				else if (args[1].equalsIgnoreCase("remove")) {
					removePlayer(args, dominion, player);
				}
				else if (args[1].equalsIgnoreCase("disband")) {
					disbandDominion(dominion, player);
				}
				else if (args[1].equalsIgnoreCase("claim")) {
					player.sendMessage(ChatUtils.chatMessage(DominionUtils.claimChunk(player, player.getChunk())));
				}
				else if (args[1].equalsIgnoreCase("unclaim")) {
					player.sendMessage(ChatUtils.chatMessage(DominionUtils.unclaimChunk(player)));
				}
				else if (args[1].equalsIgnoreCase("balance")) {
					if (dominion != null) {
						NumberFormat formatter = NumberFormat.getCurrencyInstance();
						String valueWithTwoDecimals = formatter.format(dominion.getBalance());
						player.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + "&7's balance is &6" + valueWithTwoDecimals));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
					}
				}
				else if (args[1].equalsIgnoreCase("deposit")) {
					if (dominion != null) {
						depositToDominion(args, dominion, player);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
					}
				}
				else if (args[1].equalsIgnoreCase("withdraw")) {
					if (dominion != null) {
						withdrawFromDominion(args, dominion, player);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
					}
				}
				else if (args[1].equalsIgnoreCase("home")) {
					if (!player.hasPermission("aranarth.dominion.home")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this command!"));
						return true;
					}
					teleportToDominionHome(player);
				}
				else if (args[1].equalsIgnoreCase("sethome")) {
					updateDominionHome(dominion, player);
				}
				else if (args[1].equalsIgnoreCase("who")) {
					getDominionWho(args, player);
				}
				else if (args[1].equalsIgnoreCase("list")) {
					int i = 0;
					for (Dominion dominionFromList : DominionUtils.getDominions()) {
						i++;
						player.sendMessage(ChatUtils.translateToColor("&7" + i + ". &e" + dominionFromList.getName() + "&7, ruled by &e"
								+ AranarthUtils.getNickname(Bukkit.getOfflinePlayer(dominionFromList.getLeader()))
								+ " &7- &e" + dominionFromList.getChunks().size() + " chunks &7- &6$" + dominionFromList.getBalance()));
					}
				}
				else if (args[1].equalsIgnoreCase("info")) {
					if (args.length == 2) {
						if (dominion != null) {
							displayInfoForDominion(player, dominion);
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
						}
					} else {
						StringBuilder dominionNameBuilder = new StringBuilder();
						for (int i = 2; i < args.length; i++) {
							dominionNameBuilder.append(args[i]);
							if (i < args.length - 1) {
								dominionNameBuilder.append(" ");
							}
						}

						List<Dominion> dominions = DominionUtils.getDominions();
						boolean wasDominionFound = false;
						for (Dominion dominionFromList : dominions) {
							if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
								displayInfoForDominion(player, dominionFromList);
								wasDominionFound = true;
								return true;
							}
						}

						if (!wasDominionFound) {
							player.sendMessage(ChatUtils.chatMessage("&cThat dominion could not be found!"));
						}
					}
				}
				else if (args[1].equalsIgnoreCase("rename")) {
					if (dominion != null) {
						if (dominion.getLeader().equals(player.getUniqueId())) {
							String dominionName = verifyDominionName(args, player);
							if (dominionName != null) {
								String oldName = dominion.getName();
								dominion.setName(dominionName);
								DominionUtils.updateDominion(dominion);
								Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The Dominion of &e" + oldName + " &7has been renamed to &e" + dominionName));
								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.5F);
								}
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cOnly the owner can disband the Dominion!"));
						}
					}
				} else if (args[1].equalsIgnoreCase("ally")) {
					allyDominion(args, dominion, player);
				} else if (args[1].equalsIgnoreCase("truce")) {
					truceDominion(args, dominion, player);
				} else if (args[1].equalsIgnoreCase("enemy")) {
					enemyDominion(args, dominion, player);
				} else if (args[1].equalsIgnoreCase("neutral")) {
					neutralDominion(args, dominion, player);
				} else if (args[1].equalsIgnoreCase("setleader")) {
					setLeader(args, dominion, player);
				} else if (args[1].equalsIgnoreCase("map")) {
					showDominionMap(player);
				} else if (args[1].equalsIgnoreCase("autoclaim")) {
					claimToggle(player);
				}
				else {
					player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac dominion <command>"));
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			}
			return true;
		}
	}

	/**
	 * Teleports the player to their dominion's home.
	 * @param player The player.
	 */
	private static void teleportToDominionHome(Player player) {
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		if (dominion != null) {
			if (player.hasPermission("aranarth.dominion.home")) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), dominion.getDominionHome());
				player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + dominion.getName()));

				PermissionAttachment perms = player.addAttachment(AranarthCore.getInstance());
				perms.setPermission("worldedit.*", false);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot teleport to your Dominion!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Creates a new dominion.
	 *
	 * @param args The arguments of the command.
	 * @param player The player that executed the command.
	 */
	private static void createDominion(String[] args, Player player) {
		if (player.hasPermission("aranarth.dominion.create")) {
			if (args.length >= 3) {
				String dominionName = verifyDominionName(args, player);
				if (dominionName == null) {
					return;
				}

				// Ensures the player is not in a dominion
				if (DominionUtils.getPlayerDominion(player.getUniqueId()) == null) {
					Dominion dominionOfChunk = DominionUtils.getDominionOfChunk(player.getLocation().getChunk());
					// Ensures the chunk is not already claimed
					if (dominionOfChunk == null) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						if (aranarthPlayer.getBalance() >= 5000) {
							if (player.getWorld().getName().startsWith("world")) {
								if (AranarthUtils.isSpawnLocation(player.getLocation())) {
									player.sendMessage(ChatUtils.chatMessage("&cYou cannot create a Dominion here!"));
									return;
								}

								List<UUID> members = new ArrayList<>();
								members.add(player.getUniqueId());
								List<UUID> allies = new ArrayList<>();
								List<UUID> truced = new ArrayList<>();
								List<UUID> enemies = new ArrayList<>();

								Location loc = AranarthUtils.getSafeTeleportLocation(player.getLocation());
								if (loc == null) {
									player.sendMessage(ChatUtils.chatMessage("&cThe Dominion home could not be set here!"));
									return;
								}
								List<Chunk> chunks = new ArrayList<>();
								chunks.add(player.getLocation().getChunk());
								aranarthPlayer.setBalance(aranarthPlayer.getBalance() - 5000);

								DominionUtils.createDominion(new Dominion(dominionName, player.getUniqueId(), members, allies, truced, enemies, loc.getWorld().getName(), chunks, 50, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), 5000));
								Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getNickname(player) + " &7has created the Dominion of &e" + dominionName));
								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.5F);
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
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac dominion create <name>"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to create a Dominion!"));
		}
	}

	/**
	 * Disbands an existing dominion.
	 * @param dominion The dominion being disbanded.
	 * @param player The player attempting to disband the dominion.
	 */
	private static void disbandDominion(Dominion dominion, Player player) {
		if (dominion != null) {
			if (dominion.getLeader().equals(player.getUniqueId())) {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The Dominion of &e" + dominion.getName() + " &7has been disbanded"));
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setBalance(aranarthPlayer.getBalance() + dominion.getBalance());
				player.sendMessage(ChatUtils.chatMessage("&7Your Dominion's balance has been added to your own"));
				updateDominionLeader(dominion, null, true);

				for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
					onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.5F);
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cOnly the owner can disband the Dominion!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Provides the dominion name of the input name in the command arguments.
	 * @param args The arguments of the command.
	 * @param player The player who executed the command.
	 */
	private static void getDominionWho(String[] args, Player player) {
		if (args.length == 2) {
			Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
			if (dominion != null) {
				player.sendMessage(ChatUtils.chatMessage("&7You are in the Dominion of &e" + dominion.getName()));
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
			}
			return;
		}

		if (!args[2].isEmpty()) {
			UUID uuid = AranarthUtils.getUUIDFromUsername(args[2]);
			if (uuid == null) {
				player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + " &ccould not be found!"));
				return;
			}

			if (uuid.equals(player.getUniqueId())) {
				Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
				if (dominion != null) {
					player.sendMessage(ChatUtils.chatMessage("&7You are in the Dominion of &e" + dominion.getName()));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
				}
				return;
			}

			if (uuid != null) {
				Dominion searchedPlayerDominion = DominionUtils.getPlayerDominion(uuid);
				if (searchedPlayerDominion != null) {
					player.sendMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getPlayer(uuid).getNickname() + "&7 is in the Dominion of &e" + searchedPlayerDominion.getName()));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getPlayer(uuid).getNickname() + "&7 is not in a Dominion!"));
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + " &ccould not be found!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou must enter a player's username!"));
		}
	}

	/**
	 * Updates the home of the dominion to the player's current location.
	 * @param dominion The dominion of the player.
	 * @param player The player executing the command.
	 */
	private static void updateDominionHome(Dominion dominion, Player player) {
		if (dominion.getLeader().equals(player.getUniqueId())) {
			List<Chunk> chunks = dominion.getChunks();
			if (chunks.contains(player.getLocation().getChunk())) {
				Location loc = AranarthUtils.getSafeTeleportLocation(player.getLocation());
				if (loc == null) {
					player.sendMessage(ChatUtils.chatMessage("&cYou cannot set the dominion's home here!"));
					return;
				}
				dominion.setDominionHome(loc);
				DominionUtils.updateDominion(dominion);
				player.sendMessage(ChatUtils.chatMessage("&7Your Dominion's home has been updated"));
				player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 0.5F);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou can only do this in your Dominion's land!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of the Dominion can do this!"));
		}
	}

	/**
	 * Adds the input player to the dominion.
	 * @param args The arguments of the command.
	 * @param dominion The dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void invitePlayerToDominion(String[] args, Dominion dominion, Player player) {
		if (args.length == 2) {
			player.sendMessage(ChatUtils.chatMessage("&cPlease specify the player to add"));
			return;
		} else {
			if (dominion == null) {
				player.sendMessage(ChatUtils.chatMessage("&cYou are not currently in a Dominion!"));
				return;
			}

			if (dominion.getLeader().equals(player.getUniqueId())) {
				UUID inputUuid = AranarthUtils.getUUIDFromUsername(args[2]);
				if (inputUuid == null) {
					player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + " &ccould not be found!"));
					return;
				}
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(inputUuid);
				Dominion inputDominion = DominionUtils.getPlayerDominion(inputUuid);

				// If the player is not already in a Dominion
				if (inputDominion == null) {
					aranarthPlayer.setPendingDominion(dominion);
					AranarthUtils.setPlayer(inputUuid, aranarthPlayer);
					player.sendMessage(ChatUtils.chatMessage("&7An invitation has been sent to &e" + aranarthPlayer.getNickname()));
					Player invitedPlayer = Bukkit.getPlayer(inputUuid);
					if (invitedPlayer != null) {
						if (invitedPlayer.isOnline()) {
							invitedPlayer.sendMessage(ChatUtils.chatMessage("&7You have been invited to join &e" + dominion.getName()));
							invitedPlayer.sendMessage(ChatUtils.chatMessage("&7Use &e/ac dominion accept &7to join!"));
						}
					}
				} else {
					if (inputDominion.getLeader().equals(dominion.getLeader())) {
						player.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &cis already in your Dominion!"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &cis already in &e" + dominion.getName()));
					}
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of the Dominion can do this!"));
			}
		}
	}

	/**
	 * Allows the player to accept a pending Dominion invitation.
	 * @param player The player executing the command.
	 */
	private static void acceptDominionInvite(Player player) {
		Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		// Trying to join a Dominion when already in one
		if (playerDominion != null) {
			Bukkit.getLogger().info("Dominion: " + playerDominion.getName());
			player.sendMessage(ChatUtils.chatMessage("&cYou must leave your current Dominion first!"));
			return;
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		Dominion dominion = aranarthPlayer.getPendingDominion();
		// If the player has a pending Dominion invitation
		if (dominion != null) {
			dominion.getMembers().add(player.getUniqueId());
			DominionUtils.updateDominion(dominion);

			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has joined the Dominion!"));
					onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1.2F);
				}
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou do not have a Dominion invitation!"));
		}
	}

	/**
	 * Allows the player to leave their current Dominion.
	 * @param dominion The Dominion.
	 * @param player The player executing the command.
	 */
	private static void leaveDominion(Dominion dominion, Player player) {
		if (dominion != null) {
			if (dominion.getLeader().equals(player.getUniqueId())) {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot leave your own Dominion!"));
				return;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			dominion.getMembers().remove(player.getUniqueId());
			DominionUtils.updateDominion(dominion);
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has left the Dominion!"));
				}
			}
			player.sendMessage(ChatUtils.chatMessage("&7You have left the Dominion of &e" + dominion.getName()));
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Removes the specified Player from the Dominion.
	 * @param args The command arguments.
	 * @param dominion The Dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void removePlayer(String[] args, Dominion dominion, Player player) {
		if (args.length == 2) {
			player.sendMessage(ChatUtils.chatMessage("&cPlease specify the player to remove"));
			return;
		}

		if (dominion != null) {
			if (dominion.getLeader().equals(player.getUniqueId())) {
				if (player.getName().equalsIgnoreCase(args[2])) {
					player.sendMessage(ChatUtils.chatMessage("&cYou cannot remove yourself from your Dominion!"));
					return;
				}

				UUID inputUuid = AranarthUtils.getUUIDFromUsername(args[2]);
				if (inputUuid != null) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(inputUuid);
					dominion.getMembers().remove(inputUuid);
					DominionUtils.updateDominion(dominion);
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
							onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has been removed from the Dominion!"));
						}
					}

					Player removedPlayer = Bukkit.getPlayer(inputUuid);
					if (removedPlayer.isOnline()) {
						removedPlayer.sendMessage(ChatUtils.chatMessage("&7You have been removed from the Dominion of &e" + dominion.getName()));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + args[2] + " &ccould not be found"));
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of the Dominion can do this!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Handles sending or accepting an alliance request with another Dominion.
	 * @param args The arguments of the command.
	 * @param dominion The dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void allyDominion(String[] args, Dominion dominion, Player player) {
		if (dominion != null) {
			StringBuilder dominionNameBuilder = new StringBuilder();
			for (int i = 2; i < args.length; i++) {
				dominionNameBuilder.append(args[i]);
				if (i < args.length - 1) {
					dominionNameBuilder.append(" ");
				}
			}

			List<Dominion> dominions = DominionUtils.getDominions();
			boolean wasDominionFound = false;
			for (Dominion dominionFromList : dominions) {
				if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
					if (dominion.getLeader().equals(dominionFromList.getLeader())) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot &5Ally &cyour own Dominion!"));
						return;
					}

					wasDominionFound = true;
					if (dominion.getLeader().equals(player.getUniqueId())) {
						if (dominionFromList.getAllianceRequests().contains(dominion.getLeader())) {
							player.sendMessage(ChatUtils.chatMessage("&cYour Dominion has already sent an &5Alliance &crequest to &e" + dominionFromList.getName()));
							return;
						}

						boolean wasAllied = DominionUtils.areAllied(dominion, dominionFromList);
						boolean wasTruced = DominionUtils.areTruced(dominion, dominionFromList);
						boolean wasEnemied = DominionUtils.areEnemied(dominion, dominionFromList);

						if (wasAllied) {
							player.sendMessage(ChatUtils.chatMessage("&cYour Dominion is already &5Allied &cwith &e" + dominionFromList.getName()));
							return;
						}

						// If accepting a request for a truce
						if (dominion.getAllianceRequests().contains(dominionFromList.getLeader())) {
							resetDominionRelations(dominion, dominionFromList);

							dominion.getAllied().add(dominionFromList.getLeader());
							dominionFromList.getAllied().add(dominion.getLeader());
							DominionUtils.updateDominion(dominion);
							DominionUtils.updateDominion(dominionFromList);

							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has &5Allied &7with &e" + dominionFromList.getName()));
								} else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has &5Allied &7with &e" + dominion.getName()));
								}
							}
						}
						// If sending a new request for an alliance
						else {
							List<UUID> allianceRequests = dominionFromList.getAllianceRequests();
							allianceRequests.add(dominion.getLeader());
							dominionFromList.setAllianceRequests(allianceRequests);

							DominionUtils.updateDominion(dominionFromList);

							if (wasAllied) {
								player.sendMessage(ChatUtils.chatMessage("&cYour Dominion is already &5Allied &7with &e" + dominionFromList.getName()));
							} else {
								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
										onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has requested an &5Alliance &7with &e" + dominionFromList.getName()));
									} else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
										onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7has requested an &5Alliance &7with your Dominion"));
									}
								}
							}
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of your Dominion can do this!"));
						return;
					}
					break;
				}
			}

			if (!wasDominionFound) {
				player.sendMessage(ChatUtils.chatMessage("&cThat Dominion could not be found!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Handles sending or accepting a truce request with another Dominion.
	 * @param args The arguments of the command.
	 * @param dominion The dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void truceDominion(String[] args, Dominion dominion, Player player) {
		if (dominion != null) {
			StringBuilder dominionNameBuilder = new StringBuilder();
			for (int i = 2; i < args.length; i++) {
				dominionNameBuilder.append(args[i]);
				if (i < args.length - 1) {
					dominionNameBuilder.append(" ");
				}
			}

			List<Dominion> dominions = DominionUtils.getDominions();
			boolean wasDominionFound = false;
			for (Dominion dominionFromList : dominions) {
				if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
					if (dominion.getLeader().equals(dominionFromList.getLeader())) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot &dTruce &cyour own Dominion!"));
						return;
					}

					wasDominionFound = true;
					if (dominion.getLeader().equals(player.getUniqueId())) {
						if (dominionFromList.getTruceRequests().contains(dominion.getLeader())) {
							player.sendMessage(ChatUtils.chatMessage("&cYour Dominion has already sent a &dTruce &crequest to &e" + dominionFromList.getName()));
							return;
						}

						boolean wasAllied = DominionUtils.areAllied(dominion, dominionFromList);
						boolean wasTruced = DominionUtils.areTruced(dominion, dominionFromList);
						boolean wasEnemied = DominionUtils.areEnemied(dominion, dominionFromList);

						if (wasTruced) {
							player.sendMessage(ChatUtils.chatMessage("&cYour Dominion is already &dTruced &cwith &e" + dominionFromList.getName()));
							return;
						}

						// If accepting a request for a truce
						if (dominion.getTruceRequests().contains(dominionFromList.getLeader())) {
							resetDominionRelations(dominion, dominionFromList);

							dominion.getTruced().add(dominionFromList.getLeader());
							dominionFromList.getTruced().add(dominion.getLeader());
							DominionUtils.updateDominion(dominion);
							DominionUtils.updateDominion(dominionFromList);

							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has &dTruced &7with &e" + dominionFromList.getName()));
								} else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has &dTruced &7with &e" + dominion.getName()));
								}
							}
						}
						// If sending a new request for a truce
						else {
							List<UUID> truceRequests = dominionFromList.getTruceRequests();
							truceRequests.add(dominion.getLeader());
							dominionFromList.setTruceRequests(truceRequests);

							DominionUtils.updateDominion(dominionFromList);

							if (wasTruced) {
								player.sendMessage(ChatUtils.chatMessage("&cYour Dominion is already &dTruced &7with &e" + dominionFromList.getName()));
							} else {
								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
										onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has requested a &dTruce &7with &e" + dominionFromList.getName()));
									} else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
										onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7has requested a &dTruce &7with your Dominion"));
									}
								}
							}
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of your Dominion can do this!"));
						return;
					}
					break;
				}
			}

			if (!wasDominionFound) {
				player.sendMessage(ChatUtils.chatMessage("&cThat Dominion could not be found!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Handles setting another Dominion as an enemy.
	 * @param args The arguments of the command.
	 * @param dominion The dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void enemyDominion(String[] args, Dominion dominion, Player player) {
		if (dominion != null) {
			StringBuilder dominionNameBuilder = new StringBuilder();
			for (int i = 2; i < args.length; i++) {
				dominionNameBuilder.append(args[i]);
				if (i < args.length - 1) {
					dominionNameBuilder.append(" ");
				}
			}

			List<Dominion> dominions = DominionUtils.getDominions();
			boolean wasDominionFound = false;
			for (Dominion dominionFromList : dominions) {
				if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
					if (dominion.getLeader().equals(dominionFromList.getLeader())) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot Enemy your own Dominion!"));
						return;
					}

					wasDominionFound = true;
					if (dominion.getLeader().equals(player.getUniqueId())) {
						// Enemy the opposing Dominion
						if (!dominion.getEnemied().contains(dominionFromList.getLeader())) {
							dominion.getEnemied().add(dominionFromList.getLeader());
							dominion.getAllied().remove(dominionFromList.getLeader());
							dominion.getTruced().remove(dominionFromList.getLeader());

							DominionUtils.updateDominion(dominion);
							// Only add to the other dominion if they are not already enemies
							if (!dominionFromList.getEnemied().contains(dominion.getLeader())) {
								dominionFromList.getEnemied().add(dominion.getLeader());
								dominionFromList.getAllied().remove(dominion.getLeader());
								dominionFromList.getTruced().remove(dominion.getLeader());
								DominionUtils.updateDominion(dominionFromList);
							}

							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_7, 1F, 0.8F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7has &cEnemied &7your Dominion!"));
								} else if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_7, 1F, 0.8F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has &cEnemied &e" + dominionFromList.getName()));
								}
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYour Dominion is already Enemied with &e" + dominionFromList.getName()));
							return;
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of your Dominion can do this!"));
						return;
					}
					break;
				}
			}

			if (!wasDominionFound) {
				player.sendMessage(ChatUtils.chatMessage("&cThat Dominion could not be found!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Handles setting another Dominion as neutral.
	 * @param args The arguments of the command.
	 * @param dominion The dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void neutralDominion(String[] args, Dominion dominion, Player player) {
		if (dominion != null) {
			StringBuilder dominionNameBuilder = new StringBuilder();
			for (int i = 2; i < args.length; i++) {
				dominionNameBuilder.append(args[i]);
				if (i < args.length - 1) {
					dominionNameBuilder.append(" ");
				}
			}

			List<Dominion> dominions = DominionUtils.getDominions();
			boolean wasDominionFound = false;
			for (Dominion dominionFromList : dominions) {
				if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
					if (dominion.getLeader().equals(dominionFromList.getLeader())) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot request &fNeutrality &7with your own Dominion!"));
						return;
					}

					wasDominionFound = true;
					if (dominion.getLeader().equals(player.getUniqueId())) {
						if (dominionFromList.getNeutralRequests().contains(dominion.getLeader())) {
							player.sendMessage(ChatUtils.chatMessage("&cYour Dominion has already sent a &fNeutrality &crequest to &e" + dominionFromList.getName()));
							return;
						}

						boolean wasAllied = DominionUtils.areAllied(dominion, dominionFromList);
						boolean wasTruced = DominionUtils.areTruced(dominion, dominionFromList);
						boolean wasEnemied = DominionUtils.areEnemied(dominion, dominionFromList);

						// If accepting a request for neutrality (only when currently enemied)
						if (wasEnemied && dominion.getNeutralRequests().contains(dominionFromList.getLeader())) {
							resetDominionRelations(dominion, dominionFromList);

							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominion.getMembers().contains(onlinePlayer.getUniqueId())
										|| dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has become &fNeutral &7with &e" + dominionFromList.getName()));
								}
							}
						}
						// If sending a new request for neutrality
						else {
							if (wasEnemied) {
								List<UUID> neutralRequests = dominionFromList.getNeutralRequests();
								neutralRequests.add(dominion.getLeader());
								dominionFromList.setNeutralRequests(neutralRequests);

								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
										onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has requested &fNeutrality &7with &e" + dominionFromList.getName()));
									} else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
										onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7has requested &fNeutrality &7with your Dominion"));
									}
								}
							} else if (wasAllied || wasTruced) {
								resetDominionRelations(dominion, dominionFromList);

								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									if (dominion.getMembers().contains(onlinePlayer.getUniqueId())
											|| dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
										onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has become &fNeutral &7with &e" + dominionFromList.getName()));
									}
								}
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYour Dominion is already &fNeutral &7with &e" + dominionFromList.getName()));
								return;
							}

							DominionUtils.updateDominion(dominion);
							DominionUtils.updateDominion(dominionFromList);
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of your Dominion can do this!"));
						return;
					}
					break;
				}
			}

			if (!wasDominionFound) {
				player.sendMessage(ChatUtils.chatMessage("&cThat Dominion could not be found!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
		}
	}

	/**
	 * Resets the relations between the two Dominions.
	 * @param dominion1 The first Dominion.
	 * @param dominion2 The second Dominion.
	 */
	private static void resetDominionRelations(Dominion dominion1, Dominion dominion2) {
		dominion1.getAllianceRequests().remove(dominion2.getLeader());
		dominion1.getTruceRequests().remove(dominion2.getLeader());
		dominion1.getNeutralRequests().remove(dominion2.getLeader());
		dominion1.getAllied().remove(dominion2.getLeader());
		dominion1.getTruced().remove(dominion2.getLeader());
		dominion1.getEnemied().remove(dominion2.getLeader());

		dominion2.getAllianceRequests().remove(dominion1.getLeader());
		dominion2.getTruceRequests().remove(dominion1.getLeader());
		dominion2.getNeutralRequests().remove(dominion1.getLeader());
		dominion2.getAllied().remove(dominion1.getLeader());
		dominion2.getTruced().remove(dominion1.getLeader());
		dominion2.getEnemied().remove(dominion1.getLeader());

		DominionUtils.updateDominion(dominion1);
		DominionUtils.updateDominion(dominion2);
	}

	/**
	 * Handles updating the leader of the Dominion.
	 * @param args The arguments of the command.
	 * @param dominion The dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void setLeader(String[] args, Dominion dominion, Player player) {
		if (args.length >= 3) {
			if (dominion != null) {
				if (dominion.getLeader().equals(player.getUniqueId())) {
					UUID uuid = AranarthUtils.getUUIDFromUsername(args[2]);
					if (uuid != null) {
						OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
						Dominion offlinePlayerDominion = DominionUtils.getPlayerDominion(uuid);
						if (offlinePlayerDominion != null && dominion.getLeader().equals(offlinePlayerDominion.getLeader())) {
							if (!uuid.equals(dominion.getLeader())) {
								AranarthPlayer oldLeader = AranarthUtils.getPlayer(player.getUniqueId());
								AranarthPlayer newLeader = AranarthUtils.getPlayer(uuid);
								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
										onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + oldLeader.getNickname() + " &7has transferred ownership of &e" + dominion.getName() + " &7to &e" + newLeader.getNickname()));
									}
								}
								updateDominionLeader(dominion, uuid, false);
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou cannot set yourself as the new leader!"));
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cThat player is not in your Dominion!"));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThat player does not exist!"));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of your Dominion can do this!"));
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac dominion setleader <username>"));
		}
	}

	/**
	 * Updates the leader of the Dominion by updating all references to the Dominion's Leader.
	 * @param dominionBeingUpdated The Dominion that is being updated.
	 * @param newLeader The UUID of the new leader of the Dominion.
	 * @param isDeleting If the Dominion is being deleted.
	 */
	private static void updateDominionLeader(Dominion dominionBeingUpdated, UUID newLeader, boolean isDeleting) {
		UUID oldLeader = dominionBeingUpdated.getLeader();
		for (Dominion dominion : DominionUtils.getDominions()) {
			for (int i = 0; i < dominion.getAllianceRequests().size(); i++) {
				if (dominion.getAllianceRequests().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getAllianceRequests().set(i, newLeader);
					} else {
						dominion.getAllianceRequests().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getTruceRequests().size(); i++) {
				if (dominion.getTruceRequests().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getTruceRequests().set(i, newLeader);
					} else {
						dominion.getTruceRequests().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getNeutralRequests().size(); i++) {
				if (dominion.getNeutralRequests().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getNeutralRequests().set(i, newLeader);
					} else {
						dominion.getNeutralRequests().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getAllied().size(); i++) {
				if (dominion.getAllied().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getAllied().set(i, newLeader);
					} else {
						dominion.getAllied().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getTruced().size(); i++) {
				if (dominion.getTruced().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getTruced().set(i, newLeader);
					} else {
						dominion.getTruced().remove(oldLeader);
					}
					break;
				}
			}
			for (int i = 0; i < dominion.getEnemied().size(); i++) {
				if (dominion.getEnemied().get(i).equals(oldLeader)) {
					if (!isDeleting) {
						dominion.getEnemied().set(i, newLeader);
					} else {
						dominion.getEnemied().remove(oldLeader);
					}
					break;
				}
			}
			DominionUtils.updateDominion(dominion);
		}
		if (!isDeleting) {
			dominionBeingUpdated.setLeader(newLeader);
			DominionUtils.updateDominion(dominionBeingUpdated);
		} else {
			DominionUtils.disbandDominion(dominionBeingUpdated);
		}
	}

	/**
	 * Displays the info for the input dominion.
	 * @param player The player who executed the command.
	 * @param dominion The dominion to display the info for.
	 */
	private static void displayInfoForDominion(Player player, Dominion dominion) {
		player.sendMessage(ChatUtils.translateToColor("&6&l---------------------------------"));
		player.sendMessage(ChatUtils.translateToColor("&8&lThe Dominion of &e" + dominion.getName()));

		AranarthPlayer leader = AranarthUtils.getPlayer(dominion.getLeader());
		String leaderDisplayedName = "";
		leaderDisplayedName += AranarthUtils.getSaintRank(leader);
		leaderDisplayedName += AranarthUtils.getArchitectRank(leader);
		leaderDisplayedName += AranarthUtils.getCouncilRank(leader);
		leaderDisplayedName += leader.getNickname();

		player.sendMessage(ChatUtils.translateToColor("&7Leader: &e" + leaderDisplayedName));

		StringBuilder membersBuilder = new StringBuilder();
		membersBuilder.append("&7Members: &e");
		// If the only member is the ruler
		if (dominion.getMembers().size() == 1) {
			membersBuilder.append("&7&oNone");
		} else {
			for (int i = 0; i < dominion.getMembers().size(); i++) {
				if (dominion.getMembers().get(i).equals(dominion.getLeader())) {
					continue;
				}

				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(dominion.getMembers().get(i));
				String displayedName = "";
				displayedName += AranarthUtils.getSaintRank(aranarthPlayer);
				displayedName += AranarthUtils.getArchitectRank(aranarthPlayer);
				displayedName += AranarthUtils.getCouncilRank(aranarthPlayer);
				displayedName += aranarthPlayer.getNickname();

				membersBuilder.append(displayedName);
				if (i < dominion.getMembers().size() - 1) {
					membersBuilder.append("&e, ");
				}
			}
		}
		player.sendMessage(ChatUtils.translateToColor(membersBuilder.toString()));

		StringBuilder alliesBuilder = new StringBuilder();
		alliesBuilder.append("&7Allies: ");
		if (dominion.getAllied().isEmpty()) {
			alliesBuilder.append("&7&oNone");
		} else {
			for (int i = 0; i < dominion.getAllied().size(); i++) {
				UUID uuid = dominion.getAllied().get(i);
				Dominion alliedDominion = DominionUtils.getPlayerDominion(uuid);
				if (DominionUtils.areAllied(dominion, alliedDominion)) {
					alliesBuilder.append("&5").append(alliedDominion.getName());
					if (i < dominion.getAllied().size() - 1) {
						alliesBuilder.append("&5, ");
					}
				}
			}
		}
		player.sendMessage(ChatUtils.translateToColor(alliesBuilder.toString()));

		StringBuilder trucedBuilder = new StringBuilder();
		trucedBuilder.append("&7Truced: ");
		if (dominion.getTruced().isEmpty()) {
			trucedBuilder.append("&7&oNone");
		} else {
			for (int i = 0; i < dominion.getTruced().size(); i++) {
				UUID uuid = dominion.getTruced().get(i);
				Dominion trucedDominion = DominionUtils.getPlayerDominion(uuid);
				if (DominionUtils.areTruced(dominion, trucedDominion)) {
					trucedBuilder.append("&d").append(trucedDominion.getName());
					if (i < dominion.getTruced().size() - 1) {
						trucedBuilder.append("&d, ");
					}
				}
			}
		}
		player.sendMessage(ChatUtils.translateToColor(trucedBuilder.toString()));

		StringBuilder enemyBuilder = new StringBuilder();
		enemyBuilder.append("&7Enemies: ");
		// Must search all Dominions as it is based on others as well
		List<UUID> leadersEnemiedToThisDominion = new ArrayList<>();
		for (Dominion otherDominion : DominionUtils.getDominions()) {
			if (otherDominion.getEnemied().contains(dominion.getLeader())) {
				leadersEnemiedToThisDominion.add(otherDominion.getLeader());
			}
		}
		for (UUID enemied : dominion.getEnemied()) {
			if (!leadersEnemiedToThisDominion.contains(enemied)) {
				leadersEnemiedToThisDominion.add(enemied);
			}
		}

		if (leadersEnemiedToThisDominion.isEmpty()) {
			enemyBuilder.append("&7&oNone");
		} else {
			for (int i = 0; i < leadersEnemiedToThisDominion.size(); i++) {
				Dominion enemiedDominion = DominionUtils.getPlayerDominion(leadersEnemiedToThisDominion.get(i));
				enemyBuilder.append("&c").append(enemiedDominion.getName());
				if (i < dominion.getEnemied().size() - 1) {
					enemyBuilder.append("&c, ");
				}
			}
		}
		player.sendMessage(ChatUtils.translateToColor(enemyBuilder.toString()));

		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		String valueWithTwoDecimals = formatter.format(dominion.getBalance());
		player.sendMessage(ChatUtils.translateToColor("&7Balance: &6" + valueWithTwoDecimals));
		player.sendMessage(ChatUtils.translateToColor("&7Size: &e" + dominion.getChunks().size() + "/" + (dominion.getMembers().size() * 25) + " chunks"));
		player.sendMessage(ChatUtils.translateToColor("&6&l---------------------------------"));
	}

	/**
	 * Deposit money from the player's balance to the Dominion.
	 * @param args The command arguments.
	 * @param dominion The Dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void depositToDominion(String[] args, Dominion dominion, Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (args.length >= 3) {
			double amount = 0;
			try {
				DecimalFormat df = new DecimalFormat("0.00");
				amount = Double.parseDouble(args[2]);
				String valueWithTwoDecimals = df.format(amount);
				double trimmedAmount = Double.parseDouble(valueWithTwoDecimals);
				dominion.setBalance(dominion.getBalance() + trimmedAmount);
				DominionUtils.updateDominion(dominion);
				NumberFormat formatter = NumberFormat.getCurrencyInstance();
				player.sendMessage(ChatUtils.chatMessage("&7You have deposited &6" + formatter.format(trimmedAmount) + " &7to your Dominion"));
				for (UUID uuid : dominion.getMembers()) {
					if (!uuid.equals(player.getUniqueId())) {
						if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
							Player member = Bukkit.getPlayer(uuid);
							member.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has deposited &6" + formatter.format(trimmedAmount) + " &7to your Dominion"));
						}
					}
				}
			} catch (NumberFormatException e) {
				player.sendMessage(ChatUtils.chatMessage("&cThat amount is invalid!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac dominion deposit <amount>"));
		}
	}

	/**
	 * Withdraw money from the dominion's balance.
	 * @param args The command arguments.
	 * @param dominion The Dominion of the player executing the command.
	 * @param player The player executing the command.
	 */
	private static void withdrawFromDominion(String[] args, Dominion dominion, Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (args.length >= 3) {
			double amount = 0;
			if (player.getUniqueId().equals(dominion.getLeader())) {
				try {
					DecimalFormat df = new DecimalFormat("0.00");
					amount = Double.parseDouble(args[2]);
					String valueWithTwoDecimals = df.format(amount);
					double trimmedAmount = Double.parseDouble(valueWithTwoDecimals);
					dominion.setBalance(dominion.getBalance() + trimmedAmount);
					DominionUtils.updateDominion(dominion);
					NumberFormat formatter = NumberFormat.getCurrencyInstance();
					player.sendMessage(ChatUtils.chatMessage("&7You have withdrawn &6" + formatter.format(trimmedAmount) + " &7from your Dominion"));
					for (UUID uuid : dominion.getMembers()) {
						if (!uuid.equals(player.getUniqueId())) {
							if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
								Player member = Bukkit.getPlayer(uuid);
								member.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has withdrawn &6" + formatter.format(trimmedAmount) + " &7from your Dominion"));
							}
						}
					}
				} catch (NumberFormatException e) {
					player.sendMessage(ChatUtils.chatMessage("&cThat amount is invalid!"));
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of the Dominion can make withdrawals!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac dominion withdraw <amount>"));
		}
	}

	/**
	 * Verifies that the input Dominion name is valid.
	 * @param args The arguments of the command.
	 * @param player The player executing the command.
	 * @return The Dominion's name.
	 */
	private static String verifyDominionName(String[] args, Player player) {
		StringBuilder parts = new StringBuilder();
		for (int i = 2; i < args.length; i++) {
			if (i == args.length - 1) {
				parts.append(args[i]);
			} else {
				parts.append(args[i]).append(" ");
			}
		}
		String dominionName = parts.toString();
		if (ChatUtils.stripColorFormatting(dominionName).length() > 30) {
			player.sendMessage(ChatUtils.chatMessage("&cThat Dominion name is too long!"));
			return null;
		}

		if (player.hasPermission("aranarth.chat.hex")) {
			dominionName = ChatUtils.translateToColor(dominionName);
		} else if (player.hasPermission("aranarth.chat.color")) {
			dominionName = ChatUtils.playerColorChat(dominionName);
			if (dominionName == null) {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this kind of formatting!"));
				return null;
			}
		}

		dominionName = ChatUtils.removeSpecialCharacters(dominionName);

		for (Dominion dominionInList : DominionUtils.getDominions()) {
			if (ChatUtils.stripColorFormatting(dominionInList.getName()).equalsIgnoreCase(ChatUtils.stripColorFormatting(dominionName))) {
				player.sendMessage(ChatUtils.chatMessage("&cThis name is already used by another Dominion!"));
				return null;
			}
		}
		return dominionName;
	}

	/**
	 * Displays a map of the chunks nearby the player, highlighting Dominion Chunks.
	 * @param player The player executing the command.
	 */
	private static void showDominionMap(Player player) {
		Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		Chunk playerChunk = player.getLocation().getChunk();

		// Creates base empty map
		String[][] map = new String[15][15];
		for (int i = 0; i < 15; i++) {
			for (int j = 0; j < 15; j++) {
				map[i][j] = "&7[] ";
			}
		}

		// To track what Dominions are in which chunks
		Chunk[][] chunks = new Chunk[15][15];
		int topX = playerChunk.getX() - 7;
		int topZ = playerChunk.getZ() - 7;
		List<Dominion> dominionsNearby = new ArrayList<>();
		boolean hasSpawnChunks = false;

		player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lDominion Map &8- - -"));
		// Iterate row by row, instead of by column
		for (int z = 0; z < 15; z++) {
			String[] line = map[z];
			StringBuilder lineBuilder = new StringBuilder();
			for (int x = 0; x < 15; x++) {
				// Verifies if the chunk is owned by a Dominion and colors it based on the relation with the Dominion
				Chunk chunk = player.getWorld().getChunkAt(topX + x, topZ + z);
				Dominion chunkDominion = DominionUtils.getDominionOfChunk(chunk);
				if (chunkDominion != null) {
					if (!dominionsNearby.contains(chunkDominion)) {
						dominionsNearby.add(chunkDominion);
					}

					if (playerDominion != null) {
						if (DominionUtils.areAllied(playerDominion, chunkDominion)) {
							line[x] = "&5[] ";
						} else if (DominionUtils.areTruced(playerDominion, chunkDominion)) {
							line[x] = "&d[] ";
						} else if (DominionUtils.areEnemied(playerDominion, chunkDominion)) {
							line[x] = "&c[] ";
						} else if (playerDominion.getLeader().equals(chunkDominion.getLeader())) {
							line[x] = "&a[] ";
						} else {
							line[x] = "&f[] ";
						}
					} else {
						line[x] = "&f[] ";
					}
				} else {
					int chunkBaseX = chunk.getX() * 16;
					int chunkBaseZ = chunk.getZ() * 16;
					if (AranarthUtils.isSpawnLocation(new Location(player.getWorld(), chunkBaseX, player.getY(), chunkBaseZ))) {
						hasSpawnChunks = true;
						line[x] = "&6[] ";
					}
				}

				// Highlights the chunk the player is currently in
				if (x == 7 && z == 7) {
					map[7][7] = "&e[] ";
				}

				lineBuilder.append(line[x]);

				if (x == 14) {
					lineBuilder.append("\n");
				}
			}
			player.sendMessage(ChatUtils.translateToColor(lineBuilder.toString()));
		}
		player.sendMessage(ChatUtils.translateToColor("&e[] &7- Your Location"));
		if (hasSpawnChunks) {
			player.sendMessage(ChatUtils.translateToColor("&6[] &7- Spawn"));
		}

		if (playerDominion != null && dominionsNearby.contains(playerDominion)) {
			player.sendMessage(ChatUtils.translateToColor("&a[] &7- Your Dominion"));
			dominionsNearby.remove(playerDominion);
		}

		for (Dominion nearbyDominion : dominionsNearby) {
			if (playerDominion != null) {
				if (DominionUtils.areAllied(playerDominion, nearbyDominion)) {
					player.sendMessage(ChatUtils.translateToColor("&5[] &7- &e" + nearbyDominion.getName()));
				} else if (DominionUtils.areTruced(playerDominion, nearbyDominion)) {
					player.sendMessage(ChatUtils.translateToColor("&d[] &7- &e" + nearbyDominion.getName()));
				} else if (DominionUtils.areEnemied(playerDominion, nearbyDominion)) {
					player.sendMessage(ChatUtils.translateToColor("&c[] &7- &e" + nearbyDominion.getName()));
				} else {
					player.sendMessage(ChatUtils.translateToColor("&f[] &7- &e" + nearbyDominion.getName()));
				}
			} else {
				player.sendMessage(ChatUtils.translateToColor("&f[] &7- &e" + nearbyDominion.getName()));
			}

		}
	}

	/**
	 * Toggles whether the player will automatically be claiming chunks as they enter them.
	 * @param player The player.
	 */
	private static void claimToggle(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.isAutoClaimEnabled()) {
			aranarthPlayer.setAutoClaimEnabled(false);
			player.sendMessage(ChatUtils.chatMessage("&7You have disabled auto-claim"));
		} else {
			aranarthPlayer.setAutoClaimEnabled(true);
			player.sendMessage(ChatUtils.chatMessage("&7You have enabled auto-claim"));
		}
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
	}

}
