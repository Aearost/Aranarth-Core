package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiCompressor;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Compresses the player's current inventory.
 */
public class CommandCompressor implements CommandExecutor {

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
			if (!player.hasPermission("aranarth.compress")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return false;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			if (args.length == 0) {
				GuiCompressor gui = new GuiCompressor(player);
				gui.openGui();
				return true;
			} else if (args[0].equalsIgnoreCase("toggle")) {
				if (aranarthPlayer.isCompressingItems()) {
					aranarthPlayer.setCompressingItems(false);
					player.sendMessage(ChatUtils.chatMessage("&7You are no longer compressing items"));
				} else {
					aranarthPlayer.setCompressingItems(true);
					player.sendMessage(ChatUtils.chatMessage("&7You are now compressing items"));
				}
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/compress [toggle]"));
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed by a player!"));
		}
		return false;
	}
}
