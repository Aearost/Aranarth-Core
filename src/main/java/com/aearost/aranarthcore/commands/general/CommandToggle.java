package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiToggle;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.GateUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Centralizes the logic for toggling different functionality in Aranarth.
 */
public class CommandToggle implements CommandExecutor {

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
			if (args.length >= 1) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (args[0].equalsIgnoreCase("chat")) {
					if (player.hasPermission("aranarth.toggle.chat")) {
						if (aranarthPlayer.isTogglingChat()) {
							aranarthPlayer.setTogglingChat(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7chat messages"));
						} else {
							aranarthPlayer.setTogglingChat(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7chat messages"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to toggle chat!"));
						return true;
					}
				} else if (args[0].equalsIgnoreCase("messages")) {
					if (player.hasPermission("aranarth.toggle.msg")) {
						if (aranarthPlayer.isTogglingMessages()) {
							aranarthPlayer.setTogglingMessages(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7private messages"));
						} else {
							aranarthPlayer.setTogglingMessages(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7private messages"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to toggle messages!"));
						return true;
					}
				} else if (args[0].equalsIgnoreCase("teleport")) {
					if (player.hasPermission("aranarth.toggle.tp")) {
						if (aranarthPlayer.isTogglingTp()) {
							aranarthPlayer.setTogglingTp(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7teleport requests"));
						} else {
							aranarthPlayer.setTogglingTp(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7teleport requests"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to toggle teleport requests!"));
						return true;
					}
				} else if (args[0].equalsIgnoreCase("spawnboost")) {
					// Everyone has access
					if (aranarthPlayer.isUsingSpawnBoost()) {
						aranarthPlayer.setUsingSpawnBoost(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the spawn boost effects"));
						if (AranarthUtils.isSpawnLocation(player.getLocation())) {
							player.clearActivePotionEffects();
						}
					} else {
						aranarthPlayer.setUsingSpawnBoost(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the spawn boost effects"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("changeclaim")) {
					// Everyone has access
					if (aranarthPlayer.isTogglingChangeClaim()) {
						aranarthPlayer.setTogglingChangeClaim(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7Dominion claim change messages"));
					} else {
						aranarthPlayer.setTogglingChangeClaim(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7Dominion claim change messages"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("inventory")) {
					if (player.hasPermission("aranarth.inventory")) {
						if (aranarthPlayer.isTogglingInventoryAssist()) {
							aranarthPlayer.setTogglingInventoryAssist(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the inventory assist perk"));
						} else {
							aranarthPlayer.setTogglingInventoryAssist(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the inventory assist perk"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have the Inventory Assist perk!"));
					}
				} else if (args[0].equalsIgnoreCase("shulker")) {
					if (player.hasPermission("aranarth.shulker")) {
						if (aranarthPlayer.isAddingToShulker()) {
							aranarthPlayer.setAddingToShulker(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the shulker assist perk"));
						} else {
							aranarthPlayer.setAddingToShulker(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the shulker assist perk"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have the Shulker Assist perk!"));
					}
				} else if (args[0].equalsIgnoreCase("blacklist")) {
					if (player.hasPermission("aranarth.blacklist")) {
						if (args.length == 1) {
							player.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid toggle option!"));
						} else {
							if (args[1].equals("ignore")) {
								aranarthPlayer.setBlacklistingMethod(0);
								player.sendMessage(ChatUtils.chatMessage("&7You will now ignore blacklisted items"));
							} else if (args[1].equals("trash")) {
								aranarthPlayer.setBlacklistingMethod(1);
								player.sendMessage(ChatUtils.chatMessage("&7You will now trash blacklisted items"));
							} else if (args[1].equals("off")) {
								aranarthPlayer.setBlacklistingMethod(-1);
								player.sendMessage(ChatUtils.chatMessage("&7Your blacklist is now &cdisabled"));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid toggle option!"));
								return true;
							}
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have the Blacklist perk!"));
					}
				} else if (args[0].equalsIgnoreCase("compressor")) {
					if (player.hasPermission("aranarth.compressor")) {
						if (aranarthPlayer.isCompressingItems()) {
							aranarthPlayer.setCompressingItems(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the compressor"));
						} else {
							aranarthPlayer.setCompressingItems(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the compressor"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have the Compressor perk!"));
					}
				} else if (args[0].equalsIgnoreCase("chestlock")) {
					if (aranarthPlayer.isAutoLockingChests()) {
						aranarthPlayer.setAutoLockingChests(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7automatic chest locking"));
					} else {
						aranarthPlayer.setAutoLockingChests(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7automatic chest locking"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("bluefire")) {
					// Will need to remove blue fire entirely and re-enable based on perk
					if (aranarthPlayer.getPerks().get(Perk.BLUEFIRE) == 1) {
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
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have the Blue Fire perk"));
					}
				} else if (args[0].equalsIgnoreCase("pethurt")) {
					if (aranarthPlayer.isHurtingOwnPets()) {
						aranarthPlayer.setHurtingOwnPets(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the ability to hurt your own pets"));
					} else {
						aranarthPlayer.setHurtingOwnPets(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the ability to hurt your own pets"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("gradientchat")) {
					if (!aranarthPlayer.getPerks().containsKey(Perk.CHAT) && aranarthPlayer.getSaintRank() < 2) {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have the Colored Chat perk!"));
						return true;
					}
					if (args.length >= 2) {
						if (args[1].equalsIgnoreCase("get")) {
							if (aranarthPlayer.getGradientChatColors().isEmpty()) {
								player.sendMessage(ChatUtils.chatMessage("&cYou have not saved any gradient colors yet"));
								return true;
							}
							player.sendMessage(ChatUtils.chatMessage(ChatUtils.formatGradientColorsDisplay(aranarthPlayer.getGradientChatColors())));
							return true;
						} else if (args[1].equalsIgnoreCase("bold")) {
							if (aranarthPlayer.isGradientChatBold()) {
								aranarthPlayer.setGradientChatBold(false);
								player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7gradient chat bold"));
							} else {
								aranarthPlayer.setGradientChatBold(true);
								player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7gradient chat bold"));
							}
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							return true;
						}
						// Validate and save new color pattern, then enable
						String colors = args[1];
						String[] colorArray = colors.split(",");
						if (colorArray.length < 2) {
							player.sendMessage(ChatUtils.chatMessage("&cAt least 2 colors are required! Use: &e/toggle gradientchat #hex1,#hex2,..."));
							return true;
						}
						boolean validColors = true;
						for (String color : colorArray) {
							if (!color.startsWith("#") || color.length() != 7 || !color.substring(1).matches("[0-9A-Fa-f]+")) {
								validColors = false;
								break;
							}
						}
						if (!validColors) {
							player.sendMessage(ChatUtils.chatMessage("&cThe entered hex codes are not valid"));
							return true;
						}
						aranarthPlayer.setGradientChatColors(colors);
						aranarthPlayer.setGradientChatEnabled(true);
						player.sendMessage(ChatUtils.chatMessage(ChatUtils.translateToGradient(colors, "Your gradient chat colors have been updated", false)));
					} else {
						// Toggle on/off using the saved pattern
						if (aranarthPlayer.isGradientChatEnabled()) {
							aranarthPlayer.setGradientChatEnabled(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7gradient chat"));
						} else {
							if (aranarthPlayer.getGradientChatColors().isEmpty()) {
								player.sendMessage(ChatUtils.chatMessage("&cYou have not saved any gradient colors yet"));
								return true;
							}
							aranarthPlayer.setGradientChatEnabled(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7gradient chat"));
						}
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("gate")) {
					if (player.hasPermission("aranarth.gate")) {
						boolean enabled = GateUtils.toggleGatePlacementMode(player.getUniqueId());
						if (enabled) {
							player.sendMessage(ChatUtils.chatMessage("&7Gate creation mode &aenabled&7. Place a fence or bar block to start a new gate."));
						} else {
							player.sendMessage(ChatUtils.chatMessage("&7Gate creation mode &cdisabled&7."));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to create gates!"));
					}
				} else if (args[0].equalsIgnoreCase("daymessage")) {
					if (aranarthPlayer.isDayMessageDisabled()) {
						aranarthPlayer.setDayMessageDisabled(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the new day message"));
					} else {
						aranarthPlayer.setDayMessageDisabled(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the new day message"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("weathermessage")) {
					if (aranarthPlayer.isWeatherMessageDisabled()) {
						aranarthPlayer.setWeatherMessageDisabled(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7weather change messages"));
					} else {
						aranarthPlayer.setWeatherMessageDisabled(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7weather change messages"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("bulksellshulker")) {
					if (player.hasPermission("aranarth.shulker")) {
						if (aranarthPlayer.isBulkSellShulkerEnabled()) {
							aranarthPlayer.setBulkSellShulkerEnabled(false);
							player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7bulk sell shulker"));
						} else {
							aranarthPlayer.setBulkSellShulkerEnabled(true);
							player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7bulk sell shulker"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have the Shulker Assist perk!"));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/toggle <option>"));
				}
				return true;
			} else {
				new GuiToggle(player).openGui();
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			return true;
		}
	}

}
