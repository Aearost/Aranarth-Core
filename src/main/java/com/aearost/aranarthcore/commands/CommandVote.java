package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the player with the list of URLs that they can vote using.
 */
public class CommandVote {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (args.length == 1) {
				// Display links here
			} else if (args.length == 2) {
				if (!player.hasPermission("aranarth.vote.test")) {
					player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac vote"));
					return true;
				}

				if (args[1].equals("test")) {
					Vote vote = new Vote(
							"TestService",
							player.getName(),
							"127.0.0.1",
							Long.toString(System.currentTimeMillis())
					);

					VotifierEvent event = new VotifierEvent(vote);
					Bukkit.getPluginManager().callEvent(new VotifierEvent(vote));
				}
			}
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}
}
