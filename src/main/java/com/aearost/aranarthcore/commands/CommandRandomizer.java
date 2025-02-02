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
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (args.length == 1) {
				if (aranarthPlayer.getIsRandomizerToggled()) {
					player.sendMessage(ChatUtils.chatMessage("&7You will now randomize blocks from your pattern list!"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&7You will no longer randomize blocks from your pattern list."));
				}
				aranarthPlayer.setIsRandomizerToggled(!aranarthPlayer.getIsRandomizerToggled());
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else {
				String[] components = args[1].split(",");
				List<RandomItem> randomItems = new ArrayList<>();
				for (String component : components) {
					String[] percentageItem = component.split("%");
					int percentage = 0;
					try {
						percentage = Integer.parseInt(percentageItem[0]);
					} catch (NumberFormatException e) {
						player.sendMessage(ChatUtils.chatMessage("&cYou entered an incorrect percentage!"));
						return false;
					}
					Material material = null;
					try {
						material = Material.valueOf(percentageItem[1]);
					} catch (IllegalArgumentException e) {
						player.sendMessage(ChatUtils.chatMessage("&cYou entered an invalid material!"));
						return false;
					}
					ItemStack item = new ItemStack(material, 1);
					RandomItem randomItem = new RandomItem(percentage, item);
					randomItems.add(randomItem);
				}
				aranarthPlayer.setRandomItems(randomItems);
				player.sendMessage(ChatUtils.chatMessage("&7You have updated your randomizer pattern!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to run this command!"));
			return false;
		}
	}

}
