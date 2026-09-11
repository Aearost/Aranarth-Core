package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the SMP world, sharing the SMP inventory.
 */
public class CommandSMP implements CommandExecutor {

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
			if (!AranarthUtils.isOriginalPlayer(player.getUniqueId())) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("access.smp_restricted")));
				return true;
			}

			// If already on the SMP server, teleport to SMP spawn
			if (AranarthCore.isSmpServer()) {
				if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("general.already_teleporting")));
					return true;
				}
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				Location smpSpawn = new Location(Bukkit.getWorld(AranarthCore.getSmpMainWorldName()), 0.5, 120, 3, 180, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), smpSpawn, aranarthPlayer.isInAdminMode(), "&e&lSMP", "&7You have teleported to SMP Spawn", success -> {
					if (!success) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_smp")));
					}
				});
				return true;
			}

			if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.already_teleporting")));
				return true;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			if (AranarthCore.isPublicServer() && NetworkManager.isActive()) {
				// Countdown on this server first, then transfer on completion.
				AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
						aranarthPlayer.isInAdminMode(), "&e&lSMP", "&7Transferring to the SMP...", success -> {
					if (success) {
						com.aearost.aranarthcore.network.PendingTeleport smpPt =
								new com.aearost.aranarthcore.network.PendingTeleport(
										"world", 0.5, 120.0, 3.0, 180.0f, 0.0f,
										"&e&lSMP", "&7Welcome to the SMP");
						String smpServerName = AranarthCore.getInstance().getConfig()
								.getString("network.servers.smp", "smp");
						NetworkManager.getInstance().saveInventoryAndTransfer(player, smpServerName, smpPt);
					}
				});
			} else {
				// Test server (or public server without active networking): local teleport as before.
				Location smpSpawn = new Location(Bukkit.getWorld(AranarthCore.getSmpMainWorldName()), 0.5, 120, 3, 180, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), smpSpawn, aranarthPlayer.isInAdminMode(), "&e&lSMP", "&7You have teleported to the SMP", success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.to_smp")));
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_smp")));
					}
				});
			}
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}
}
