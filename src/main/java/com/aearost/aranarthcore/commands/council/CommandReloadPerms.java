package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Re-evaluates permissions for all online players (or a specific player) on both servers.
 */
public class CommandReloadPerms {

    /**
     * @param sender The user that entered the command.
     * @param args   The arguments of the command.
     */
    public static boolean onCommand(CommandSender sender, String[] args) {
        // /ac reloadperms [player]
        if (args.length >= 2) {
            String targetName = args[1];
            UUID targetUuid = AranarthUtils.getUUIDFromUsername(targetName);
            if (targetUuid == null) {
                sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", targetName)));
                return true;
            }

            Player localPlayer = Bukkit.getPlayer(targetUuid);
            if (localPlayer != null) {
                PermissionUtils.evaluatePlayerPermissions(localPlayer);
                sender.sendMessage(ChatUtils.chatMessage(Lang.get("reloadperms.reloaded_single", "player", localPlayer.getName())));
            } else {
                sender.sendMessage(ChatUtils.chatMessage(Lang.get("reloadperms.offline", "player", targetName)));
            }

            // Notify the other server to reload this player's permissions if they are online there
            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishPermReload(targetUuid);
            }
            return true;
        }

        // Re-evaluate all currently online players on this server
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PermissionUtils.evaluatePlayerPermissions(player);
            count++;
        }

        sender.sendMessage(ChatUtils.chatMessage(Lang.get("reloadperms.reloaded_all", "count", String.valueOf(count))));
        sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.offline_perms")));

        // Notify the other server to reload all its online players' permissions too
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishPermReload(null);
        }
        return true;
    }
}
