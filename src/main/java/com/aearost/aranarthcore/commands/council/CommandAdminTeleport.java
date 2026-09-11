package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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

	private static NetworkPlayer findRemotePlayer(String username) {
		if (!NetworkManager.isActive()) return null;
		for (NetworkPlayer np : NetworkManager.getInstance().getRemoteRoster().values()) {
			if (np.getUsername().equalsIgnoreCase(username)) return np;
		}
		return null;
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

			if (aranarthPlayer.getCouncilRank() < 2) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
		} else {
            player = null;
            aranarthPlayer = null;
            if (!(sender instanceof BlockCommandSender)) {
                sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
                return true;
            }
        }

		boolean isSenderPlayer = player != null;

		if (args[0].equalsIgnoreCase("tpw")) {
			// /ac tpw <worldname> - teleport self to surface of 0,0 in the specified world
			if (isSenderPlayer && aranarthPlayer.getCouncilRank() < 3) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
			if (!isSenderPlayer) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
				return true;
			}
			if (args.length != 2) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac tpw <worldname>")));
				return true;
			}
			World world = Bukkit.getWorld(args[1]);
			if (world == null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.world_not_found", "name", args[1])));
				return true;
			}
			Location loc = AranarthUtils.getSafeTeleportLocation(new Location(world, 0, world.getHighestBlockYAt(0, 0), 0));
			player.teleport(loc);
			player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_world_success", "world", world.getName())));
			return true;
		} else if (args[0].equalsIgnoreCase("tpf")) {
			if (isSenderPlayer && aranarthPlayer.getCouncilRank() < 3) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
			// /ac tpf x y z [<yaw> <pitch>] - self teleport, player only
			if (args.length == 4 || args.length == 6) {
				if (!isSenderPlayer) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
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
					player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_coordinates")));
					return true;
				}

				Location loc = args.length == 4
						? new Location(player.getWorld(), x, y, z)
						: new Location(player.getWorld(), x, y, z, yaw, pitch);
				player.teleport(loc);
				player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.to_coordinates")));
			}
			// /ac tpf username x y z [<yaw> <pitch>] - command blocks supported, @p resolved
			else if (args.length == 5 || args.length == 7) {
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
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_coordinates")));
					return true;
				}
				Player target = resolvePlayer(sender, args[1]);
				if (target != null) {
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
					Location loc = args.length == 5
							? new Location(target.getWorld(), x, y, z)
							: new Location(target.getWorld(), x, y, z, yaw, pitch);
					target.teleport(loc);
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_player_coords", "player", targetAranarthPlayer.getNickname())));
					target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.to_coordinates")));
				} else {
					NetworkPlayer remoteTarget = findRemotePlayer(args[1]);
					if (remoteTarget != null) {
						World destWorld = isSenderPlayer ? player.getWorld() : Bukkit.getWorlds().get(0);
						Location loc = args.length == 5
								? new Location(destWorld, x, y, z)
								: new Location(destWorld, x, y, z, yaw, pitch);
						PendingTeleport pending = new PendingTeleport(
								destWorld.getName(), loc.getX(), loc.getY(), loc.getZ(),
								loc.getYaw(), loc.getPitch(),
								"&e&lCoordinates", "&7You have been teleported to the input coordinates");
						NetworkManager.getInstance().setPendingTeleport(remoteTarget.getUuid(), pending);
						NetworkManager.getInstance().publishTransfer(remoteTarget.getUuid(), NetworkManager.getInstance().getThisServer());
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleporting_player_coords", "player", remoteTarget.getNickname())));
					} else {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
					}
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_coordinates")));
			}
			return true;
		} else {
			// Teleports the sender to the player
			// /ac tp username
			if (args.length == 2) {
				if (!isSenderPlayer) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
					return true;
				}

				Player target = Bukkit.getPlayer(args[1]);
				if (target != null) {
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
                    AranarthUtils.teleportPlayer(player, player.getLocation(), target.getLocation(), true, targetAranarthPlayer.getNickname(), Lang.get("admin.teleport_to_player_success", "player", targetAranarthPlayer.getNickname()), success -> {
						if (success) {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_to_player_success", "player", targetAranarthPlayer.getNickname())));
						} else {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_to_player_failed", "player", targetAranarthPlayer.getNickname())));
						}
					});
				} else {
					NetworkPlayer remoteTarget = findRemotePlayer(args[1]);
					if (remoteTarget != null) {
						String targetServer = AranarthCore.getInstance().getConfig()
								.getString("network.servers." + remoteTarget.getServer(), remoteTarget.getServer());
						PendingTeleport pending = new PendingTeleport(
								remoteTarget.getUuid().toString(),
								"&e&l" + remoteTarget.getServer().toUpperCase(),
								"&7You have teleported to " + remoteTarget.getNickname());
						player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleporting_to_player", "player", remoteTarget.getNickname())));
						NetworkManager.getInstance().saveInventoryAndTransfer(player, targetServer, pending);
					} else {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
					}
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

					AranarthUtils.teleportPlayer(target1, target1.getLocation(), target2.getLocation(), true, target2AranarthPlayer.getNickname(), Lang.get("admin.teleport_to_player_success", "player", target2AranarthPlayer.getNickname()), success -> {
						if (success) {
							if (!isSenderPlayer
									|| (!player.getUniqueId().equals(target1.getUniqueId())
									&& !player.getUniqueId().equals(target2.getUniqueId()))) {
								sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_p1_to_p2", "player1", target1AranarthPlayer.getNickname(), "player2", target2AranarthPlayer.getNickname())));
							}

							target1.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleported_to_player", "player", target2AranarthPlayer.getNickname())));
						} else {
							sender.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.failed")));
						}
					});
				} else if (NetworkManager.isActive()) {
					NetworkPlayer remote1 = target1 == null ? findRemotePlayer(args[1]) : null;
					NetworkPlayer remote2 = target2 == null ? findRemotePlayer(args[2]) : null;

					if (target1 == null && remote1 == null) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
						return true;
					}
					if (target2 == null && remote2 == null) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[2])));
						return true;
					}

					String nick1 = target1 != null ? AranarthUtils.getPlayer(target1.getUniqueId()).getNickname() : remote1.getNickname();
					String nick2 = target2 != null ? AranarthUtils.getPlayer(target2.getUniqueId()).getNickname() : remote2.getNickname();

					if (target1 != null && remote2 != null) {
						// player1 local, player2 remote: transfer player1 to player2's server
						String targetServer = AranarthCore.getInstance().getConfig()
								.getString("network.servers." + remote2.getServer(), remote2.getServer());
						PendingTeleport pending = new PendingTeleport(
								remote2.getUuid().toString(),
								"&e&l" + remote2.getServer().toUpperCase(),
								"&7You have been teleported to " + nick2);
						target1.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleporting_to_player", "player", nick2)));
						NetworkManager.getInstance().saveInventoryAndTransfer(target1, targetServer, pending);
						if (!isSenderPlayer || !player.getUniqueId().equals(target1.getUniqueId())) {
							sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_p1_to_p2", "player1", nick1, "player2", nick2)));
						}
					} else if (remote1 != null && target2 != null) {
						// player1 remote, player2 local: pull player1 to this server to tp to player2
						PendingTeleport pending = new PendingTeleport(
								target2.getUniqueId().toString(),
								"&e&l" + NetworkManager.getInstance().getThisServer().toUpperCase(),
								Lang.get("admin.teleported_to_player", "player", nick2));
						NetworkManager.getInstance().setPendingTeleport(remote1.getUuid(), pending);
						NetworkManager.getInstance().publishTransfer(remote1.getUuid(), NetworkManager.getInstance().getThisServer());
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_p1_to_p2", "player1", nick1, "player2", nick2)));
					} else {
						// both remote
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.both_remote")));
					}
				} else {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.one_not_found")));
				}
				return true;
			}
			// Teleports the target player to the input coordinates
			// /ac tp username x y z [<yaw> <pitch>] - command blocks supported, @p resolved
			else if (args.length == 5 || args.length == 7) {
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
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_coordinates")));
					return true;
				}
				Player target = resolvePlayer(sender, args[1]);
				if (target != null) {
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
					Location loc = args.length == 5
							? new Location(target.getWorld(), x, y, z)
							: new Location(target.getWorld(), x, y, z, yaw, pitch);
					AranarthUtils.teleportPlayer(target, target.getLocation(), loc, true, "&e&lCoordinates", "&7You have teleported to the input coordinates", success -> {
						if (success) {
							sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_player_coords", "player", targetAranarthPlayer.getNickname())));
							target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.to_coordinates")));
						} else {
							sender.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.failed")));
						}
					});
				} else {
					NetworkPlayer remoteTarget = findRemotePlayer(args[1]);
					if (remoteTarget != null) {
						World destWorld = isSenderPlayer ? player.getWorld() : Bukkit.getWorlds().get(0);
						Location loc = args.length == 5
								? new Location(destWorld, x, y, z)
								: new Location(destWorld, x, y, z, yaw, pitch);
						PendingTeleport pending = new PendingTeleport(
								destWorld.getName(), loc.getX(), loc.getY(), loc.getZ(),
								loc.getYaw(), loc.getPitch(),
								"&e&lCoordinates", "&7You have been teleported to the input coordinates");
						NetworkManager.getInstance().setPendingTeleport(remoteTarget.getUuid(), pending);
						NetworkManager.getInstance().publishTransfer(remoteTarget.getUuid(), NetworkManager.getInstance().getThisServer());
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleporting_player_coords", "player", remoteTarget.getNickname())));
					} else {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
					}
				}
			}
			// Teleports self to the input coordinates
			// /ac tp x y z [<yaw> <pitch>]
			else if (args.length == 4 || args.length == 6) {
				if (!isSenderPlayer) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
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
					player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_coordinates")));
					return true;
				}

				Location loc = args.length == 4
						? new Location(player.getWorld(), x, y, z)
						: new Location(player.getWorld(), x, y, z, yaw, pitch);
				AranarthUtils.teleportPlayer(player, player.getLocation(), loc, true, "&e&lCoordinates", "&7You have teleported to the input coordinates", success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.to_coordinates")));
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.failed")));
					}
				});
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_coordinates")));
			}
			return true;
		}
	}

}
