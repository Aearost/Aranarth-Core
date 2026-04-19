package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiVoteShop;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the player with a GUI of the vote shop.
 */
public class CommandVoteShop implements CommandExecutor {

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
			String worldName = player.getWorld().getName();
			if (worldName.startsWith("spawn") || worldName.startsWith("world")
					|| worldName.startsWith("smp") || worldName.startsWith("resource")){
				GuiVoteShop gui = new GuiVoteShop(player);
				gui.openGui();
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cThis command can only be used in &eSurvival!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}
}
