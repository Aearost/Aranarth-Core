package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandResetQuest {

    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("aranarth.resetquests")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                return true;
            }
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac resetquest <player> <daily|weekly>")));
            return true;
        }

        String targetName = args[1];
        String type = args[2].toLowerCase();

        if (!type.equals("daily") && !type.equals("weekly")) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac resetquest <player> <daily|weekly>")));
            return true;
        }

        UUID uuid = AranarthUtils.getUUIDFromUsername(targetName);
        if (uuid == null) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", targetName)));
            return true;
        }

        Player target = Bukkit.getPlayer(uuid);
        if (type.equals("daily")) {
            QuestUtils.resetPlayerDailyQuests(uuid);
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("quest.reset_success", "player", targetName)));
            if (target != null) {
                target.sendMessage(ChatUtils.chatMessage(Lang.get("quest.daily_reset_personal")));
            }
        } else {
            QuestUtils.resetPlayerWeeklyQuests(uuid);
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("quest.reset_success", "player", targetName)));
            if (target != null) {
                target.sendMessage(ChatUtils.chatMessage(Lang.get("quest.weekly_reset_personal")));
            }
        }

        return true;
    }

}
