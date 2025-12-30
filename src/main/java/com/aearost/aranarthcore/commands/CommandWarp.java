package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiWarps;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to warp to the input warp name.
 */
public class CommandWarp {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (args.length == 1) {
				if (AranarthUtils.getWarps().isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&7There are currently no warps"));
					return true;
				}
				GuiWarps gui = new GuiWarps(player);
				gui.openGui();
				return true;
			} else if (args.length == 2) {
				if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("delete")) {
					if (player.hasPermission("aranarth.warp.modify")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou must specify a warp name"));
						return true;
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
						return true;
					}
				} else {
					for (Home warp : AranarthUtils.getWarps()) {
						if (ChatUtils.stripColorFormatting(warp.getName()).equalsIgnoreCase(args[1])) {
							AranarthUtils.teleportPlayer(player, player.getLocation(), warp.getLocation());
							player.sendMessage(ChatUtils.chatMessage("&7You have warped to &e" + warp.getName()));
							return true;
						}
					}
					player.sendMessage(ChatUtils.chatMessage("&cThe warp &e" + args[1] + " &ccould not be found!"));
					return true;
				}
			} else {
				if (args[1].equalsIgnoreCase("create")) {
					if (player.hasPermission("aranarth.warp.modify")) {
						String warpName = ChatUtils.translateToColor(args[2]);
						warpName = ChatUtils.removeSpecialCharacters(warpName);
						for (Home warp : AranarthUtils.getWarps()) {
							if (ChatUtils.stripColorFormatting(warp.getName()).equalsIgnoreCase(ChatUtils.stripColorFormatting(warpName))) {
								player.sendMessage(ChatUtils.chatMessage("&cThe warp &e" + warpName + " &calready exists!"));
								return true;
							}
						}

						Home warp = new Home(warpName, player.getLocation(), Material.BARRIER);
						AranarthUtils.addWarp(warp);
						player.sendMessage(ChatUtils.chatMessage("&7You have added the warp &e" + warp.getName()));
						return true;
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
						return true;
					}
				} else if (args[1].equalsIgnoreCase("delete")) {
					if (player.hasPermission("aranarth.warp.modify")) {
						Home warpToDelete = null;
						for (Home warp : AranarthUtils.getWarps()) {
							if (ChatUtils.stripColorFormatting(warp.getName()).equalsIgnoreCase(args[2])) {
								warpToDelete = warp;
								break;
							}
						}

						if (warpToDelete != null) {
							player.sendMessage(ChatUtils.chatMessage("&7You have removed the warp &e" + warpToDelete.getName()));
							AranarthUtils.removeWarp(warpToDelete);
							return true;
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cThe warp &e" + args[2] + " &ccould not be found!"));
							return true;
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
						return true;
					}
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
		return false;
	}
}
