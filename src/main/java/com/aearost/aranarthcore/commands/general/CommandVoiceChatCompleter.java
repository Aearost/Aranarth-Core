package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.voice.VoiceChatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /voicechat command.
 */
public class CommandVoiceChatCompleter implements TabCompleter {

    private static final List<String> OPTIONS = List.of("global", "local", "dominion", "council", "leave", "who");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> results = new ArrayList<>();
        if (args.length == 1) {
            for (String option : OPTIONS) {
                // Council is only suggested to council members and architects
                if (option.equals("council") && (!(sender instanceof Player player) || !VoiceChatManager.canUseCouncil(player))) {
                    continue;
                }
                if (option.startsWith(args[0].toLowerCase())) {
                    results.add(option);
                }
            }
        }
        return results;
    }
}
