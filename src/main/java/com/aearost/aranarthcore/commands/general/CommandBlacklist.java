package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiBlacklist;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to prevent specified items from being picked up.
 */
public class CommandBlacklist implements CommandExecutor {

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
			if (!player.hasPermission("aranarth.blacklist")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return false;
			}

            if (args.length == 0) {
				GuiBlacklist gui = new GuiBlacklist(player);
				gui.openGui();
				return true;
			} else {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (args[0].equals("ignore")) {
					aranarthPlayer.setBlacklistingMethod(0);
					player.sendMessage(ChatUtils.chatMessage("&7You will now ignore blacklisted items"));
				} else if (args[0].equals("trash")) {
					aranarthPlayer.setBlacklistingMethod(1);
					player.sendMessage(ChatUtils.chatMessage("&7You will now trash blacklisted items"));
				} else if (args[0].equals("off")) {
					aranarthPlayer.setBlacklistingMethod(-1);
					player.sendMessage(ChatUtils.chatMessage("&7Your blacklist is now disabled"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid toggle option!"));
					return false;
				}
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			}
        } else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis must be executed in-game!"));
			return false;
		}
    }
}
