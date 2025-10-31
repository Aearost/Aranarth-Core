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
import org.bukkit.potion.PotionEffect;

import java.io.IOException;
import java.text.DecimalFormat;
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
						DecimalFormat df = new DecimalFormat("0.00");
						String valueWithTwoDecimals = df.format(dominion.getBalance());
						player.sendMessage(ChatUtils.chatMessage("&e" + dominion.getName() + "&7's balance is &e$" + valueWithTwoDecimals));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
					}
				}
				else if (args[1].equalsIgnoreCase("home")) {
					teleportToDominionHome(player);
				}
				else if (args[1].equalsIgnoreCase("sethome")) {
					updateDominionHome(dominion, player);
				}
				else if (args[1].equalsIgnoreCase("who")) {
					getDominionWho(args, player);
				}
				else if (args[1].equalsIgnoreCase("list")) {
					// All Dominions that currently exist
					// Sorted by balance
				}
				else if (args[1].equalsIgnoreCase("members")) {
					// /ac dominion members [name]
					// Without inputting name, show your Dominion's members
					// When inputting a name, make sure it matches the full name of the Dominion (spaces and all)
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
				// Teleports you to the survival world spawn
				try {
					AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "world");
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
					return;
				}

				// Only remove potion effects if changing from a non-survival world
				if (!player.getLocation().getWorld().getName().startsWith("world")) {
					for (PotionEffect effect : player.getActivePotionEffects()) {
						player.removePotionEffect(effect.getType());
					}
				}

				player.teleport(dominion.getDominionHome());
				player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eSurvival!"));
				player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + dominion.getName()));
				player.setGameMode(GameMode.SURVIVAL);

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
					return;
				}

				if (player.hasPermission("aranarth.chat.hex")) {
					dominionName = ChatUtils.translateToColor(dominionName);
				} else if (player.hasPermission("aranarth.chat.color")) {
					dominionName = ChatUtils.playerColorChat(dominionName);
					if (dominionName == null) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this kind of formatting!"));
						return;
					}
				} else {
					if (!dominionName.matches("^[A-Za-z ]*$")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this kind of formatting!"));
						return;
					}
				}

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
								aranarthPlayer.setBalance(aranarthPlayer.getBalance() - 5000);

								DominionUtils.createDominion(new Dominion(dominionName, player.getUniqueId(), members, loc.getWorld().getName(), chunks, 50, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), 5000));
								Bukkit.broadcastMessage(ChatUtils.chatMessage(AranarthUtils.getNickname(player) + " &7has created the Dominion of &e" + dominionName));
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
			if (dominion.getOwner().equals(player.getUniqueId())) {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The Dominion of &e" + dominion.getName() + " &7has been disbanded"));
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setBalance(aranarthPlayer.getBalance() + dominion.getBalance());
				player.sendMessage(ChatUtils.chatMessage("&7Your Dominion's balance has been added to your own"));
				DominionUtils.disbandDominion(dominion);
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
		if (dominion.getOwner().equals(player.getUniqueId())) {
			List<Chunk> chunks = dominion.getChunks();
			if (chunks.contains(player.getLocation().getChunk())) {
				dominion.setDominionHome(player.getLocation());
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

			if (dominion.getOwner().equals(player.getUniqueId())) {
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
					if (invitedPlayer.isOnline()) {
						invitedPlayer.sendMessage(ChatUtils.chatMessage("&7You have been invited to join &e" + dominion.getName()));
						invitedPlayer.sendMessage(ChatUtils.chatMessage("&7Use &e/ac dominion accept &7to join!"));
					}
				} else {
					if (inputDominion.getOwner().equals(dominion.getOwner())) {
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
			if (dominion.getOwner().equals(player.getUniqueId())) {
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
			if (dominion.getOwner().equals(player.getUniqueId())) {
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
}
