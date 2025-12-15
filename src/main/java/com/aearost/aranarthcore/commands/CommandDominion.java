package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
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
					player.sendMessage(ChatUtils.chatMessage(DominionUtils.claimChunk(player)));
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
									onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.5F);
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
									onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.5F);
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
				DominionUtils.disbandDominion(dominion);

				for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
					onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.5F);
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
					onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1.2F);
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
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot ally yourself!"));
						return;
					}

					wasDominionFound = true;
					if (dominion.getLeader().equals(player.getUniqueId())) {
						// Send/accept an alliance request
						if (!dominion.getAllied().contains(dominionFromList.getLeader())) {
							dominion.getAllied().add(dominionFromList.getLeader());
							DominionUtils.updateDominion(dominion);
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.8F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7has requested an &5Alliance &7with &e" + dominionFromList.getName()));
								} else if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.8F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has requested an &5Alliance &7with &e" + dominionFromList.getName()));
								}
							}
						} else {
							// If they are both marked as allies
							if (dominionFromList.getAllied().contains(dominion.getLeader())) {
								player.sendMessage(ChatUtils.chatMessage("&cYou are already &5Allied &7with &e" + dominionFromList.getName()));
							}
							// Only a request has been sent
							else {
								player.sendMessage(ChatUtils.chatMessage("&cYou have already sent an &5Alliance &7request to &e" + dominionFromList.getName()));
							}
							return;
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of the Dominion can do this!"));
						return;
					}
					break;
				}
			}

			if (!wasDominionFound) {
				player.sendMessage(ChatUtils.chatMessage("&cThat dominion could not be found!"));
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
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot truce yourself!"));
						return;
					}

					wasDominionFound = true;
					if (dominion.getLeader().equals(player.getUniqueId())) {
						// Send/accept a truce request
						if (!dominion.getTruced().contains(dominionFromList.getLeader())) {
							dominion.getTruced().add(dominionFromList.getLeader());
							DominionUtils.updateDominion(dominion);
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 1.6F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7has requested a &dTruce &7with &e" + dominionFromList.getName()));
								} else if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 1.6F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has requested a &dTruce &7with &e" + dominionFromList.getName()));
								}
							}
						} else {
							// If they are both marked as truced
							if (dominionFromList.getTruced().contains(dominion.getLeader())) {
								player.sendMessage(ChatUtils.chatMessage("&cYou are already &dTruced &7with &e" + dominionFromList.getName()));
							}
							// Only a request has been sent
							else {
								player.sendMessage(ChatUtils.chatMessage("&cYou have already sent a &dTruce &7request to &e" + dominionFromList.getName()));
							}
							return;
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of the Dominion can do this!"));
						return;
					}
					break;
				}
			}

			if (!wasDominionFound) {
				player.sendMessage(ChatUtils.chatMessage("&cThat dominion could not be found!"));
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
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot set yourself as an enemy!"));
						return;
					}

					wasDominionFound = true;
					if (dominion.getLeader().equals(player.getUniqueId())) {
						// Enemy the opposing Dominion
						if (!dominion.getEnemied().contains(dominionFromList.getLeader())) {
							dominion.getEnemied().add(dominionFromList.getLeader());
							DominionUtils.updateDominion(dominion);
							boolean wereAlreadyEnemied = false;
							// Only add to the other dominion if they are not already enemies
							if (!dominionFromList.getEnemied().contains(dominion.getLeader())) {
								dominionFromList.getEnemied().add(dominion.getLeader());
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
							player.sendMessage(ChatUtils.chatMessage("&e" + dominionFromList.getName() + " &cis already an enemy of yours!"));
							return;
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of the Dominion can do this!"));
						return;
					}
					break;
				}
			}

			if (!wasDominionFound) {
				player.sendMessage(ChatUtils.chatMessage("&cThat dominion could not be found!"));
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
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot do this with your own Dominion!"));
						return;
					}

					wasDominionFound = true;
					if (dominion.getLeader().equals(player.getUniqueId())) {
						dominion.getAllied().remove(dominionFromList.getLeader());
						dominion.getTruced().remove(dominionFromList.getLeader());
						dominion.getEnemied().remove(dominionFromList.getLeader());
						DominionUtils.updateDominion(dominion);

						boolean hasBecomeNeutral = false;
						for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
							if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
								onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
								if (dominionFromList.getEnemied().contains(dominion.getLeader())) {
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has requested neutrality with &e" + dominionFromList.getName()));
								} else {
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has become neutral to &e" + dominionFromList.getName()));
									hasBecomeNeutral = true;
								}
							}
						}

						if (hasBecomeNeutral) {
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your Dominion has become neutral to &e" + dominionFromList.getName()));
								}
							}
							return;
						}

						// Forcefully set to neutral
						if (dominionFromList.getAllied().contains(dominion.getLeader()) || dominionFromList.getTruced().contains(dominion.getLeader())) {
							dominionFromList.getAllied().remove(dominion.getLeader());
							dominionFromList.getTruced().remove(dominion.getLeader());
							DominionUtils.updateDominion(dominionFromList);
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7has declared themselves neutral to your Dominion"));
								}
							}
						} else if (dominionFromList.getEnemied().contains(dominion.getLeader())) {
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
									onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, 1F, 0.9F);
									onlinePlayer.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + " &7has requested neutrality with your Dominion"));
								}
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage("&7Your Dominion is neutral to &e" + dominionFromList.getName()));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOnly the leader of the Dominion can do this!"));
						return;
					}
					break;
				}
			}

			if (!wasDominionFound) {
				player.sendMessage(ChatUtils.chatMessage("&cThat dominion could not be found!"));
			}
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
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
		if (dominionName.length() > 30) {
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
}
