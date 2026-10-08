package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiSitColor;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.SitUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to sit on a cushion wherever they are, or select their cushion's color.
 */
public class CommandSit implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}

		if (args.length >= 1) {
			if (args[0].equalsIgnoreCase("color")) {
				if (!player.hasPermission("aranarth.sit.color")) {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("sit.no_color_perk")));
					return true;
				}
				new GuiSitColor(player).openGui();
			} else {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "sit [color]")));
			}
			return true;
		}

		if (SitUtils.isSitting(player)) {
			SitUtils.standUp(player);
			return true;
		}

		if (!player.isOnGround() || !SitUtils.sitOnGround(player)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.get("sit.cannot_sit")));
		}
		return true;
	}

}
