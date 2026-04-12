package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to forcefully teleport to the other player.
 */
public class CommandAdminTeleport {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			if (aranarthPlayer.getCouncilRank() != 3) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}

			if (args[0].equalsIgnoreCase("tpf")) {
				// /ac tpf x y z [<yaw> <pitch>]
				if (args.length == 4 || args.length == 6) {
					double x, y, z;
					float yaw = 0;
					float pitch = 0;
					try {
						x = Double.parseDouble(args[1]);
						y = Double.parseDouble(args[2]);
						z = Double.parseDouble(args[3]);

						if (args.length == 6) {
							yaw = Float.parseFloat(args[4]);
							pitch = Float.parseFloat(args[5]);
						}
					} catch (NumberFormatException e) {
						player.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
						return true;
					}

					Location loc = null;
					if (args.length == 4) {
						loc = new Location(player.getWorld(), x, y, z);
					} else {
						loc = new Location(player.getWorld(), x, y, z, yaw, pitch);
					}
					player.teleport(loc);
					player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the input coordinates"));
				}
				// /ac tpf username x y z [<yaw> <pitch>]
				else if (args.length == 5 || args.length == 7) {
					Player target = Bukkit.getPlayer(args[1]);
					if (target == null) {
						player.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
						return true;
					}
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
					double x, y, z;
					float yaw = 0;
					float pitch = 0;
					try {
						x = Double.parseDouble(args[2]);
						y = Double.parseDouble(args[3]);
						z = Double.parseDouble(args[4]);
						if (args.length == 7) {
							yaw = Float.parseFloat(args[5]);
							pitch = Float.parseFloat(args[6]);
						}
					} catch (NumberFormatException e) {
						player.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
						return true;
					}
					Location loc = args.length == 5
							? new Location(target.getWorld(), x, y, z)
							: new Location(target.getWorld(), x, y, z, yaw, pitch);
					target.teleport(loc);
					player.sendMessage(ChatUtils.chatMessage("&7You have teleported &e" + targetAranarthPlayer.getNickname() + " &7to the input coordinates"));
					target.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the input coordinates"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
				}
				return true;
			} else {
				// Teleports the sender to the player
				// /ac tp username
				if (args.length == 2) {
					Player target = Bukkit.getPlayer(args[1]);
					if (target != null) {
						AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
						AranarthUtils.teleportPlayer(player, player.getLocation(), target.getLocation(), true, success -> {
							if (success) {
								player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + targetAranarthPlayer.getNickname()));
								target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has teleported to you"));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + targetAranarthPlayer.getNickname()));
							}
						});
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
					}
					return true;
				}
				// Teleports the first player to the second player
				// /ac tp username1 username2
				else if (args.length == 3) {
					Player target1 = Bukkit.getPlayer(args[1]);
					Player target2 = Bukkit.getPlayer(args[2]);
					if (target1 != null && target2 != null) {
						AranarthPlayer target1AranarthPlayer = AranarthUtils.getPlayer(target1.getUniqueId());
						AranarthPlayer target2AranarthPlayer = AranarthUtils.getPlayer(target2.getUniqueId());

						AranarthUtils.teleportPlayer(target1, target1.getLocation(), target2.getLocation(), true, success -> {
							if (success) {
								if (!player.getUniqueId().equals(target1.getUniqueId())
										&& !player.getUniqueId().equals(target2.getUniqueId())) {
									player.sendMessage(ChatUtils.chatMessage("&7You have teleported &e"
											+ target1AranarthPlayer.getNickname() + " &7to &e" + target2AranarthPlayer.getNickname()));
								}

								target1.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &e" + target2AranarthPlayer.getNickname()));
								target2.sendMessage(ChatUtils.chatMessage("&e" + target1AranarthPlayer.getNickname() + " &7has been teleported to you"));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cThe teleportation failed"));
							}
						});
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cOne of the two input players could not be found!"));
					}
					return true;
				}
				// Teleports the target player to the input coordinates
				// /ac tf username x y z [<yaw> <pitch>]
				else if (args.length == 5 || args.length == 7) {
					Player target = Bukkit.getPlayer(args[1]);
					if (target == null) {
						player.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
						return true;
					}
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
					double x, y, z;
					float yaw = 0;
					float pitch = 0;
					try {
						x = Double.parseDouble(args[2]);
						y = Double.parseDouble(args[3]);
						z = Double.parseDouble(args[4]);
						if (args.length == 7) {
							yaw = Float.parseFloat(args[5]);
							pitch = Float.parseFloat(args[6]);
						}
					} catch (NumberFormatException e) {
						player.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
						return true;
					}
					Location loc = args.length == 5
							? new Location(target.getWorld(), x, y, z)
							: new Location(target.getWorld(), x, y, z, yaw, pitch);
					AranarthUtils.teleportPlayer(target, target.getLocation(), loc, true, success -> {
						if (success) {
							player.sendMessage(ChatUtils.chatMessage("&7You have teleported &e" + targetAranarthPlayer.getNickname() + " &7to the input coordinates"));
							target.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the input coordinates"));
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cThe teleportation failed"));
						}
					});
				}
				// Teleports self to the input coordinates
				// /ac tp x y z [<yaw> <pitch>]
				else if (args.length == 4 || args.length == 6) {
					double x, y, z;
					float yaw = 0;
					float pitch = 0;
					try {
						x = Double.parseDouble(args[1]);
						y = Double.parseDouble(args[2]);
						z = Double.parseDouble(args[3]);

						if (args.length == 6) {
							yaw = Float.parseFloat(args[4]);
							pitch = Float.parseFloat(args[5]);
						}
					} catch (NumberFormatException e) {
						player.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
						return true;
					}

					Location loc = null;
					if (args.length == 4) {
						loc = new Location(player.getWorld(), x, y, z);
					} else {
						loc = new Location(player.getWorld(), x, y, z, yaw, pitch);
					}
					AranarthUtils.teleportPlayer(player, player.getLocation(), loc, true, success -> {
						if (success) {
							player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the input coordinates"));
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cThe teleportation failed"));
						}
					});
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
				}
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command must be executed in-game!"));
			return true;
		}
	}

}
