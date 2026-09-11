package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.CommandSender;

/**
 * Teleports the player back to their last known location.
 */
public class CommandDateSet {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length <= 2) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac dateset <field> <value>")));
			return true;
		}
		switch (args[1]) {
			case "month" -> {
				for (Month enumMonth : Month.values()) {
					if (enumMonth.name().equals(args[2])) {
						AranarthUtils.setMonth(Month.valueOf(args[2]));
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("dateset.success", "field", "month", "value", AranarthUtils.getMonth().name())));
					}
				}
			}
			case "day" -> {
				AranarthUtils.setDay(Integer.parseInt(args[2]));
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("dateset.success", "field", "day", "value", String.valueOf(AranarthUtils.getDay()))));
			}
			case "weekday" -> {
				AranarthUtils.setWeekday(Integer.parseInt(args[2]));
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("dateset.success", "field", "weekday", "value", DateUtils.provideWeekdayName(AranarthUtils.getWeekday()))));
			}
			case "year" -> {
				AranarthUtils.setYear(Integer.parseInt(args[2]));
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("dateset.success", "field", "year", "value", String.valueOf(AranarthUtils.getYear()))));
			}
			default -> sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac dateset <field> <value>")));
		}
		return true;
	}

}
