package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.HologramUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to message the council.
 */
public class CommandHologram {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() != 3) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return false;
			}

			applyLogic(sender, args);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return true;
		}
	}

	private static void applyLogic(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		if (args.length < 3) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac hologram <create | edit | delete> <x,y,z> [Text]"));
		} else {
			// Builds the text
			StringBuilder msg = new StringBuilder();
			for (int i = 3; i < args.length; i++) {
				msg.append(args[i]);
				if (i < args.length - 1) {
					msg.append(" ");
				}
			}
			String assembledMsg = msg.toString();

			// Delete does not require text but the other sub-commands do
			if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("modify")) {
				if (args.length == 3) {
					sender.sendMessage(ChatUtils.chatMessage("&cYou must specify the text contents!"));
					return;
				}

				if (ChatUtils.stripColorFormatting(assembledMsg).isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&cThe entered text is invalid!"));
					return;
				}
			}

			// Converts the coordinates to int variables
			String[] coordinates = args[2].split(",");
			int x, z = 0;
			double y = 0;
			try {
				x = Integer.parseInt(coordinates[0]);
				y = Double.parseDouble(coordinates[1]);
				z = Integer.parseInt(coordinates[2]);
			} catch (Exception e) {
				player.sendMessage(ChatUtils.chatMessage("&cThe entered coordinates are invalid!"));
				return;
			}

			Location location = new Location(player.getWorld(), x, y, z);
			if (args[1].equalsIgnoreCase("create")) {
				boolean wasCreated = HologramUtils.createHologram(location, assembledMsg);
				if (wasCreated) {
					player.sendMessage(ChatUtils.chatMessage("&7The hologram was created successfully"));
					player.playSound(player, Sound.BLOCK_LAVA_POP, 1.5F, 2F);
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThe hologram could not be created!"));
				}
			} else if (args[1].equalsIgnoreCase("modify")) {

			} else if (args[1].equalsIgnoreCase("delete")) {
				boolean wasDeleted = HologramUtils.removeHologram(location);
				if (wasDeleted) {
					player.sendMessage(ChatUtils.chatMessage("&7The hologram was deleted successfully"));
					player.playSound(player, Sound.BLOCK_LAVA_POP, 1.5F, 1.3F);
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThe hologram could not be removed!"));
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou have entered an invalid sub-command!"));
			}
		}
	}
}
