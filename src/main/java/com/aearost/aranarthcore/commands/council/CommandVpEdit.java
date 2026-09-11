package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.AranarthVote;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows for the manual addition or removal of vote points.
 */
public class CommandVpEdit {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() < 3) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
		}

		if (args.length != 3) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac vpedit <username> <+amount|-amount>")));
			return true;
		}

		String amountArg = args[2];
		if (!amountArg.startsWith("+") && !amountArg.startsWith("-")) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("vpedit.must_be_signed")));
			return true;
		}

		int amount;
		try {
			amount = Integer.parseInt(amountArg);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
			return true;
		}

		if (amount == 0) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.amount_zero")));
			return true;
		}

		OfflinePlayer target = null;
		for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
			if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(args[1])) {
				target = offlinePlayer;
				break;
			}
		}

		if (target == null) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
			return true;
		}

		AranarthPlayer targetPlayer = AranarthUtils.getPlayer(target.getUniqueId());
		String displayName = targetPlayer.getNickname();

		if (amount < 0) {
			int available = AranarthUtils.getAvailableVotePoints(target.getUniqueId());
			if (-amount > available) {
				sender.sendMessage(ChatUtils.chatMessage(displayName + "&c does not have enough vote points for this!"));
				return true;
			}
		}

		AranarthUtils.addVote(new AranarthVote(target.getUniqueId(), amount, System.currentTimeMillis()));

		int total = AranarthUtils.getAvailableVotePoints(target.getUniqueId());
		sender.sendMessage(ChatUtils.chatMessage(Lang.get("vpedit.success", "player", displayName, "amount", String.valueOf(amount), "total", String.valueOf(total))));

		if (target.isOnline()) {
			Player onlineTarget = target.getPlayer();
			if (onlineTarget != null) {
				String playerMsg = amount > 0
						? "&7Your vote points have been &aincreased &7by &e" + amount
						: "&7Your vote points have been &cdecreased &7by &e" + amount;
				onlineTarget.sendMessage(ChatUtils.chatMessage(playerMsg));
			}
		}
		return true;
	}
}
