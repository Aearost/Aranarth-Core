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
 * Sends a /tp request to a player on either this server or the survival server.
 */
public class CommandTeleport implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§cYou must enter a player to teleport to!");
            return true;
        }

        SMPNetworkManager net = SMPNetworkManager.getInstance();
        if (net == null) {
            player.sendMessage("§cNetwork is not available.");
            return true;
        }

        UUID targetUuid = resolveUuid(args[0], net);
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
            // Same-server: just teleport directly (SMP is a small trusted group)
            player.teleport(localTarget.getLocation());
            player.sendMessage("§7You have teleported to §e" + localTarget.getName());
        } else {
            // Cross-server: send request
            NetworkPlayer remote = net.getPlayer(targetUuid);
            String targetName = remote != null ? remote.getNickname() : args[0];
            player.sendMessage("§7You have requested to teleport to §e" + targetName);
            net.publishTpRequest(player.getUniqueId(), player.getName(), targetUuid, false);
        }
        return true;
    }

    static UUID resolveUuid(String input, SMPNetworkManager net) {
        // Check local players first
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(input)) return p.getUniqueId();
        }
        // Then network roster
        for (NetworkPlayer np : net.getRoster().values()) {
            if (np.getNickname().equalsIgnoreCase(input)) return np.getUuid();
        }
        return null;
    }
}
