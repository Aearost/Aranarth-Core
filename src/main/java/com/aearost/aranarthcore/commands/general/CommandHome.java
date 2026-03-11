package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiHomes;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to teleport to one of their homes
 */
public class CommandHome implements CommandExecutor {

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
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (args.length >= 1) {
				StringBuilder homeNameBuilder = new StringBuilder();
				for (int i = 0; i < args.length; i++) {
					homeNameBuilder.append(args[i]);
					if (i < args.length - 1) {
						homeNameBuilder.append(" ");
					}
				}

				String homeName = homeNameBuilder.toString();
				for (Home home : aranarthPlayer.getHomes()) {
					if (homeName.equalsIgnoreCase(ChatUtils.stripColorFormatting(home.getName()))) {
						AranarthUtils.teleportPlayer(player, player.getLocation(), home.getLocation(), aranarthPlayer.isInAdminMode(), success -> {
							if (success) {
								player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + home.getName()));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + home.getName()));
							}
						});
						return true;
					}
				}
				player.sendMessage(ChatUtils.chatMessage("&cThis home could not be found!"));
			} else {
				if (aranarthPlayer.getHomes().isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&7You do not have any homes"));
				} else {
					GuiHomes gui = new GuiHomes(player);
					gui.openGui();
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
		}
		return false;
	}
}
