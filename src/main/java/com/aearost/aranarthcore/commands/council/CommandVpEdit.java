package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.AranarthVote;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}
		}

		if (args.length != 3) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac vpedit <username> <+amount|-amount>"));
			return true;
		}

		String amountArg = args[2];
		if (!amountArg.startsWith("+") && !amountArg.startsWith("-")) {
			sender.sendMessage(ChatUtils.chatMessage("&cThe amount must be increasing or decreasing!"));
			return true;
		}

		int amount;
		try {
			amount = Integer.parseInt(amountArg);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatUtils.chatMessage("&cThat is not a valid number!"));
			return true;
		}

		if (amount == 0) {
			sender.sendMessage(ChatUtils.chatMessage("&cAmount cannot be zero!"));
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
			sender.sendMessage(ChatUtils.chatMessage("&cThat player was not found!"));
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

		String sign = amount > 0 ? "&a+" : "&c";
		sender.sendMessage(ChatUtils.chatMessage("&7Updated &e" + displayName + "&7's vote points by " + sign + amount));

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
