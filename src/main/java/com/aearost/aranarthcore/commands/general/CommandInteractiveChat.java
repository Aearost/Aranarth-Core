package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiChatSnapshot;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Opens the view-only GUI for a chat snapshot.
 */
public class CommandInteractiveChat implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("view")) {
            return true;
        }
        UUID snapshotId;
        try {
            snapshotId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtils.chatMessage("&cInvalid snapshot ID."));
            return true;
        }
        GuiChatSnapshot.open(player, snapshotId);
        return true;
    }
}
