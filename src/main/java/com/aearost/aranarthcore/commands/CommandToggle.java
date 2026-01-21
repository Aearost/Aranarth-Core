package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Centralizes the logic for toggling different functionality in Aranarth.
 */
public class CommandToggle {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (args.length >= 2) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (args[1].equalsIgnoreCase("messages")) {
					if (player.hasPermission("aranarth.toggle.msg")) {
						if (aranarthPlayer.isTogglingMessages()) {
							aranarthPlayer.setTogglingMessages(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have re-enabled private messages"));
						} else {
							aranarthPlayer.setTogglingMessages(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have disabled private messages"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to toggle messages!"));
						return true;
					}
				} else if (args[1].equalsIgnoreCase("chat")) {
					if (player.hasPermission("aranarth.toggle.chat")) {
						if (aranarthPlayer.isTogglingChat()) {
							aranarthPlayer.setTogglingChat(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have re-enabled chat messages"));
						} else {
							aranarthPlayer.setTogglingChat(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have disabled chat messages"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to toggle chat!"));
						return true;
					}
				} else if (args[1].equalsIgnoreCase("teleport")) {
					if (player.hasPermission("aranarth.toggle.tp")) {
						if (aranarthPlayer.isTogglingTp()) {
							aranarthPlayer.setTogglingTp(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have re-enabled teleport requests"));
						} else {
							aranarthPlayer.setTogglingTp(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have disabled teleport requests"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to toggle teleport requests!"));
						return true;
					}
				} else if (args[1].equalsIgnoreCase("changeclaim")) {
					// Everyone has access
					if (aranarthPlayer.isTogglingChangeClaim()) {
						aranarthPlayer.setTogglingChangeClaim(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have re-enabled Dominion claim change messages"));
					} else {
						aranarthPlayer.setTogglingChangeClaim(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have disabled Dominion claim change messages"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[1].equalsIgnoreCase("bluefire")) {
					// Will need to remove blue fire entirely and re-enable based on perk
					String[] perks = aranarthPlayer.getPerks().split("\\*");
					// The blue fire perk
					if (perks[10].equals("1")) {
						if (aranarthPlayer.hasBlueFireDisabled()) {
							aranarthPlayer.setBlueFireDisabled(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7your blue fire"));
						} else {
							aranarthPlayer.setBlueFireDisabled(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7your blue fire"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						PermissionUtils.evaluatePlayerPermissions(player);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have access to the blue fire perk"));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac toggle <option>"));
				}
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac toggle <option>"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			return true;
		}
	}

}
