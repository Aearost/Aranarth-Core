package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiBrewBook;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the player's Brew Book, showing all unlocked BreweryX recipes.
 */
public class CommandBrewBook implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            new GuiBrewBook(player, 0).openGui();
        } else {
            sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
        }
        return true;
    }
}
