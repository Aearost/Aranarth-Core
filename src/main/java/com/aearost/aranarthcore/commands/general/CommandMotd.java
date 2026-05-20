package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.event.listener.misc.PlayerServerJoinListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Displays the server MOTD to the player on demand.
 */
public class CommandMotd implements CommandExecutor {

    /**
     * @param sender The user that entered the command.
     * @param command The command itself.
     * @param alias The alias of the command.
     * @param args The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            PlayerServerJoinListener.displayMotd(player);
        }
        return false;
    }

}
