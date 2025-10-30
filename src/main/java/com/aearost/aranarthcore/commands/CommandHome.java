package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiHomes;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to teleport to one of their homes
 */
public class CommandHome {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (args.length >= 2) {
				StringBuilder homeNameBuilder = new StringBuilder();
				for (int i = 1; i < args.length; i++) {
					homeNameBuilder.append(args[i]);
					if (i < args.length - 1) {
						homeNameBuilder.append(" ");
					}
				}
				String homeName = homeNameBuilder.toString();


				for (Home home : aranarthPlayer.getHomes()) {
					if (homeName.equalsIgnoreCase(ChatUtils.stripColorFormatting(home.getHomeName()))) {
						player.teleport(home.getLocation());
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + home.getHomeName()));
						return true;
					}
				}
				player.sendMessage(ChatUtils.chatMessage("&cThis home could not be found!"));
				return true;
			} else {
				if (aranarthPlayer.getHomes().isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have any homes!"));
					return true;
				} else {
					GuiHomes gui = new GuiHomes(player);
					gui.openGui();
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}
}
