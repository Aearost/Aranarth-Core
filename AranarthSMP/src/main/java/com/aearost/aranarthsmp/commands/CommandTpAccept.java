package com.aearost.aranarthsmp.commands;

import com.aearost.aranarthsmp.network.CrossServerTpContext;
import com.aearost.aranarthsmp.network.SMPNetworkManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Accepts a pending (cross-server) teleport request.
 */
public class CommandTpAccept implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        SMPNetworkManager net = SMPNetworkManager.getInstance();
        if (net == null) {
            player.sendMessage("§cNetwork is not available.");
            return true;
        }

        CrossServerTpContext ctx = net.getCrossServerTpContext(player.getUniqueId());
        if (ctx == null) {
            player.sendMessage("§cYou do not have any pending teleport requests!");
            return true;
        }

        net.clearCrossServerTpContext(player.getUniqueId());
        player.sendMessage("§7You have accepted §e" + ctx.remotePlayerNickname() + "§7's teleport request");
        net.publishTpAccepted(player.getUniqueId(), player.getName(), ctx.remotePlayerUuid(), ctx.isTpHere());
        return true;
    }
}
