package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
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
 * Teleports the player to the Spawn world, sharing the Survival inventory.
 */
public class CommandSpawn implements CommandExecutor {

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
			if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.already_teleporting")));
				return true;
			}
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			// On SMP: countdown here first, then transfer to Survival and teleport to spawn on arrival.
			if (AranarthCore.isSmpServer() && NetworkManager.isActive()) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
						aranarthPlayer.isInAdminMode(), "&e&lSpawn", "&7Transferring to Spawn...", success -> {
					if (success) {
						String survivalServerName = AranarthCore.getInstance().getConfig()
								.getString("network.servers.survival", "survival");
						NetworkManager.getInstance().saveInventoryAndTransfer(player, survivalServerName,
								new PendingTeleport("spawn", 0.5, 101.0, 0.5, 180.0f, 0.0f,
										"&e&lSpawn", "&7You have teleported to Spawn"));
					}
				});
				return true;
			}

			Location spawn = new Location(Bukkit.getWorld("spawn"), 0.5, 101, 0.5, 180, 0);
			AranarthUtils.teleportPlayer(player, player.getLocation(), spawn, aranarthPlayer.isInAdminMode(), "&e&lSpawn", "&7You have teleported to Spawn", success -> {
				if (success) {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.to_spawn")));
				} else {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_spawn")));
				}
			});
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}
}
