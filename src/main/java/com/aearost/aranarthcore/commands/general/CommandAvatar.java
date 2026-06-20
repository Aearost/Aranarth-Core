package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the list of avatars.
 */
public class CommandAvatar implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		// Lists the current avatar
		if (args.length == 0) {
			Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
			if (currentAvatar == null) {
				sender.sendMessage(ChatUtils.chatMessage("&7&oAranarth is currently without an Avatar..."));
				return true;
			}
			String element = AvatarUtils.getElementSymbol(currentAvatar.getUuid(), currentAvatar);
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(currentAvatar.getUuid());
			sender.sendMessage(ChatUtils.chatMessage("&5&l&oThe current Avatar is " + element + " &d" + aranarthPlayer.getNickname() + " " + element));
			return true;
		} else {
			if (args[0].equalsIgnoreCase("set")) {
				if (sender instanceof Player player) {
					if (!player.hasPermission("aranarth.avatar.set")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
						return false;
					}
				}
				boolean wasAvatarFound = AvatarUtils.selectAvatar(true);
				if (!wasAvatarFound) {
					sender.sendMessage(ChatUtils.chatMessage("&7No Avatar was selected, will try again the next execution"));
				}
			}
			return false;
		}
	}

}
