package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.AranarthVote;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Allows the player to transfer vote points to another player.
 */
public class CommandVpTransfer implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}

		if (args.length != 2) {
			player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax! &e/vptransfer <player> <amount>"));
			return true;
		}

		int amount;
		try {
			amount = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			player.sendMessage(ChatUtils.chatMessage("&cThat is not a valid number!"));
			return true;
		}

		if (amount <= 0) {
			player.sendMessage(ChatUtils.chatMessage("&cAmount must be greater than zero!"));
			return true;
		}

		UUID receiverUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
		if (receiverUuid == null) {
			player.sendMessage(ChatUtils.chatMessage("&e" + args[0] + " &ccould not be found"));
			return true;
		}

		if (receiverUuid.equals(player.getUniqueId())) {
			player.sendMessage(ChatUtils.chatMessage("&cYou cannot transfer vote points to yourself!"));
			return true;
		}

		AranarthPlayer senderAp = AranarthUtils.getPlayer(player.getUniqueId());
		AranarthPlayer receiverAp = AranarthUtils.getPlayer(receiverUuid);

		int available = AranarthUtils.getAvailableVotePoints(player.getUniqueId());
		if (amount > available) {
			player.sendMessage(ChatUtils.chatMessage("&cYou only have &e" + available + " &cvote points available!"));
			return true;
		}

		String receiverNickname = receiverAp.getNickname();
		long timestamp = System.currentTimeMillis();

		// Deduct from sender
		senderAp.setVotePointsSpent(senderAp.getVotePointsSpent() + amount);
		PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());

		// Credit receiver in-memory
		AranarthUtils.addVote(new AranarthVote(receiverUuid, amount, timestamp));

		Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
				PersistenceUtils.syncVoteKeysForPlayerToDatabase(receiverUuid)
		);

		player.sendMessage(ChatUtils.chatMessage("&7You have transferred &e" + amount + " &7vote points to &e" + receiverNickname));

		// Notify receiver
		boolean receiverOnThisServer = Bukkit.getPlayer(receiverUuid) != null;
		if (receiverOnThisServer) {
			Player localReceiver = Bukkit.getPlayer(receiverUuid);
			if (localReceiver != null) {
				localReceiver.sendMessage(ChatUtils.chatMessage("&7You have received &e" + amount + " &7vote points from &e" + senderAp.getNickname()));
			}
		} else if (NetworkManager.isActive()) {
			NetworkPlayer networkReceiver = NetworkManager.getInstance().getRemoteRoster().get(receiverUuid);
			if (networkReceiver != null) {
				NetworkManager.getInstance().publishVpTransfer(receiverUuid, amount, timestamp, senderAp.getNickname());
			}
		}

		return true;
	}
}
