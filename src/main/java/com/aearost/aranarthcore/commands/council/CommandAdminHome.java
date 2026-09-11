package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Allows council members to teleport to another player's home.
 */
public class CommandAdminHome {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.getCouncilRank() != 3) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
			return true;
		} else if (!aranarthPlayer.isInAdminMode()) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.must_be_admin")));
			return true;
		}

		if (args.length < 3) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac home <player> <home>")));
			return true;
		}

		OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
		if (target == null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
			return true;
		}
		AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
		if (targetAranarthPlayer == null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
			return true;
		}
		String homeName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

		for (Home home : targetAranarthPlayer.getHomes()) {
			if (homeName.equalsIgnoreCase(ChatUtils.stripColorFormatting(home.getName()))) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), home.getLocation(), true, home.getName(), Lang.get("admin.teleport_home_success", "player", targetAranarthPlayer.getNickname(), "home", home.getName()), success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_home_success", "player", targetAranarthPlayer.getNickname(), "home", home.getName())));
					} else {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.teleport_home_failed", "player", targetAranarthPlayer.getNickname())));
					}
				});
				return true;
			}
		}

		player.sendMessage(ChatUtils.chatMessage(Lang.get("home.not_found")));
		return true;
	}
}
