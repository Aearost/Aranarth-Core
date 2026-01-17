package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiBlacklist;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to prevent specified items from being picked up.
 */
public class CommandBlacklist {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.blacklist")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}

            if (args.length == 1) {
				GuiBlacklist gui = new GuiBlacklist(player);
				gui.openGui();
				return true;
			} else {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (args[1].equals("ignore")) {
					aranarthPlayer.setDeletingBlacklistedItems(false);
					player.sendMessage(ChatUtils.chatMessage("&7You will now ignore blacklisted items"));
				} else if (args[1].equals("trash")) {
					aranarthPlayer.setDeletingBlacklistedItems(true);
					player.sendMessage(ChatUtils.chatMessage("&7You will now trash blacklisted items"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid blacklist sub-command!"));
					return  false;
				}
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			}
        } else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis must be executed in-game!"));
			return true;
		}
    }
}
