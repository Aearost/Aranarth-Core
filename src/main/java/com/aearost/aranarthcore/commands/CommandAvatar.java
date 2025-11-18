package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the list of avatars.
 */
public class CommandAvatar {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		// Lists the current avatar
		if (args.length == 0) {
			Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(currentAvatar.getUuid());
			sender.sendMessage(ChatUtils.translateToColor("&5&l&oThe current Avatar is &d" + aranarthPlayer.getNickname()));
		} else {
			if (sender instanceof Player player) {
				if (!player.hasPermission("aranarth.avatar.set")) {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
					return true;
				}
			}

			AvatarUtils.selectAvatar();
		}

		return false;
	}

}
