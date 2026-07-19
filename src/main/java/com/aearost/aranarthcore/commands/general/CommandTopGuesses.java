package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiTopGuesses;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the player with a GUI of the top chat game guesses.
 */
public class CommandTopGuesses implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			GuiTopGuesses.open(player, 0);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}
}
