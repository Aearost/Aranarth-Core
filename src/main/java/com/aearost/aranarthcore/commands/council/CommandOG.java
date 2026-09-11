package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.getCouncilRank() != 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac og <add|remove> <player>")));
            return true;
        }

        String targetName = args[2];
        UUID targetUuid = AranarthUtils.getUUIDFromUsername(targetName);
        if (targetUuid == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", targetName)));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (AranarthUtils.isOriginalPlayer(targetUuid)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("og.already_og", "name", targetName)));
                } else {
                    AranarthUtils.addOriginalPlayer(targetUuid);
                    PersistenceUtils.saveOriginalPlayers();
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("og.added", "player", targetName)));
                }
            }
            case "remove" -> {
                if (!AranarthUtils.isOriginalPlayer(targetUuid)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("og.not_og", "name", targetName)));
                } else {
                    AranarthUtils.removeOriginalPlayer(targetUuid);
                    PersistenceUtils.saveOriginalPlayers();
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("og.removed", "player", targetName)));
                }
            }
            default -> player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac og <add|remove> <player>")));
        }

        return true;
    }
}
