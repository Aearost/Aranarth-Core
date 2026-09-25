package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Tab completer for the /aranarth guide command.
 */
public class CommandAranarthCompleter implements TabCompleter {

    private static final List<String> TOPICS = List.of("calendar", "aranarthium", "incantations", "rules");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return TOPICS.stream()
                    .filter(t -> t.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            List<String> chapters = switch (args[0].toLowerCase()) {
                case "calendar" -> CommandAranarth.CALENDAR_MONTHS;
                case "aranarthium" -> CommandAranarth.ARANARTHIUM_SECTIONS;
                case "incantations" -> CommandAranarth.INCANTATION_SECTIONS;
                default -> List.of();
            };
            return chapters.stream()
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
