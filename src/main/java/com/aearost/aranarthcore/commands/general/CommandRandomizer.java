package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.RandomItem;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows players to input a pattern and randomize their block placements.
 */
public class CommandRandomizer implements CommandExecutor {

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
			if (!player.hasPermission("aranarth.randomizer")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
				return true;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (args.length == 0) {
				if (aranarthPlayer.isRandomizing()) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.block_no_longer")));
				} else {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.blocks_now")));
				}
				aranarthPlayer.isRandomizing(!aranarthPlayer.isRandomizing());
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else {
				if (!args[0].contains(",")) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.need_multiple_items")));
					return true;
				}

				String[] components = args[0].split(",");
				// Materials entered without a percentage, which evenly share whatever percentage is left over
				List<Material> unspecifiedMaterials = new ArrayList<>();
				List<RandomItem> randomItems = new ArrayList<>();
				int specifiedPercentageSum = 0;
				for (String component : components) {
					String materialName;
					int percentage = 0;
					if (component.contains("%")) {
						String[] percentageItem = component.split("%");
						if (percentageItem.length != 2) {
							player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.invalid_format")));
							return true;
						}

						// Validates the percentage was entered correctly
						try {
							percentage = Integer.parseInt(percentageItem[0]);
						} catch (NumberFormatException e) {
							player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.invalid_percent")));
							return true;
						}
						if (percentage <= 0) {
							player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.zero_percent")));
							return true;
						}
						materialName = percentageItem[1];
					} else {
						materialName = component;
					}

					// Validates a proper item was entered
					Material material;
					try {
						material = Material.valueOf(materialName.toUpperCase());
					} catch (IllegalArgumentException e) {
						player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.invalid_material")));
						return true;
					}

					if (component.contains("%")) {
						specifiedPercentageSum += percentage;
						randomItems.add(new RandomItem(percentage, new ItemStack(material, 1)));
					} else {
						unspecifiedMaterials.add(material);
					}
				}

				// Evenly splits the remaining percentage between the materials entered without one
				if (!unspecifiedMaterials.isEmpty()) {
					int remaining = 100 - specifiedPercentageSum;
					int count = unspecifiedMaterials.size();
					if (remaining < count) {
						player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.not_100_percent")));
						return true;
					}
					int base = remaining / count;
					int extra = remaining % count;
					for (int i = 0; i < count; i++) {
						int share = base + (i < extra ? 1 : 0);
						randomItems.add(new RandomItem(share, new ItemStack(unspecifiedMaterials.get(i), 1)));
					}
				}

				// Ensures the total sums to 100%
				int totalPercentageSum = 0;
				for (RandomItem randomItem : randomItems) {
					totalPercentageSum += randomItem.getPercentage();
				}
				if (totalPercentageSum != 100) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.not_100_percent")));
					return true;
				} else {
					aranarthPlayer.setRandomItems(randomItems);
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.pattern_updated")));
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
			return true;
		}
	}

}
