package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiQuests;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the Aranarth Quests GUI for the player.
 */
public class CommandQuests implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            new GuiQuests(player).openGui();
        } else {
            sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
        }
        return true;
    }
}
