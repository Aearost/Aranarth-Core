package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiQuests;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
        }
        return true;
    }
}
