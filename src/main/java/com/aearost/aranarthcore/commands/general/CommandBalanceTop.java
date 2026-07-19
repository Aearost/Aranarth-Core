package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lists the players with the highest balances.
 */
public class CommandBalanceTop implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		List<String> lines = baltopSetup();

		double generalPageAmount = lines.size() / 10.0;
		int totalPageNumber = (int) generalPageAmount;

		// Adds one if there isn't exactly 10 players on the last page
		if (generalPageAmount % 1 != 0) {
			totalPageNumber++;
		}

		sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lAranarth Balances &8- - -"));
		int page = 1;

		if (args.length > 0) {
			try {
				page = Integer.parseInt(args[0]);

				// If it's an invalid input
				if (page <= 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatUtils.translateToColor("&cPlease enter a valid page number!"));
				return false;
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
	 * Provides the full list of all players' balances, queried fresh from the database
	 * so that balances from other servers are included.
	 * @return The full list of all player's balances.
	 */
	private static List<String> baltopSetup() {
		Map<UUID, DatabaseManager.BalanceEntry> balances = DatabaseManager.getInstance().loadAllPlayerBalances();
		List<Map.Entry<UUID, DatabaseManager.BalanceEntry>> sorted = new ArrayList<>(balances.entrySet());
		sorted.sort(Comparator.<Map.Entry<UUID, DatabaseManager.BalanceEntry>>comparingDouble(e -> e.getValue().balance()).reversed());

		List<String> lines = new ArrayList<>();
		NumberFormat formatter = NumberFormat.getCurrencyInstance();

		int counter = 1;
		for (Map.Entry<UUID, DatabaseManager.BalanceEntry> entry : sorted) {
			UUID uuid = entry.getKey();
			DatabaseManager.BalanceEntry balanceEntry = entry.getValue();
			AranarthPlayer aranarthPlayer = AranarthUtils.getAranarthPlayers().get(uuid);

			String displayedName;
			if (aranarthPlayer != null) {
				displayedName = ChatUtils.providePrefixAndName(uuid);
			} else {
				// Player is not loaded on this server — use DB nickname/username for display.
				// If they are currently online on another server, prefer the remote roster entry.
				NetworkPlayer remote = NetworkManager.isActive()
						? NetworkManager.getInstance().getRemotePlayer(uuid) : null;
				if (remote != null) {
					displayedName = remote.getNickname().isEmpty()
							? remote.getUsername()
							: ChatUtils.stripColorFormatting(remote.getNickname());
				} else {
					String nick = balanceEntry.nickname();
					displayedName = (nick == null || nick.isEmpty())
							? balanceEntry.username()
							: ChatUtils.stripColorFormatting(nick);
				}
			}

			lines.add("&8[&6" + counter + "&8] &e" + displayedName + ", &6" + formatter.format(balanceEntry.balance()));
			counter++;
		}

		return lines;
	}

}
