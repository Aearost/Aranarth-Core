package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the player with the list of URLs that they can vote using.
 */
public class CommandVote implements CommandExecutor {

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
			if (args.length == 0) {
				displayVoteLinks(player);
			} else if (args.length == 1) {
				if (args[0].equals("test")) {
					if (!player.hasPermission("aranarth.vote.test")) {
						displayVoteLinks(player);
						return true;
					}

					if (args[0].equals("test")) {
						Vote vote = new Vote(
								"AranarthCore",
								player.getName(),
								"127.0.0.1",
								Long.toString(System.currentTimeMillis())
						);

						VotifierEvent event = new VotifierEvent(vote);
						Bukkit.getPluginManager().callEvent(new VotifierEvent(vote));
						return true;
					} else {
						displayVoteLinks(player);
					}
				}
			}
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}

	/**
	 * Displays the different voting links.
	 * @param player The player who ran the command.
	 */
	private void displayVoteLinks(Player player) {
		player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lVote Links &8- - -"));
		player.sendMessage(ChatUtils.translateToColor("&cLINK #1"));
		player.sendMessage(ChatUtils.translateToColor("&6LINK #2"));
		player.sendMessage(ChatUtils.translateToColor("&eLINK #3"));
		player.sendMessage(ChatUtils.translateToColor("&aLINK #4"));
		player.sendMessage(ChatUtils.translateToColor("&bLINK #5"));
		player.sendMessage(ChatUtils.translateToColor("&5LINK #6"));
		player.sendMessage(ChatUtils.translateToColor("&dLINK #7"));
	}
}
