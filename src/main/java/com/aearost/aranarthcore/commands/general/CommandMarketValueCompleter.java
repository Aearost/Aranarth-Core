package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

/**
 * Tab completer for /marketvalue - shows "item" as a hint when the argument is empty,
 * and returns nothing once the player starts typing (to avoid cluttering with player names).
 */
public class CommandMarketValueCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && args[0].isEmpty()) {
            return Collections.singletonList("item");
        }
        return Collections.emptyList();
    }
}
