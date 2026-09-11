package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatGameUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * AC subcommand for managing the unscramble word pool.
 */
public class CommandUnscramble {

    public static boolean onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.getCouncilRank() < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac unscramble <add|remove> <word>")));
            return true;
        }
        String subCommand = args[1].toLowerCase();
        String word = args[2].toLowerCase();
        switch (subCommand) {
            case "add" -> {
                if (ChatGameUtils.addWord(word)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("unscramble.word_added", "word", word)));
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("unscramble.word_exists", "word", word)));
                }
            }
            case "remove" -> {
                if (ChatGameUtils.removeWord(word)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("unscramble.word_removed", "word", word)));
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("unscramble.word_not_found", "word", word)));
                }
            }
            default -> player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac unscramble <add|remove> <word>")));
        }
        return true;
    }
}
