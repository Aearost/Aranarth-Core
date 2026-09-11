package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Toggles spy mode, allowing the player to intercept private messages and dominion chat.
 */
public class CommandSpy {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			if (aranarthPlayer.getCouncilRank() != 3) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}

			if (aranarthPlayer.isInSpyMode()) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("spy.mode_exit")));
				aranarthPlayer.setInSpyMode(false);
			} else {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("spy.mode_enter")));
				aranarthPlayer.setInSpyMode(true);
			}
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}
}
