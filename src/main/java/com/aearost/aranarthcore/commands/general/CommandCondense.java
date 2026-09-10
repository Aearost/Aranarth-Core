package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Manually compresses all compressible items in the player's inventory.
 */
public class CommandCondense implements CommandExecutor {

	/**
	 * @param sender  The user that entered the command.
	 * @param command The command itself.
	 * @param alias   The alias of the command.
	 * @param args    The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.compressor")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return false;
			}

			if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
				player.sendMessage(ChatUtils.chatMessage("&cYou can only condense your inventory in the Survival world!"));
				return false;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (!aranarthPlayer.isCompressingItems()) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have the compressor enabled! Use &e/compressor &cto configure it."));
				return false;
			}

			AranarthUtils.compressPlayerInventory(player, aranarthPlayer, null);
			player.sendMessage(ChatUtils.chatMessage("&aCondensed your inventory!"));
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed by a player!"));
		}
		return false;
	}
}
