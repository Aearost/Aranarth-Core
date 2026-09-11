package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("bending.no_avatar")));
				return true;
			}
			String element = AvatarUtils.getElementSymbol(currentAvatar.getUuid(), currentAvatar);
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(currentAvatar.getUuid());
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("avatar.current", "element", element, "player", aranarthPlayer.getNickname())));
			return true;
		} else {
			if (args[0].equalsIgnoreCase("set")) {
				if (sender instanceof Player player) {
					if (!player.hasPermission("aranarth.avatar.set")) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
						return false;
					}
				}
				boolean wasAvatarFound = AvatarUtils.selectAvatar(true);
				if (!wasAvatarFound) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.no_avatar_selected")));
				}
			}
			return false;
		}
	}

}
