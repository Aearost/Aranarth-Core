package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Clears the entire chat.
 */
public class CommandClearChat {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		for (int i = 0; i < 128; i++) {
			Bukkit.broadcastMessage("");
		}

		if (sender instanceof Player player) {
			DiscordUtils.createNotification(AranarthUtils.getNickname(player) + " has cleared the chat", player.getUniqueId());
			Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getNickname(player) + " &7has cleared the chat"));
		} else {
			Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The chat has been cleared"));
		}
		return true;
	}
}
