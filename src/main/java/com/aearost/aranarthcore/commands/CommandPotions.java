package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiPotions;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

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
				GuiPotions gui = new GuiPotions(player, 0);
				gui.openGui();
				return true;
			} else {
                switch (args[1]) {
                    case "list" -> {
                        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

                        if (aranarthPlayer.getPotions() != null) {
							HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();

                            if (potions.isEmpty()) {
                                player.sendMessage(ChatUtils.chatMessage("&7You don't have any stored potions!"));
                                return true;
                            }

							HashMap<String, Integer> amountOfPotions = new HashMap<>();
							for (ItemStack potion : potions.keySet()) {
								String potionName = null;
								if (potion.getType() == Material.AIR) {
									potions.remove(potion);
									continue;
								}

								// If it is an mcMMO potion
								if (potion.hasItemMeta() && potion.getItemMeta().hasItemName()) {
									potionName = potion.getItemMeta().getItemName();
								} else {
									PotionMeta meta = (PotionMeta) potion.getItemMeta();
									if (meta != null) {
										potionName = addPotionConsumptionMethodToName(potion, ChatUtils.getFormattedItemName(meta.getBasePotionType().name()));
									}
								}

								amountOfPotions.put(potionName, potions.get(potion));
							}

                            // Sorts all potion names alphabetically
                            SortedSet<String> sortedMap = new TreeSet<>(amountOfPotions.keySet());

                            // Displays the potions
                            player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lYour Potions &8- - -"));
                            // Iterate over sortedMap but display values from amountOfPotions
                            for (String potionName : sortedMap) {
                                player.sendMessage(
                                        ChatUtils.translateToColor("&e" + potionName + " &6x" + amountOfPotions.get(potionName)));
                            }
							return true;
                        } else {
                            player.sendMessage(ChatUtils.chatMessage("&7You don't have any stored potions!"));
							return true;
                        }
                    }
                    case "add" -> {
                        GuiPotions gui = new GuiPotions(player, 1);
                        gui.openGui();
						return true;
                    }
                    case "remove" -> {
						if (player.getWorld().getName().startsWith("world") || player.getWorld().getName().startsWith("smp")) {
							if (args.length >= 3) {
								HashMap<ItemStack, Integer> potions = AranarthUtils.getPlayer(player.getUniqueId()).getPotions();
								if (potions == null || potions.isEmpty()) {
									player.sendMessage(ChatUtils.chatMessage("&7You do not have any potions"));
									return true;
								}

								try {
									int quantity = Integer.parseInt(args[2]);
									if (quantity > 0) {
										AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
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

	private static String addPotionConsumptionMethodToName(ItemStack potion, String potionName) {
		String[] partsOfName = potionName.split(" ");
		StringBuilder finalName = new StringBuilder();
		
		if (potionName.startsWith("Long")) {
			if (potion.getType() == Material.POTION) {
				finalName = new StringBuilder("Extended Potion of ");
			} else if (potion.getType() == Material.SPLASH_POTION) {
				finalName = new StringBuilder("Extended Splash Potion of ");
			} else if (potion.getType() == Material.LINGERING_POTION) {
				finalName = new StringBuilder("Extended Lingering Potion of ");
			}
		} else {
			if (potion.getType() == Material.POTION) {
				finalName = new StringBuilder("Potion of ");
			} else if (potion.getType() == Material.SPLASH_POTION) {
				finalName = new StringBuilder("Splash Potion of ");
			} else if (potion.getType() == Material.LINGERING_POTION) {
				finalName = new StringBuilder("Lingering Potion of ");
			}
		}
		
		// Handles formatting the actual potion name
		for (int i = 0; i < partsOfName.length; i++) {
			if (partsOfName[i].equals("Long") || partsOfName[i].equals("Strong") || partsOfName[i].equals("of")) {
				continue;
			} else {
				if (i == partsOfName.length - 1) {
					finalName.append(partsOfName[i]);
				} else {
					finalName.append(partsOfName[i]).append(" ");
				}
			}
		}
		
		if (potionName.startsWith("Strong")) {
			finalName.append(" II");
		}
		
		return finalName.toString();
	}

}
