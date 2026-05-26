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
		player.sendMessage(buildVoteLink(" &8&l[&6&l1&8&l] &e&l&oPlanet Minecraft", "https://www.planetminecraft.com/server/aranarth-6795356/vote/"));
		player.sendMessage(buildVoteLink(" &8&l[&6&l2&8&l] &7&l&oMC Server List", "https://minecraft-mp.com/server/356466/vote/"));
		player.sendMessage(buildVoteLink(" &8&l[&6&l3&8&l] &e&l&oMinecraft MP", "https://minecraftservers.org/vote/685940"));
		player.sendMessage(buildVoteLink(" &8&l[&6&l4&8&l] &7&l&oMC Servers", "https://minecraft-server-list.com/server/519140/vote/"));
		player.sendMessage(buildVoteLink(" &8&l[&6&l5&8&l] &e&l&oTopG", "https://topg.org/minecraft-servers/server-681145"));
		player.sendMessage(buildVoteLink(" &8&l[&6&l6&8&l] &7&l&oMineList", "https://minelist.net/vote/5200"));
		player.sendMessage(buildVoteLink(" &8&l[&6&l7&8&l] &e&l&oMinecraft Buzz", "https://minecraft.buzz/vote/20178"));
	}

	/**
	 * Builds a chat component with a plain label and a clickable URL that opens in the browser.
	 * @param label The label text (supports & color codes).
	 * @param url The URL to display and open on click.
	 */
	private Component buildVoteLink(String label, String url) {
//		Component labelComponent = LegacyComponentSerializer.legacySection().deserialize(
//				ChatUtils.translateToColor(label));
//		Component urlComponent = ChatUtils.clickableUrl(
//				LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor("&e" + url)),
//				ChatUtils.translateToColor("&7Click to open in browser"),
//				url
//		);

		String[] parts = label.split(" ");
		String siteName = ChatUtils.stripColorFormatting(parts[1]);
		if (parts.length > 2) {
			siteName += " " + ChatUtils.stripColorFormatting(parts[2]);
		}
		if (parts.length > 3) {
			siteName += " " + ChatUtils.stripColorFormatting(parts[3]);
		}

		Component voteComponent = ChatUtils.clickableUrl(
				LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor("&e" + label)),
				ChatUtils.translateToColor("&7Click to open &e" + siteName + " &7in browser"),
				url
		);

		return Component.empty().append(voteComponent);

//		return Component.empty().append(labelComponent).append(urlComponent);
	}
}
