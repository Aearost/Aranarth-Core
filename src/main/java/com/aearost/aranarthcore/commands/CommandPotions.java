package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiPotions;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Allows players to add to and view their potion inventory.
 */
public class CommandPotions {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.getLocation().getWorld().getName().startsWith("world") && !player.getLocation().getWorld().getName().startsWith("smp")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou must be in Survival to use this command!"));
				return true;
			}

            if (args.length == 1) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (aranarthPlayer.getPotions() == null || aranarthPlayer.getPotions().isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&7You don't have any stored potions!"));
					return true;
				}

				GuiPotions gui = new GuiPotions(player, 0);
				gui.openGui();
				return true;
			} else {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                switch (args[1]) {
                    case "list" -> {
                        if (aranarthPlayer.getPotions() != null) {
							HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();

                            if (potions.isEmpty()) {
                                player.sendMessage(ChatUtils.chatMessage("&7You don't have any stored potions!"));
                                return true;
                            }

							HashMap<String, HashMap<ItemStack, Integer>> amountOfPotions = AranarthUtils.getPlayerPotionNames(player);

                            // Sorts all potion names alphabetically
                            SortedSet<String> sortedMap = new TreeSet<>(amountOfPotions.keySet());

                            player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lYour Potions &8- - -"));
                            // Iterate over sortedMap but display values from amountOfPotions
                            for (String potionName : sortedMap) {
								// Should only ever be one value in here
								for (ItemStack potion : amountOfPotions.get(potionName).keySet()) {
									player.sendMessage(
											ChatUtils.translateToColor("&e" + potionName + " &6x" + amountOfPotions.get(potionName).get(potion)));
								}
                            }
							return true;
                        } else {
                            player.sendMessage(ChatUtils.chatMessage("&7You don't have any stored potions!"));
							return true;
                        }
                    }
                    case "add" -> {
						// Prevents adding potions when already at the limit
						if (aranarthPlayer.getPotions() != null && !aranarthPlayer.getPotions().isEmpty()) {
							if (AranarthUtils.getPlayerStoredPotionNum(player) >= AranarthUtils.getMaxPotionNum(player)) {
								player.sendMessage(ChatUtils.chatMessage("&cYour potions pouch is full!"));
								return true;
							}
						}

                        GuiPotions gui = new GuiPotions(player, 1);
                        gui.openGui();
						return true;
                    }
                    case "remove" -> {
						if (player.getWorld().getName().startsWith("world") || player.getWorld().getName().startsWith("smp")) {
							if (args.length >= 3) {
								HashMap<ItemStack, Integer> potions = AranarthUtils.getPlayer(player.getUniqueId()).getPotions();
								if (potions == null || potions.isEmpty()) {
									player.sendMessage(ChatUtils.chatMessage("&7You do not have any stored potions"));
									return true;
								}

								try {
									int quantity = Integer.parseInt(args[2]);
									if (quantity > 0) {
										aranarthPlayer.setPotionQuantityToRemove(quantity);
										AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
										GuiPotions gui = new GuiPotions(player, -1);
										gui.openGui();
									} else {
										player.sendMessage(ChatUtils.chatMessage("&cYou must enter a valid quantity!"));
									}
								} catch (NumberFormatException e) {
									player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: /ac potions remove <qty>"));
								}
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: /ac potions remove <qty>"));
							}
							return true;
						}
                    }
                    default ->
                            player.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid potion sub-command!"));
                }
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return true;
		}
		return false;
	}

}
