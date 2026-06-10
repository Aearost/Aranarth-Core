package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandMountCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("info", "skills", "nickname");
    private static final List<String> ELEMENTS = List.of("fire", "water", "earth", "air");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            if (sub.equals("skills")) {
                return ELEMENTS.stream().filter(e -> e.startsWith(prefix)).toList();
            }
            if (sub.equals("nickname")) {
                return "remove".startsWith(prefix) ? List.of("remove") : List.of();
            }
        }

        return List.of();
    }
}
