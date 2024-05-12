package com.aearost.aranarthcore.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import com.aearost.aranarthcore.gui.GuiPotions;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandPotion {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (args.length == 1) {
				player.sendMessage(
						ChatUtils.chatMessageError("You must specify a sub-command! /ac potion <sub-command>"));
				return false;
			} else {
				if (args[1].equals("list")) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

					if (Objects.nonNull(aranarthPlayer.getPotions())) {
						List<ItemStack> potions = aranarthPlayer.getPotions();
						
						if (potions.size() == 0) {
							player.sendMessage(ChatUtils.chatMessage("&7You don't have any stored potions!"));
							return false;
						}

						// Counts how many of each potion there is
						HashMap<String, Integer> amountOfPotions = new HashMap<>();
						for (ItemStack potionToCount : potions) {
							PotionMeta meta = (PotionMeta) potionToCount.getItemMeta();
							String potionName = ChatUtils.getFormattedItemName(meta.getBasePotionType().name());
							String finalizedName = addPotionConsumptionMethodToName(potionToCount, potionName);

							if (amountOfPotions.containsKey(finalizedName)) {
								Integer newAmount = Integer.valueOf(amountOfPotions.get(finalizedName).intValue() + 1);
								amountOfPotions.put(finalizedName, newAmount);
							} else {
								amountOfPotions.put(finalizedName, 1);
							}
						}
						
						// Sorts all potion names alphabetically
						SortedSet<String> sortedMap = new TreeSet<String>(amountOfPotions.keySet());

						// Displays the potions
						player.sendMessage(ChatUtils.chatMessage("&7Below are the potions you have stored:"));
						// Iterate over sortedMap but display values from amountOfPotions
						for (String potionName : sortedMap) {
							player.sendMessage(
									ChatUtils.chatMessage("&e" + potionName + " x" + amountOfPotions.get(potionName)));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&7You don't have any stored potions!"));
					}

				} else if (args[1].equals("add")) {

					GuiPotions gui = new GuiPotions(player);
					gui.openGui();

				} else if (args[1].equals("remove")) {
					// Check next sub-commands to ensure valid syntax
					// Also will need auto-completer to display all potions that they have
					// /ac potion remove HEALTH_BOOST 7 (assuming this is the accurate name)
					// Most likely comes from PotionEffectType (for effect) and PotionEffect (for
					// amplifier)
				} else {
					player.sendMessage(ChatUtils.chatMessageError("Please enter a valid potion sub-command!"));
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
		}
		return false;
	}

	private static String addPotionConsumptionMethodToName(ItemStack potion, String potionName) {
		String[] partsOfName = potionName.split(" ");
		String finalName = "";
		if (potionName.startsWith("Long")) {
			if (potion.getType() == Material.POTION) {
				finalName = "Long Potion of ";
			} else if (potion.getType() == Material.SPLASH_POTION) {
				finalName = "Long Splash Potion of ";
			} else if (potion.getType() == Material.LINGERING_POTION) {
				finalName = "Long Lingering Potion of ";
			}
		} else if (potionName.startsWith("Strong")) {
			if (potion.getType() == Material.POTION) {
				finalName = "Strong Potion of ";
			} else if (potion.getType() == Material.SPLASH_POTION) {
				finalName = "Strong Splash Potion of ";
			} else if (potion.getType() == Material.LINGERING_POTION) {
				finalName = "Strong Lingering Potion of ";
			}
		} else {
			if (potion.getType() == Material.POTION) {
				finalName = "Potion of ";
			} else if (potion.getType() == Material.SPLASH_POTION) {
				finalName = "Splash Potion of ";
			} else if (potion.getType() == Material.LINGERING_POTION) {
				finalName = "Lingering Potion of ";
			}
		}
		
		// Handles formatting the actual potion name
		for (int i = 0; i < partsOfName.length; i++) {
			if (partsOfName[i].equals("Long") || partsOfName[0].equals("Strong")) {
				continue;
			} else {
				if (i == partsOfName.length - 1) {
					finalName += partsOfName[i];
				} else {
					finalName += partsOfName[i] + " ";
				}
			}
			
		}
		return finalName;
	}

}
