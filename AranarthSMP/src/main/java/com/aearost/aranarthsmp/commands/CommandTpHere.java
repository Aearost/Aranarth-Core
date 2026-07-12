package com.aearost.aranarthsmp.commands;

import com.aearost.aranarthsmp.network.NetworkPlayer;
import com.aearost.aranarthsmp.network.SMPNetworkManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Requests another player to teleport to the sender's location.
 */
public class CommandTpHere implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§cYou must enter a player!");
            return true;
        }

        SMPNetworkManager net = SMPNetworkManager.getInstance();
        if (net == null) {
            player.sendMessage("§cNetwork is not available.");
            return true;
        }

        UUID targetUuid = CommandTeleport.resolveUuid(args[0], net);
        if (targetUuid == null) {
            player.sendMessage("§cThat player is not online!");
            return true;
        }
        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot teleport to yourself!");
            return true;
        }

        Player localTarget = Bukkit.getPlayer(targetUuid);
        if (localTarget != null) {
            localTarget.teleport(player.getLocation());
            player.sendMessage("§e" + localTarget.getName() + " §7has been teleported to you");
            localTarget.sendMessage("§7You have been teleported to §e" + player.getName());
        } else {
            NetworkPlayer remote = net.getPlayer(targetUuid);
            String targetName = remote != null ? remote.getNickname() : args[0];
            player.sendMessage("§7You have requested for §e" + targetName + " §7to teleport to you");
            net.publishTpRequest(player.getUniqueId(), player.getName(), targetUuid, true);
        }
        return true;
    }
}
