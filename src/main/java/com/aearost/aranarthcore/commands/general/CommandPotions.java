package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiPotions;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Allows players to add to and view their potion inventory.
 */
public class CommandPotions implements CommandExecutor {

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
			if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("access.must_survival")));
				return true;
			}

            if (args.length == 0) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (aranarthPlayer.getPotions() == null || aranarthPlayer.getPotions().isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("potions.none_stored")));
					return true;
				}

				GuiPotions gui = new GuiPotions(player, 0);
				gui.openGui();
				return true;
			} else {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                switch (args[0]) {
                    case "list" -> {
                        if (aranarthPlayer.getPotions() != null) {
							HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();

                            if (potions.isEmpty()) {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("potions.no_stored")));
                                return true;
                            }

							HashMap<String, HashMap<ItemStack, Integer>> amountOfPotions = AranarthUtils.getPlayerPotionNames(player);

                            // Sorts all potion names alphabetically
                            SortedSet<String> sortedMap = new TreeSet<>(amountOfPotions.keySet());
							String potionStats = AranarthUtils.getPlayerStoredPotionNum(player) + "/" + AranarthUtils.getMaxPotionNum(player);

                            player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lYour Potions &e(" + potionStats + ") &8- - -"));
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
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("potions.no_stored")));
							return true;
                        }
                    }
                    case "add" -> {
						// Prevents adding potions when already at the limit
						if (aranarthPlayer.getPotions() != null && !aranarthPlayer.getPotions().isEmpty()) {
							if (AranarthUtils.getPlayerStoredPotionNum(player) >= AranarthUtils.getMaxPotionNum(player)) {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("potions.pouch_full")));
								return true;
							}
						}

                        GuiPotions gui = new GuiPotions(player, 1);
                        gui.openGui();
						return true;
                    }
                    case "remove" -> {
						if (args.length >= 2) {
							HashMap<ItemStack, Integer> potions = AranarthUtils.getPlayer(player.getUniqueId()).getPotions();
							if (potions == null || potions.isEmpty()) {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("potions.none_stored")));
								return true;
							}

							try {
								int quantity = Integer.parseInt(args[1]);
								if (quantity > 0) {
									aranarthPlayer.setPotionQuantityToRemove(quantity);
									AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
									GuiPotions gui = new GuiPotions(player, -1);
									gui.openGui();
								} else {
									player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
								}
							} catch (NumberFormatException e) {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "potions remove <qty>")));
							}
						} else {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("potions.invalid_syntax")));
						}
						return true;
                    }
                    default -> player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_toggle")));
                }
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
	}

}
