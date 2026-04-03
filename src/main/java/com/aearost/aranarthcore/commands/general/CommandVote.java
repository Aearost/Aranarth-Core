package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
		player.sendMessage(buildVoteLink("&7&lPlanet Minecraft: ", "https://tinyurl.com/5n7wezr9"));
		player.sendMessage(buildVoteLink("&7&lMC Server List: ", "https://tinyurl.com/ynsp6zm2"));
		player.sendMessage(buildVoteLink("&7&lMC Servers: ", "https://tinyurl.com/4r97nrsa"));
		player.sendMessage(buildVoteLink("&7&lMineList: ", "https://tinyurl.com/f8wfzbj6"));
		player.sendMessage(buildVoteLink("&7&lMinecraft MP: ", "https://tinyurl.com/3c8hfadt"));
		player.sendMessage(buildVoteLink("&7&lTopG: ", "https://tinyurl.com/5c2yxkzj"));
		player.sendMessage(buildVoteLink("&7&lMinecraft Buzz: ", "https://tinyurl.com/4ve6h5zn"));
	}

	/**
	 * Builds a chat component with a plain label and a clickable URL that opens in the browser.
	 * @param label The label text (supports & color codes).
	 * @param url The URL to display and open on click.
	 */
	private Component buildVoteLink(String label, String url) {
		Component labelComponent = LegacyComponentSerializer.legacySection().deserialize(
				ChatUtils.translateToColor(label));
		Component urlComponent = ChatUtils.clickableUrl(
				LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor("&e" + url)),
				ChatUtils.translateToColor("&7Click to open in browser"),
				url
		);
		return Component.empty().append(labelComponent).append(urlComponent);
	}
}
