package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to modify the speed of their walking or their flight.
 */
public class CommandSpeed {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() == 0 && aranarthPlayer.getArchitectRank() == 0) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return false;
			}

			if (args.length < 2) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("speed.must_enter")));
				return true;
			}

			float speed = 0;
			try {
				speed = Float.parseFloat(args[1]);
				if (speed < 1) {
					throw new NumberFormatException();
				}
				if (speed > 10) {
					throw new NumberFormatException();
				}
				speed = speed / 10;
			} catch (NumberFormatException e) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("speed.must_enter_range")));
				return true;
			}

			if (player.isFlying()) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("speed.set_success", "player", player.getName(), "value", String.valueOf(speed * 10))));
				player.setFlySpeed(speed);
			} else {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("speed.set_success", "player", player.getName(), "value", String.valueOf(speed * 10))));
				player.setWalkSpeed(speed);
			}
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}

}
