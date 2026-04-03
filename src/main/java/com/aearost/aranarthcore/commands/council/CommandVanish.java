package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return false;
			}

			aranarthPlayer.setVanished(!aranarthPlayer.isVanished());
			player.setInvisible(aranarthPlayer.isVanished());
			String message = aranarthPlayer.isVanished() ? "You have now vanished" : "You are no longer vanished";
			player.sendMessage(ChatUtils.chatMessage("&7" + message));



			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute that command!"));
			return true;
		}
	}

}
