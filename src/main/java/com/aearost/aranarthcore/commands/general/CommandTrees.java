package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiTrees;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the GUI where a player chooses which custom trees their saplings may grow into.
 */
public class CommandTrees implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getDefault("general.no_permission_console")));
            return true;
        }
        new GuiTrees(player, 0).openGui();
        return true;
    }
}
