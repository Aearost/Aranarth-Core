package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the creative world, using the creative inventory.
 */
public class CommandCreative implements CommandExecutor {

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
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			// Only the OG players, Council, and Architect are permitted into the Creative world
			if (!AranarthUtils.isOriginalPlayer(player.getUniqueId()) && aranarthPlayer.getCouncilRank() == 0
					&& aranarthPlayer.getArchitectRank() == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have access to this world!"));
				return false;
			}

			if (AranarthUtils.getTeleportTask(player.getUniqueId()) != null) {
				player.sendMessage(ChatUtils.chatMessage("&cYou are already teleporting somewhere!"));
				return false;
			}

			Location creativeSpawn = new Location(Bukkit.getWorld("creative"), 0, -60, 0, 0, 2);
			AranarthUtils.teleportPlayer(player, player.getLocation(), creativeSpawn, aranarthPlayer.isInAdminMode(), success -> {
				if (success) {
					player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eCreative"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &eCreative"));
				}
			});

			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return false;
		}
	}
}
