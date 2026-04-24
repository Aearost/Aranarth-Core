package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthVote;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

/**
 * Lists the top 10 players by vote count, with optional year/month filters.
 */
public class CommandVoteTop implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) {
			displayVoteTop(sender, null, null, "All-Time");
		} else if (args.length == 2 && args[0].equalsIgnoreCase("year")) {
			try {
				int year = Integer.parseInt(args[1]);
				if (year < 1000 || year > 9999) throw new NumberFormatException();
				displayVoteTop(sender, year, null, "Year " + year);
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid year! &e(e.g. /vtop year 2025)"));
			}
		} else if (args.length == 2 && args[0].equalsIgnoreCase("month")) {
			try {
				String[] parts = args[1].split("-");
				if (parts.length != 2) throw new IllegalArgumentException();
				int month = Integer.parseInt(parts[0]);
				int year = Integer.parseInt(parts[1]);
				if (month < 1 || month > 12) throw new IllegalArgumentException();
				String label = String.format("%02d-%d", month, year);
				displayVoteTop(sender, year, month, label);
			} catch (Exception e) {
				sender.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid month! &e(e.g. /vtop month 04-2025)"));
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: &e/votetop [month|year] [MM-YYYY|YYYY]"));
		}
		return true;
	}

	/**
	 * Displays the top 10 voters for the given period.
	 * @param sender The command sender.
	 * @param year The year to filter by, or null for all-time.
	 * @param month The month to filter by (1-12), or null for all months in the year.
	 * @param label The label to show in the header.
	 */
	private void displayVoteTop(CommandSender sender, Integer year, Integer month, String label) {
		List<AranarthVote> allVotes = AranarthUtils.getVotes();

		long startTime;
		long endTime;

		// Dictates what the range of votes should be considered
		if (year != null && month != null) {
			YearMonth yearMonth = YearMonth.of(year, month);
			startTime = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
			endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		} else if (year != null) {
			startTime = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
			endTime = LocalDate.of(year, 12, 31).atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		} else {
			startTime = Long.MIN_VALUE;
			endTime = Long.MAX_VALUE;
		}

		// Count votes per player within the time range
		Map<UUID, Integer> voteCounts = new HashMap<>();
		for (AranarthVote vote : allVotes) {
			if (vote.getTimestamp() >= startTime && vote.getTimestamp() <= endTime) {
				voteCounts.merge(vote.getUuid(), 1, Integer::sum);
			}
		}

		// Sort descending by vote count and take top 10
		List<Map.Entry<UUID, Integer>> sorted = voteCounts.entrySet().stream()
				.sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
				.limit(10)
				.toList();

		if (sorted.isEmpty()) {
			sender.sendMessage(ChatUtils.chatMessage("&7There are no votes in this period"));
			return;
		}

		sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lTop Voters (&e&l" + label + "&6&l) &8- - -"));

		int rank = 1;
		for (Map.Entry<UUID, Integer> entry : sorted) {
			UUID uuid = entry.getKey();
			int count = entry.getValue();
			String displayedName = ChatUtils.providePrefixAndName(uuid);
			String voteWord = count == 1 ? "vote" : "votes";
			sender.sendMessage(ChatUtils.translateToColor("&8[&6" + rank + "&8] &e" + displayedName + ", &6" + count + " &e" + voteWord));
			rank++;
		}
	}
}
