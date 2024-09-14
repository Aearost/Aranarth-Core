package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to toggle the shulker-filling functionality
 */
public class CommandItemName {

    /**
     * @param sender The user that entered the command.
     * @param args   The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            player.sendMessage(ChatUtils.chatMessage("&dSLAY"));
        }
        return false;
    }

}
