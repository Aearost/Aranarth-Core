package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows for the player to unlock the next clicked Locked Container.
 */
public class CommandUnlock {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			sender.sendMessage(ChatUtils.chatMessage("&7Right-click the container to be unlocked"));
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			aranarthPlayer.setIsUnlockingContainer(true);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        } else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be executed in-game!"));
        }
        return true;
    }

}
