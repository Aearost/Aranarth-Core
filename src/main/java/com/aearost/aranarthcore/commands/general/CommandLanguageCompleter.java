package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.LangManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab completion for /language.
 */
public class CommandLanguageCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String displayName : LangManager.LOCALE_DISPLAY_NAMES.values()) {
                if (displayName.toLowerCase().startsWith(partial)) {
                    completions.add(displayName);
                }
            }
        }
        return completions;
    }
}
