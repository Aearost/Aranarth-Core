package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the number of deaths of the input player.
 */
public class CommandDeaths implements CommandExecutor {

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
			// List their own deaths
			if (args.length == 0) {
				int deathCount = AranarthUtils.getKillsOrDeathsInWorld(player.getUniqueId(), player.getWorld(), false);
				player.sendMessage(ChatUtils.chatMessage("&7You have &c" + deathCount + " deaths"));
				return true;
			} else {
				OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
				if (target != null) {
					int deathCount = AranarthUtils.getKillsOrDeathsInWorld(target.getUniqueId(), player.getWorld(), false);
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
					player.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has &c" + deathCount + " deaths"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + args[0] + " &ccould not be found"));
				}
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			return true;
		}
	}
}
