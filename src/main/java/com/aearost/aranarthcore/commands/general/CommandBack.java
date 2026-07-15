package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player back to their last known location.
 */
public class CommandBack implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.back")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this command!"));
				return true;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getLastKnownTeleportLocation() != null) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), aranarthPlayer.getLastKnownTeleportLocation(), aranarthPlayer.isInAdminMode(), "&e&lPrevious Location", "&7You have returned to your previous location", success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have returned to your previous location"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not return to your previous location"));
					}
				});
				return true;
			} else if (NetworkManager.isActive()) {
				// Check for a cross-server /back location (set on cross-server arrival)
				String crossBack = NetworkManager.getInstance().consumeCrossServerBack(player.getUniqueId());
				if (crossBack != null) {
					// Format: "serverKey|world|x|y|z|yaw|pitch"
					String[] parts = crossBack.split("\\|", 7);
					if (parts.length == 7) {
						String serverKey = parts[0];
						String world = parts[1];
						double bx = Double.parseDouble(parts[2]);
						double by = Double.parseDouble(parts[3]);
						double bz = Double.parseDouble(parts[4]);
						float byaw = Float.parseFloat(parts[5]);
						float bpitch = Float.parseFloat(parts[6]);
						String targetServer = AranarthCore.getInstance().getConfig()
								.getString("network.servers." + serverKey, serverKey);
						AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
								aranarthPlayer.isInAdminMode(), "&e&lPrevious Location", "&7Transferring to your previous location...", success -> {
							if (success) {
								NetworkManager.getInstance().saveInventoryAndTransfer(player, targetServer,
										new PendingTeleport(world, bx, by, bz, byaw, bpitch,
												"&e&lPrevious Location", "&7You have returned to your previous location"));
							}
						});
						return true;
					}
				}
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have a previous location to teleport to!"));
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have a previous location to teleport to!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}
}
