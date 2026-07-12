package com.aearost.aranarthsmp.commands;

import com.aearost.aranarthsmp.AranarthSMP;
import com.aearost.aranarthsmp.network.SMPNetworkManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Transfers the player back to the Survival server.
 */
public class CommandSurvival implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cYou must be a player to use this command!");
            return true;
        }

        SMPNetworkManager net = SMPNetworkManager.getInstance();
        if (net == null) {
            player.sendMessage("§cNetwork is not available.");
            return true;
        }

        String survivalServer = AranarthSMP.getInstance().getConfig()
                .getString("network.servers.survival", "survival");
        player.sendMessage("§7Transferring you to §eSurvival§7...");
        net.transferPlayer(player, survivalServer);
        return true;
    }
}
