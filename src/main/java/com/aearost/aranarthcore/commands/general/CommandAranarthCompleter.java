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
            "dominions", "bending", "ranks", "economy", "perks", "mechanics", "recipes"
    );

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
                case "essences" -> CommandAranarth.ESSENCE_SECTIONS;
                case "dominions" -> CommandAranarth.DOMINION_SECTIONS;
                case "bending" -> CommandAranarth.BENDING_SECTIONS;
                case "ranks" -> CommandAranarth.RANKS_SECTIONS;
                case "economy" -> CommandAranarth.ECONOMY_SECTIONS;
                case "perks" -> CommandAranarth.PERKS_SECTIONS;
                case "mechanics" -> CommandAranarth.MECHANICS_SECTIONS;
                default -> List.of();
            };
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
