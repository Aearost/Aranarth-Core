package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "bending.no_avatar")));
				return true;
			}
			String element = AvatarUtils.getElementSymbol(currentAvatar.getUuid(), currentAvatar);
			String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(currentAvatar.getUuid()));
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "avatar.current", "element", element, "player", nickname)));
			return true;
		}

		if (!args[0].equalsIgnoreCase("set") && !args[0].equalsIgnoreCase("end")) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "avatar <set [player]|end>")));
			return true;
		}

		if (sender instanceof Player player && !player.hasPermission("aranarth.avatar.set")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
			return true;
		}

		// Ends the current avatar's reign
		if (args[0].equalsIgnoreCase("end")) {
			if (AvatarUtils.getCurrentAvatar() == null) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "bending.no_avatar")));
				return true;
			}
			AvatarUtils.removeCurrentAvatar("/avatar end by " + sender.getName());
			return true;
		}

		// Selects a new avatar, either the entered player or a random one
		if (AvatarUtils.getCurrentAvatar() != null) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "avatar.already_reigning")));
			return true;
		}

		UUID target = null;
		if (args.length >= 2) {
			target = AranarthUtils.getUUIDFromUsernameOrNickname(args[1]);
			if (target == null) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
				return true;
			}
		}

		String targetName = args.length >= 2 ? args[1] : "";
		AvatarUtils.logInfo(sender.getName() + " ran /avatar set " + targetName);
		AvatarUtils.selectAvatar(target, result -> {
			AvatarUtils.logInfo("/avatar set by " + sender.getName() + " finished with " + result);
			switch (result) {
				case ALREADY_REIGNING -> sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "avatar.already_reigning")));
				case NOT_ELIGIBLE -> sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "avatar.not_eligible", "player", targetName)));
				case NONE_FOUND, BUSY -> sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "admin.no_avatar_selected")));
				default -> {
				}
			}
		});
		return true;
	}

}
