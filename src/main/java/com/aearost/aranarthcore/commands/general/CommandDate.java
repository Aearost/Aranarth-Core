package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Provides the current server date to the player.
 */
public class CommandDate implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		int day = AranarthUtils.getDay();
		String weekday = DateUtils.provideWeekdayName(AranarthUtils.getWeekday());
		String month = DateUtils.provideMonthName(AranarthUtils.getMonth());
		int year = AranarthUtils.getYear();

		String[] messages = DateUtils.determineServerDate(day, weekday, month, year);
		sender.sendMessage(messages[0]);
		sender.sendMessage("  " + messages[1]);
		sender.sendMessage(messages[2]);
		return true;
	}

}
