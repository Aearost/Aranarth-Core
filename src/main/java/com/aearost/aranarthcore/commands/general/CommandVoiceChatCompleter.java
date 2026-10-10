package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /voicechat command.
 */
public class CommandVoiceChatCompleter implements TabCompleter {

    private static final List<String> OPTIONS = List.of("global", "local", "dominion", "leave", "who");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> results = new ArrayList<>();
        if (args.length == 1) {
            for (String option : OPTIONS) {
                if (option.startsWith(args[0].toLowerCase())) {
                    results.add(option);
                }
            }
        }
        return results;
    }
}
