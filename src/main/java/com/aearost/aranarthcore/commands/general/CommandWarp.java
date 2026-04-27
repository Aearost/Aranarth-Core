package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiWarps;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to warp to the input warp name.
 */
public class CommandWarp implements CommandExecutor {

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
			if (args.length == 0) {
				if (AranarthUtils.getWarps().isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&7There are currently no warps"));
					return true;
				}
				GuiWarps gui = new GuiWarps(player);
				gui.openGui();
				return true;
			} else if (args.length == 1) {
				if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("delete")) {
					if (player.hasPermission("aranarth.warp.modify")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou must specify a warp name"));
						return true;
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
						return true;
					}
				} else {
					for (Home warp : AranarthUtils.getWarps()) {
						if (ChatUtils.stripColorFormatting(warp.getName()).equalsIgnoreCase(args[0])) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
							AranarthUtils.teleportPlayer(player, player.getLocation(), warp.getLocation(), aranarthPlayer.isInAdminMode(), success -> {
								if (success) {
									player.sendMessage(ChatUtils.chatMessage("&7You have warped to &e" + warp.getName()));
								} else {
									player.sendMessage(ChatUtils.chatMessage("&cYou could not warp to &e" + warp.getName()));
								}
							});
							return true;
						}
					}
					player.sendMessage(ChatUtils.chatMessage("&cThe warp &e" + args[0] + " &ccould not be found!"));
					return true;
				}
			} else {
				if (args[0].equalsIgnoreCase("create")) {
					if (player.hasPermission("aranarth.warp.modify")) {
						String warpName = ChatUtils.translateToColor(args[1]);
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
				} else if (args[0].equalsIgnoreCase("delete")) {
					if (player.hasPermission("aranarth.warp.modify")) {
						Home warpToDelete = null;
						for (Home warp : AranarthUtils.getWarps()) {
							if (ChatUtils.stripColorFormatting(warp.getName()).equalsIgnoreCase(args[1])) {
								warpToDelete = warp;
								break;
							}
						}

						if (warpToDelete != null) {
							player.sendMessage(ChatUtils.chatMessage("&7You have removed the warp &e" + warpToDelete.getName()));
							AranarthUtils.removeWarp(warpToDelete);
							return true;
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cThe warp &e" + args[1] + " &ccould not be found!"));
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
