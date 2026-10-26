package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Tab completer for the /aranarth guide command.
 */
public class CommandAranarthCompleter implements TabCompleter {

    private static final List<String> TOPICS = List.of(
            "calendar", "aranarthium", "essences", "rules",
            "dominions", "bending", "ranks", "economy", "perks", "mechanics", "creative", "recipes"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return TOPICS.stream()
                    .filter(t -> t.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            List<String> chapters = args[0].equalsIgnoreCase("calendar")
                    ? CommandAranarth.CALENDAR_MONTHS
                    : CommandAranarth.getSections(args[0].toLowerCase());
            return chapters.stream()
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("calendar") && args[1].equalsIgnoreCase("events")) {
            return CommandAranarth.CALENDAR_EVENTS.stream()
                    .filter(e -> e.startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
