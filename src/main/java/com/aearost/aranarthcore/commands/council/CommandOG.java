package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * AC subcommand for managing the OG player list.
 */
public class CommandOG {

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

        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac og <add|remove> <player>"));
            return true;
        }

        String targetName = args[2];
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(targetName);
        if (targetUuid == null) {
            player.sendMessage(ChatUtils.chatMessage("&e" + targetName + " &ccould not be found!"));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (AranarthUtils.isOriginalPlayer(targetUuid)) {
                    player.sendMessage(ChatUtils.chatMessage("&e" + targetName + " &cis already an OG player!"));
                } else {
                    AranarthUtils.addOriginalPlayer(targetUuid);
                    PersistenceUtils.saveOriginalPlayers();
                    player.sendMessage(ChatUtils.chatMessage("&e" + targetName + " &7has been added to the list of OG players"));
                }
            }
            case "remove" -> {
                if (!AranarthUtils.isOriginalPlayer(targetUuid)) {
                    player.sendMessage(ChatUtils.chatMessage("&e" + targetName + " &cis not an OG player!"));
                } else {
                    AranarthUtils.removeOriginalPlayer(targetUuid);
                    PersistenceUtils.saveOriginalPlayers();
                    player.sendMessage(ChatUtils.chatMessage("&e" + targetName + " &ahas been removed from the list of OG players"));
                }
            }
            default -> player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac og <add|remove> <player>"));
        }

        return true;
    }
}
