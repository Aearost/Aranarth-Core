package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * AC subcommand for managing Quest NPC villagers.
 * Usage: /ac questnpc <spawn|remove>
 * Requires councilRank == 3.
 */
public class CommandQuestNpc {

    public static boolean onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
            return true;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.getCouncilRank() != 3) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
            return true;
        }

        // args[0] = "questnpc", args[1] = subcommand
        if (args.length < 2) {
            player.sendMessage(ChatUtils.chatMessage("&cUsage: /ac questnpc <spawn|remove>"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "spawn" -> {
                QuestUtils.spawnQuestNpc(player.getLocation());
                player.sendMessage(ChatUtils.chatMessage("&aQuest NPC spawned at your location."));
            }
            case "remove" -> {
                boolean removed = QuestUtils.removeNearestQuestNpc(player.getLocation());
                if (removed) {
                    player.sendMessage(ChatUtils.chatMessage("&aNearest Quest NPC has been removed."));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cNo Quest NPC found within 5 blocks."));
                }
            }
            default -> player.sendMessage(ChatUtils.chatMessage("&cUsage: /ac questnpc <spawn|remove>"));
        }

        return true;
    }
}
