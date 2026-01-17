package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.RandomItem;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows players to input a pattern and randomize their block placements.
 */
public class CommandRandomizer {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.randomizer")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (args.length == 1) {
				if (aranarthPlayer.isRandomizing()) {
					player.sendMessage(ChatUtils.chatMessage("&7Blocks will no longer be randomized from the pattern"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&7Blocks will now be randomized from the pattern!"));
				}
				aranarthPlayer.isRandomizing(!aranarthPlayer.isRandomizing());
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else {
				if (!args[1].contains(",")) {
					player.sendMessage(ChatUtils.chatMessage("&cYou must specify more than one item!"));
					return true;
				}

				String[] components = args[1].split(",");
				List<RandomItem> randomItems = new ArrayList<>();
				for (String component : components) {
					String[] percentageItem = component.split("%");
					if (percentageItem.length != 2) {
						player.sendMessage(ChatUtils.chatMessage("&cAn incorrect format was entered!"));
						return true;
					}

					// Validates the percentage was entered correctly
					int percentage = 0;
					try {
						percentage = Integer.parseInt(percentageItem[0]);
					} catch (NumberFormatException e) {
						player.sendMessage(ChatUtils.chatMessage("&cYou entered an incorrect percentage!"));
						return true;
					}
					if (percentage == 0) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot enter a percentage of 0%!"));
						return true;
					}

					// Validates a proper item was entered after the percentage
					Material material = null;
					try {
						material = Material.valueOf(percentageItem[1].toUpperCase());
					} catch (IllegalArgumentException e) {
						player.sendMessage(ChatUtils.chatMessage("&cYou entered an invalid material!"));
						return true;
					}

					ItemStack item = new ItemStack(material, 1);
					RandomItem randomItem = new RandomItem(percentage, item);
					randomItems.add(randomItem);
				}

				// Ensures the total sums to 100%
				int totalPercentageSum = 0;
				for (RandomItem randomItem : randomItems) {
					totalPercentageSum += randomItem.getPercentage();
				}
				if (totalPercentageSum != 100) {
					player.sendMessage(ChatUtils.chatMessage("&cThe percentages do not sum to 100%!"));
					return true;
				} else {
					aranarthPlayer.setRandomItems(randomItems);
					player.sendMessage(ChatUtils.chatMessage("&7You have updated your randomizer pattern!"));
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to run this command!"));
			return true;
		}
	}

}
