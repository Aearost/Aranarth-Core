package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows for the player to lock the next clicked container.
 */
public class CommandLock {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			sender.sendMessage(ChatUtils.chatMessage("&7Right-click the container to be locked"));
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			aranarthPlayer.setIsLockingContainer(true);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        } else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be executed in-game!"));
        }
        return true;
    }

}
