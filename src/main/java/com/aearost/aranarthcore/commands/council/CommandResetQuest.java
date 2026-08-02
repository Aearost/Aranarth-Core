package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.QuestUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandResetQuest {

    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("aranarth.resetquests")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
                return true;
            }
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtils.chatMessage("&cUsage: &e/ac resetquest <player> <daily|weekly>"));
            return true;
        }

        String targetName = args[1];
        String type = args[2].toLowerCase();

        if (!type.equals("daily") && !type.equals("weekly")) {
            sender.sendMessage(ChatUtils.chatMessage("&cUsage: &e/ac resetquest <player> <daily|weekly>"));
            return true;
        }

        UUID uuid = AranarthUtils.getUUIDFromUsername(targetName);
        if (uuid == null) {
            sender.sendMessage(ChatUtils.chatMessage("&cPlayer &e" + targetName + " &ccould not be found"));
            return true;
        }

        Player target = Bukkit.getPlayer(uuid);
        if (type.equals("daily")) {
            QuestUtils.resetPlayerDailyQuests(uuid);
            sender.sendMessage(ChatUtils.chatMessage("&aReset daily quests for &e" + targetName));
            if (target != null) {
                target.sendMessage(ChatUtils.chatMessage("&7Your daily quests have been reset"));
            }
        } else {
            QuestUtils.resetPlayerWeeklyQuests(uuid);
            sender.sendMessage(ChatUtils.chatMessage("&aReset weekly quests for &e" + targetName));
            if (target != null) {
                target.sendMessage(ChatUtils.chatMessage("&7Your weekly quests have been reset"));
            }
        }

        return true;
    }

}
