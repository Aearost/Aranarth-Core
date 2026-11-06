package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.Lang;
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
		if (sender instanceof Player || !AranarthCore.isSmpServer()) {
			for (int i = 0; i < 128; i++) {
				Bukkit.broadcastMessage("");
			}

			if (sender instanceof Player player) {
				// The console runs this on every server, so only a player's clear is relayed
				// Published before the notice below so the other server clears before showing it
				if (NetworkManager.isActive()) {
					NetworkManager.getInstance().publishClearChat();
				}
				DiscordUtils.createNotification(AranarthUtils.getNickname(player) + " has cleared the chat", player.getUniqueId());
				Lang.broadcast("chat.cleared", "player", AranarthUtils.getNickname(player));
			} else {
				Lang.broadcast("general.chat_cleared");
			}
		}
		return true;
	}
}
