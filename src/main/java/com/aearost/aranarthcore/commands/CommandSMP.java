package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the SMP world, sharing the SMP inventory.
 */
public class CommandSMP {

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

				Location smpSpawn = new Location(Bukkit.getWorld("smp"), 0.5, 120, 3, 180, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), smpSpawn, aranarthPlayer.isInAdminMode(), success -> {
					if (success) {
						aranarthPlayer.setLastWorldCommandUse(System.currentTimeMillis());
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the &eSMP"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to the &eSMP"));
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
