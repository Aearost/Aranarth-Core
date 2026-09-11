package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to vanish and appear as if they weren't online in tab.
 */
public class CommandVanish {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() < 3) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return false;
			}

			aranarthPlayer.setVanished(!aranarthPlayer.isVanished());
			player.setInvisible(aranarthPlayer.isVanished());
			String langKey = aranarthPlayer.isVanished() ? "vanish.now_vanished" : "vanish.no_longer_vanished";
			player.sendMessage(ChatUtils.chatMessage(Lang.get(langKey)));



			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}

}
