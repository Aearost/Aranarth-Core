package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.command.CommandSender;

/**
 * Provides the current server date to the player.
 */
public class CommandDate {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args[0].equals("date")) {
			int day = AranarthUtils.getDay();
			String weekday = DateUtils.provideWeekdayName(AranarthUtils.getWeekday());
			String month = DateUtils.provideMonthName(AranarthUtils.getMonth());
			int year = AranarthUtils.getYear();

			String[] messages = DateUtils.determineServerDate(day, weekday, month, year);
			sender.sendMessage(messages[0]);
			sender.sendMessage("  " + messages[1]);
			sender.sendMessage(messages[2]);
			return true;
		} else if (args[0].equals("dateset")) {
			if (args.length <= 2) {
				sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: /ac dateset <field> <value>"));
				return true;
			}
            switch (args[1]) {
                case "month" -> {
					for (Month enumMonth : Month.values()) {
						if (enumMonth.name().equals(args[2])) {
							AranarthUtils.setMonth(Month.valueOf(args[2]));
							sender.sendMessage(ChatUtils.chatMessage("&7You have updated the month to &e&l" + AranarthUtils.getMonth().name()));
						}
					}
                }
                case "day" -> {
                    AranarthUtils.setDay(Integer.parseInt(args[2]));
                    sender.sendMessage(ChatUtils.chatMessage("&7You have updated the day to &e&l" + AranarthUtils.getDay()));
                }
                case "weekday" -> {
                    AranarthUtils.setWeekday(Integer.parseInt(args[2]));
                    sender.sendMessage(ChatUtils.chatMessage("&7You have updated the weekday to &e&l" + DateUtils.provideWeekdayName(AranarthUtils.getWeekday())));
                }
                case "year" -> {
                    AranarthUtils.setYear(Integer.parseInt(args[2]));
                    sender.sendMessage(ChatUtils.chatMessage("&7You have updated the year to &e&l" + AranarthUtils.getYear()));
                }
                default -> sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: /ac dateset <field> <value>"));
            }
		}
		return false;
	}

}
