package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows players to change their pronouns.
 */
public class CommandPronouns implements CommandExecutor {

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
			if (args.length == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax! /ac pronouns <pronouns>"));
				return true;
			}

			if (args.length > 0) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (args[0].equalsIgnoreCase("MALE")) {
					aranarthPlayer.setPronouns(Pronouns.MALE);
				} else if (args[0].equalsIgnoreCase("FEMALE")) {
					aranarthPlayer.setPronouns(Pronouns.FEMALE);
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThose pronouns are not supported!"));
					return true;
				}

				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				player.sendMessage(ChatUtils.chatMessage("&7Your pronouns have been updated!"));
				return true;
			}
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}

}
