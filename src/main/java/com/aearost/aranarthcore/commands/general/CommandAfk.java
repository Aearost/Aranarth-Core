package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AfkLocation;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Manually sets the player to be AFK.
 */
public class CommandAfk implements CommandExecutor {

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
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			// Entering AFK
			if (aranarthPlayer.getAfkLocation() == null
					|| aranarthPlayer.getAfkLocation().getSeconds() < AranarthUtils.getAfkSecondsAmount()) {
				// + 10 seconds to avoid double AFK messages
				aranarthPlayer.setAfkLocation(new AfkLocation(player.getLocation(), AranarthUtils.getAfkSecondsAmount() + 10));
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				AranarthUtils.toggleAfkStatus(player.getUniqueId(), true);
			}
			// Exiting AFK
			else {
				AranarthUtils.toggleAfkStatus(player.getUniqueId(), false);
			}

			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed in-game!"));
			return false;
		}
	}
}
