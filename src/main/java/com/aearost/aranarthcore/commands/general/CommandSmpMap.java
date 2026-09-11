package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sends OG players a clickable link to the Aranarth SMP live map.
 */
public class CommandSmpMap implements CommandExecutor {

    /**
     * @param sender The user that entered the command.
     * @param command The command itself.
     * @param alias The alias of the command.
     * @param args The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        if (!AranarthUtils.isOriginalPlayer(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return true;
        }

        player.sendMessage(ChatUtils.buildMessageWithUrls(ChatUtils.chatMessage(Lang.get("smpmap.link"))));
        return true;
    }

}
