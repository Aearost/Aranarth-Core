package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Allows the player to forcefully teleport to the other player.
 */
public class CommandAdminTeleport {

	/**
	 * Resolves a player name or selector (e.g. @p) to a Player from the perspective of the sender.
	 */
	private static Player resolvePlayer(CommandSender sender, String nameOrSelector) {
		if (nameOrSelector.startsWith("@")) {
			try {
				List<Entity> entities = Bukkit.selectEntities(sender, nameOrSelector);
				for (Entity entity : entities) {
					if (entity instanceof Player) {
						return (Player) entity;
					}
				}
				return null;
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		return Bukkit.getPlayer(nameOrSelector);
	}

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		Player player;
		AranarthPlayer aranarthPlayer;
		if (sender instanceof Player) {
			player = (Player) sender;
			aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			if (aranarthPlayer.getCouncilRank() != 3) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}
		} else {
            player = null;
            aranarthPlayer = null;
            if (!(sender instanceof BlockCommandSender)) {
                sender.sendMessage(ChatUtils.chatMessage("&cThis command must be executed in-game!"));
                return true;
            }
        }

		boolean isSenderPlayer = player != null;

		if (args[0].equalsIgnoreCase("tpw")) {
			// /ac tpw <worldname> — teleport self to surface of 0,0 in the specified world
			if (!isSenderPlayer) {
				sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
				return true;
			}
			if (args.length != 2) {
				player.sendMessage(ChatUtils.chatMessage("&cUsage: /ac tpw <worldname>"));
				return true;
			}
			World world = Bukkit.getWorld(args[1]);
			if (world == null) {
				player.sendMessage(ChatUtils.chatMessage("&cWorld &e" + args[1] + " &ccould not be found!"));
				return true;
			}
			Location loc = AranarthUtils.getSafeTeleportLocation(new Location(world, 0, world.getHighestBlockYAt(0, 0), 0));
			player.teleport(loc);
			player.sendMessage(ChatUtils.chatMessage("&7You have teleported to world &e" + world.getName()));
			return true;
		} else if (args[0].equalsIgnoreCase("tpf")) {
			// /ac tpf x y z [<yaw> <pitch>] — self teleport, player only
			if (args.length == 4 || args.length == 6) {
				if (!isSenderPlayer) {
					sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
					return true;
				}

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

				Location loc = args.length == 4
						? new Location(player.getWorld(), x, y, z)
						: new Location(player.getWorld(), x, y, z, yaw, pitch);
				player.teleport(loc);
				player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the input coordinates"));
			}
			// /ac tpf username x y z [<yaw> <pitch>] — command blocks supported, @p resolved
			else if (args.length == 5 || args.length == 7) {
				Player target = resolvePlayer(sender, args[1]);
				if (target == null) {
					sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
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
					sender.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
					return true;
				}
				Location loc = args.length == 5
						? new Location(target.getWorld(), x, y, z)
						: new Location(target.getWorld(), x, y, z, yaw, pitch);
				target.teleport(loc);
				sender.sendMessage(ChatUtils.chatMessage("&7You have teleported &e" + targetAranarthPlayer.getNickname() + " &7to the input coordinates"));
				target.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the input coordinates"));
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
			}
			return true;
		} else {
			// Teleports the sender to the player
			// /ac tp username
			if (args.length == 2) {
				if (!isSenderPlayer) {
					sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
					return true;
				}

				Player target = Bukkit.getPlayer(args[1]);
				if (target != null) {
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
                    AranarthUtils.teleportPlayer(player, player.getLocation(), target.getLocation(), true, targetAranarthPlayer.getNickname(), "&7You have teleported to " + targetAranarthPlayer.getNickname(), success -> {
						if (success) {
							player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + targetAranarthPlayer.getNickname()));
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + targetAranarthPlayer.getNickname()));
						}
					});
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
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

					AranarthUtils.teleportPlayer(target1, target1.getLocation(), target2.getLocation(), true, target2AranarthPlayer.getNickname(), "&7You have teleported to " + target2AranarthPlayer.getNickname(), success -> {
						if (success) {
							if (!isSenderPlayer
									|| (!player.getUniqueId().equals(target1.getUniqueId())
									&& !player.getUniqueId().equals(target2.getUniqueId()))) {
								sender.sendMessage(ChatUtils.chatMessage("&7You have teleported &e"
										+ target1AranarthPlayer.getNickname() + " &7to &e" + target2AranarthPlayer.getNickname()));
							}

							target1.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &e" + target2AranarthPlayer.getNickname()));
						} else {
							sender.sendMessage(ChatUtils.chatMessage("&cThe teleportation failed"));
						}
					});
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cOne of the two input players could not be found!"));
				}
				return true;
			}
			// Teleports the target player to the input coordinates
			// /ac tp username x y z [<yaw> <pitch>] — command blocks supported, @p resolved
			else if (args.length == 5 || args.length == 7) {
				Player target = resolvePlayer(sender, args[1]);
				if (target == null) {
					sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
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
					sender.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
					return true;
				}
				Location loc = args.length == 5
						? new Location(target.getWorld(), x, y, z)
						: new Location(target.getWorld(), x, y, z, yaw, pitch);
				AranarthUtils.teleportPlayer(target, target.getLocation(), loc, true, "&e&lCoordinates", "&7You have teleported to the input coordinates", success -> {
					if (success) {
						sender.sendMessage(ChatUtils.chatMessage("&7You have teleported &e" + targetAranarthPlayer.getNickname() + " &7to the input coordinates"));
						target.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the input coordinates"));
					} else {
						sender.sendMessage(ChatUtils.chatMessage("&cThe teleportation failed"));
					}
				});
			}
			// Teleports self to the input coordinates
			// /ac tp x y z [<yaw> <pitch>]
			else if (args.length == 4 || args.length == 6) {
				if (!isSenderPlayer) {
					sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
					return true;
				}

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

				Location loc = args.length == 4
						? new Location(player.getWorld(), x, y, z)
						: new Location(player.getWorld(), x, y, z, yaw, pitch);
				AranarthUtils.teleportPlayer(player, player.getLocation(), loc, true, "&e&lCoordinates", "&7You have teleported to the input coordinates", success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to the input coordinates"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThe teleportation failed"));
					}
				});
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cThose coordinates are invalid"));
			}
			return true;
		}
	}

}
