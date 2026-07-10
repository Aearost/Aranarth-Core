package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Allows council members to teleport to another player's home.
 */
public class CommandAdminHome {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.getCouncilRank() != 3) {
			player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
			return true;
		} else if (!aranarthPlayer.isInAdminMode()) {
			player.sendMessage(ChatUtils.chatMessage("&cYou must be in admin mode to use this command!"));
			return true;
		}

		if (args.length < 3) {
			player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: &e/ac home <player> <home>"));
			return true;
		}

		OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
		if (target == null) {
			player.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
			return true;
		}
		AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
		if (targetAranarthPlayer == null) {
			player.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
			return true;
		}
		String homeName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

		for (Home home : targetAranarthPlayer.getHomes()) {
			if (homeName.equalsIgnoreCase(ChatUtils.stripColorFormatting(home.getName()))) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), home.getLocation(), true, home.getName(), "&7You have teleported to " + targetAranarthPlayer.getNickname() + "&7's home", success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e"
								+ targetAranarthPlayer.getNickname() + "&7's home &e" + home.getName()));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e"
								+ targetAranarthPlayer.getNickname() + "&7's home"));
					}
				});
				return true;
			}
		}

		player.sendMessage(ChatUtils.chatMessage("&cThis home could not be found!"));
		return true;
	}
}
