package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.BalanceComparator;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;

import java.text.NumberFormat;
import java.util.*;

/**
 * Lists the players with the highest balances.
 */
public class CommandBaltop {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		List<String> lines = baltopSetup();

		double generalPageAmount = lines.size() / 10.0;
		int totalPageNumber = (int) generalPageAmount;

		// Adds one if there isn't exactly 10 players on the last page
		if (generalPageAmount % 1 != 0) {
			totalPageNumber++;
		}

		sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lAranarth Balances &8- - -"));
		int page = 1;

		if (args.length > 1) {
			try {
				page = Integer.parseInt(args[1]);

				// If it's an invalid input
				if (page <= 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatUtils.translateToColor("&cPlease enter a valid page number!"));
				return true;
			}

			if (page > totalPageNumber) {
				page = totalPageNumber;
			}
		}

		sender.sendMessage(ChatUtils.translateToColor("&7Showing page &6" + page + " &7of &6" + totalPageNumber));

		int line = (page * 10) - 10;
		page++;

		for (int i = 0; i < 10; i++) {
			sender.sendMessage(ChatUtils.translateToColor(lines.get(line)));
			line++;
			if (lines.size() == line) {
				break;
			}
		}

		return true;
	}

	/**
	 * Provides the full list of all players' balances.
	 * @return The full list of all player's balances.
	 */
	private static List<String> baltopSetup() {
		HashMap<UUID, AranarthPlayer> players = AranarthUtils.getAranarthPlayers();
		List<AranarthPlayer> playersAsList = new ArrayList<>(players.values());
		List<String> lines = new ArrayList<>();

		playersAsList.sort(new BalanceComparator());
		NumberFormat formatter = NumberFormat.getCurrencyInstance();

		int counter = 1;
		for (AranarthPlayer aranarthPlayer : playersAsList) {
			String displayedName = "";
			displayedName += AranarthUtils.getSaintRank(aranarthPlayer);
			displayedName += AranarthUtils.getArchitectRank(aranarthPlayer);
			displayedName += AranarthUtils.getCouncilRank(aranarthPlayer);
			displayedName += aranarthPlayer.getNickname();

			lines.add("&7" + counter + ". &e" + displayedName + ", &6" + formatter.format(aranarthPlayer.getBalance()));
			counter++;
		}

		return lines;
	}

}
