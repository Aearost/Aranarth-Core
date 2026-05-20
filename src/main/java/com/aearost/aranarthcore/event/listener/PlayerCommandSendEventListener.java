package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

/**
 * Filters the command list sent to each player so that only commands they have
 * permission to use appear in tab completion.
 */
public class PlayerCommandSendEventListener implements Listener {

    public PlayerCommandSendEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        event.getCommands().removeIf(label -> {
            Command command = Bukkit.getCommandMap().getCommand(label);
            if (command == null) {
                return false;
            }
            String permission = command.getPermission();
            if (permission == null || permission.isEmpty()) {
                return false;
            }
            return !player.hasPermission(permission);
        });
    }
}
