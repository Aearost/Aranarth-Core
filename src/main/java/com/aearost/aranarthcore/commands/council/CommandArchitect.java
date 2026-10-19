package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sets the player in architect mode, allowing them to bypass the building protections.
 */
public class CommandArchitect {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			if (aranarthPlayer.getCouncilRank() == 0 && aranarthPlayer.getArchitectRank() == 0) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}

			if (aranarthPlayer.isInArchitectMode()) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("architect.mode_exit")));
				aranarthPlayer.setInArchitectMode(false);
			} else {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("architect.mode_enter")));
				aranarthPlayer.setInArchitectMode(true);
			}
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}

}
