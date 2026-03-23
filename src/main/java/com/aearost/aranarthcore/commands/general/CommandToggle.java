package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
				} else if (args[0].equalsIgnoreCase("messages")) {
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
				} else if (args[0].equalsIgnoreCase("teleport")) {
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
				} else if (args[0].equalsIgnoreCase("spawnboost")) {
					// Everyone has access
					if (aranarthPlayer.isUsingSpawnBoost()) {
						aranarthPlayer.setUsingSpawnBoost(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have disabled the spawn boost effects"));
						if (AranarthUtils.isSpawnLocation(player.getLocation())) {
							player.clearActivePotionEffects();
						}
					} else {
						aranarthPlayer.setUsingSpawnBoost(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have enabled the spawn boost effects"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("changeclaim")) {
					// Everyone has access
					if (aranarthPlayer.isTogglingChangeClaim()) {
						aranarthPlayer.setTogglingChangeClaim(false);
						player.sendMessage(ChatUtils.chatMessage("&7You have re-enabled Dominion claim change messages"));
					} else {
						aranarthPlayer.setTogglingChangeClaim(true);
						player.sendMessage(ChatUtils.chatMessage("&7You have disabled Dominion claim change messages"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("inventory")) {
					if (aranarthPlayer.isTogglingInventoryAssist()) {
						aranarthPlayer.setTogglingInventoryAssist(false);
						player.sendMessage(ChatUtils.chatMessage("&7Your inventory assist perk is now &aenabled"));
					} else {
						aranarthPlayer.setTogglingInventoryAssist(true);
						player.sendMessage(ChatUtils.chatMessage("&7Your inventory assist perk is now &cdisabled"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("shulker")) {
					if (player.hasPermission("aranarth.shulker")) {
						if (aranarthPlayer.isAddingToShulker()) {
							aranarthPlayer.setAddingToShulker(false);
							player.sendMessage(ChatUtils.chatMessage("&7Your shulker assist perk is now &cdisabled"));
						} else {
							aranarthPlayer.setAddingToShulker(true);
							player.sendMessage(ChatUtils.chatMessage("&7Your shulker assist perk is now &aenabled"));
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to run this command!"));
					}
				} else if (args[0].equalsIgnoreCase("blacklist")) {
					if (player.hasPermission("aranarth.blacklist")) {
						if (args[1].equals("ignore")) {
							aranarthPlayer.setBlacklistingMethod(0);
							player.sendMessage(ChatUtils.chatMessage("&7You will now ignore blacklisted items"));
						} else if (args[1].equals("trash")) {
							aranarthPlayer.setBlacklistingMethod(1);
							player.sendMessage(ChatUtils.chatMessage("&7You will now trash blacklisted items"));
						} else if (args[1].equals("off")) {
							aranarthPlayer.setBlacklistingMethod(-1);
							player.sendMessage(ChatUtils.chatMessage("&7Your blacklist is now disabled"));
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid toggle option!"));
							return true;
						}
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
					}
				} else if (args[0].equalsIgnoreCase("compressor")) {
					if (aranarthPlayer.isCompressingItems()) {
						aranarthPlayer.setCompressingItems(false);
						player.sendMessage(ChatUtils.chatMessage("&7You are no longer compressing items"));
					} else {
						aranarthPlayer.setCompressingItems(true);
						player.sendMessage(ChatUtils.chatMessage("&7You are now compressing items"));
					}
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else if (args[0].equalsIgnoreCase("chestlock")) {
					if (aranarthPlayer.isAutoLockingChests()) {
						aranarthPlayer.setAutoLockingChests(false);
						player.sendMessage(ChatUtils.chatMessage("&7Automatic chest locking is now &cdisabled"));
					} else {
						aranarthPlayer.setAutoLockingChests(true);
						player.sendMessage(ChatUtils.chatMessage("&7Automatic chest locking is now &aenabled"));
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
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have access to the blue fire perk"));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/toggle <option>"));
				}
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/toggle <option>"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			return true;
		}
	}

}
