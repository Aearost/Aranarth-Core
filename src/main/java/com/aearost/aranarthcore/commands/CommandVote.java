package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
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
				player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lVote Links &8- - -"));
				player.sendMessage(ChatUtils.translateToColor("&cLINK #1"));
				player.sendMessage(ChatUtils.translateToColor("&6LINK #2"));
				player.sendMessage(ChatUtils.translateToColor("&eLINK #3"));
				player.sendMessage(ChatUtils.translateToColor("&aLINK #4"));
				player.sendMessage(ChatUtils.translateToColor("&bLINK #5"));
				player.sendMessage(ChatUtils.translateToColor("&5LINK #6"));
				player.sendMessage(ChatUtils.translateToColor("&dLINK #7"));
			} else if (args.length == 2) {
				if (args[1].equals("test")) {
					if (!player.hasPermission("aranarth.vote.test")) {
						player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac vote"));
						return true;
					}

					if (args[1].equals("test")) {
						Vote vote = new Vote(
								"AranarthCore",
								player.getName(),
								"127.0.0.1",
								Long.toString(System.currentTimeMillis())
						);

						VotifierEvent event = new VotifierEvent(vote);
						Bukkit.getPluginManager().callEvent(new VotifierEvent(vote));
						return true;
					}
				} else if (args[1].equals("stats")) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lYour Vote Stats &8- - -"));
					player.sendMessage(ChatUtils.translateToColor("&7Total number of votes: &e" + aranarthPlayer.getVoteTotal()));
					player.sendMessage(ChatUtils.translateToColor("&7Number of vote points: &e" + aranarthPlayer.getVoteTotal()));
					player.sendMessage(ChatUtils.translateToColor("&7Number of vote points used: &e" + (aranarthPlayer.getVoteTotal() - aranarthPlayer.getVotePoints())));
					return true;
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac vote"));
					return true;
				}
			}
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}
}
