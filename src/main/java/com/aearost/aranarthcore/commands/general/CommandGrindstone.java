package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

/**
 * Opens a grindstone GUI.
 */
public class CommandGrindstone implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.tables")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}

			MenuType.GRINDSTONE.builder()
					.checkReachable(false)
					.build(player)
					.open();
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
		}
		return true;
	}

}
