package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Places the player's held item on their head.
 */
public class CommandHat implements CommandExecutor {

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
			if (!player.hasPermission("aranarth.hat")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to run this command!"));
				return true;
			}

			if (player.getInventory().getItemInMainHand() != null) {
				if (player.getInventory().getHelmet() == null) {
					player.getInventory().setHelmet(player.getInventory().getItemInMainHand());
					player.getInventory().setItemInMainHand(null);
					player.updateInventory();
					return true;
				} else {
					// Switch the items
					ItemStack helmetCopy = player.getInventory().getHelmet().clone();
					player.getInventory().setHelmet(player.getInventory().getItemInMainHand());
					player.getInventory().setItemInMainHand(helmetCopy);
					player.updateInventory();
					return true;
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou must be holding something!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed in-game!"));
			return true;
		}
	}

}
