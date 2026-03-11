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
			aranarthPlayer.setAfkLocation(new AfkLocation(player.getLocation(), 300));
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			AranarthUtils.toggleAfkStatus(player.getUniqueId());
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed in-game!"));
			return false;
		}
	}
}
