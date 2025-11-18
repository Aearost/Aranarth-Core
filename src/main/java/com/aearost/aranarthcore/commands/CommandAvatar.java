package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
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
		if (args.length == 1) {
			Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
			if (currentAvatar == null) {
				sender.sendMessage(ChatUtils.chatMessage("&7&oThere is currently no Avatar..."));
				return true;
			}
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(currentAvatar.getUuid());
			sender.sendMessage(ChatUtils.translateToColor("&5&l&oThe current Avatar is &d" + aranarthPlayer.getNickname()));
			return true;
		} else {
			if (sender instanceof Player player) {
				if (!player.hasPermission("aranarth.avatar.set")) {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
					return true;
				}
			}
			boolean wasAvatarFound = AvatarUtils.selectAvatar();
			if (!wasAvatarFound) {
				sender.sendMessage(ChatUtils.chatMessage("&7No Avatar was selected, will try again the next execution"));
			}
			return true;
		}
	}

}
