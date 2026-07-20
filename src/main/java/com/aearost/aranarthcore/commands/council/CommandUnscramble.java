package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatGameUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * AC subcommand for managing the unscramble word pool.
 */
public class CommandUnscramble {

    public static boolean onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be used by players!"));
            return true;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.getCouncilRank() < 3) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage("&cUsage: &e/ac unscramble <add|remove> <word>"));
            return true;
        }
        String subCommand = args[1].toLowerCase();
        String word = args[2].toLowerCase();
        switch (subCommand) {
            case "add" -> {
                if (ChatGameUtils.addWord(word)) {
                    player.sendMessage(ChatUtils.chatMessage("&7Added &e" + word + " &7to the unscramble word pool"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cThe word &e" + word + " &calready exists in the pool"));
                }
            }
            case "remove" -> {
                if (ChatGameUtils.removeWord(word)) {
                    player.sendMessage(ChatUtils.chatMessage("&7Removed &e" + word + " &7from the unscramble word pool"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cThe word &e" + word + " &ccould not be found in the pool"));
                }
            }
            default -> player.sendMessage(ChatUtils.chatMessage("&cUsage: &e/ac unscramble <add|remove> <word>"));
        }
        return true;
    }
}
