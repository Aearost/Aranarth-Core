package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the auto complete functionality while using the /sit command.
 */
public class CommandSitCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1 && player.hasPermission("aranarth.sit.color")
                && "color".startsWith(args[0].toLowerCase())) {
            return List.of("color");
        }
        return List.of();
    }
}
