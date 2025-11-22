package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to toggle the shulker-filling functionality
 */
public class CommandShulker {

    /**
     * @param sender The user that entered the command.
     * @param args   The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("aranarth.shulker")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to run this command!"));
                return true;
            }

            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (aranarthPlayer.getIsAddingToShulker()) {
                aranarthPlayer.setIsAddingToShulker(false);
                player.sendMessage(ChatUtils.chatMessage("&7You are no longer adding items to shulkers"));
            } else {
                aranarthPlayer.setIsAddingToShulker(true);
                player.sendMessage(ChatUtils.chatMessage("&7You are now adding items to shulkers"));
            }
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            return true;
        } else {
            sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
            return true;
        }
    }

}
