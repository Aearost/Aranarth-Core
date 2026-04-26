package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiLoginStreak;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the Login Streak GUI for the player.
 */
public class CommandStreak implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            new GuiLoginStreak(player).openGui();
        }
        return true;
    }
}
