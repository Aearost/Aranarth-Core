package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the creative world, using the creative inventory.
 */
public class CommandCreative {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
				if (!AranarthUtils.isOriginalPlayer(player.getUniqueId())) {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have access to this world!"));
					return true;
				}

				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (System.currentTimeMillis() < aranarthPlayer.getLastWorldCommandUse() + 60000) {
					if (!aranarthPlayer.isInAdminMode()) {
						int wait = (int) ((aranarthPlayer.getLastWorldCommandUse() + 60000) - System.currentTimeMillis()) / 1000;
						player.sendMessage(ChatUtils.chatMessage("&cYou must wait another &e" + wait + " seconds &cto use this command!"));
						return true;
					}
				}

				Location creativeSpawn = new Location(Bukkit.getWorld("creative"), 0, -60, 0, 0, 2);
				AranarthUtils.teleportPlayer(player, player.getLocation(), creativeSpawn, aranarthPlayer.isInAdminMode(), success -> {
					if (success) {
						aranarthPlayer.setLastWorldCommandUse(System.currentTimeMillis());
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eCreative"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &eCreative"));
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
