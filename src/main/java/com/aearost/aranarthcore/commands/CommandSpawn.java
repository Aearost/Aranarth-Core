package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the Spawn world, sharing the Survival inventory.
 */
public class CommandSpawn {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (System.currentTimeMillis() < aranarthPlayer.getLastWorldCommandUse() + 60000) {
					if (!aranarthPlayer.isInAdminMode()) {
						int wait = (int) ((aranarthPlayer.getLastWorldCommandUse() + 60000) - System.currentTimeMillis()) / 1000;
						player.sendMessage(ChatUtils.chatMessage("&cYou must wait another &e" + wait + " seconds &cto use this command!"));
						return true;
					}
				}

				Location spawn = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), spawn, aranarthPlayer.isInAdminMode(), success -> {
					if (success) {
						aranarthPlayer.setLastWorldCommandUse(System.currentTimeMillis());
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eSpawn"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &eSpawn"));
					}
				});

				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
				return true;
			}
		}
		return false;
	}
}
