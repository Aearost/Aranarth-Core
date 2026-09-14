package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiTrade;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Trade;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.TradeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Initiates or accepts a trade with another player.
 */
public class CommandTrade implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "trade <player>")));
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (TradeManager.isInTrade(uuid)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.already_in_trade")));
            return true;
        }

        UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
        if (targetUuid == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "player.not_found", "name", args[0])));
            return true;
        }

        if (targetUuid.equals(uuid)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.self")));
            return true;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        boolean targetOnThisServer = target != null;

        // Cross-server
        NetworkPlayer networkTarget = null;
        if (!targetOnThisServer && NetworkManager.isActive()) {
            networkTarget = NetworkManager.getInstance().getRemoteRoster().get(targetUuid);
        }

        if (!targetOnThisServer && networkTarget == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.player_not_online", "name", args[0])));
            return true;
        }

        AranarthPlayer senderAp = AranarthUtils.getPlayer(uuid);

        // Check if target is already in a trade
        if (TradeManager.isInTrade(targetUuid)) {
            AranarthPlayer targetAp = AranarthUtils.getPlayer(targetUuid);
            String targetNick = targetAp != null ? targetAp.getNickname() : args[0];
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.target_busy", "player", targetNick)));
            return true;
        }

        // Accept flow - check for a pending same-server invite from target
        if (TradeManager.hasInviteFrom(uuid, targetUuid)) {
            TradeManager.removeInvite(uuid);
            Trade trade = new Trade(targetUuid, uuid); // Original inviter is initiator
            TradeManager.registerTrade(trade);
            TradeManager.startAnimation(trade);

            if (targetOnThisServer) {
                new GuiTrade(target, trade).openGui();
            }
            new GuiTrade(player, trade).openGui();

            AranarthPlayer targetAp = AranarthUtils.getPlayer(targetUuid);
            String targetNick = targetAp != null ? targetAp.getNickname() : args[0];
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.accepted", "player", targetNick)));
            if (targetOnThisServer) {
                target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "trade.accepted_other",
                        "player", senderAp.getNickname())));
            }
            return true;
        }

        // Accept flow - check for a pending cross-server invite from target
        if (TradeManager.hasCrossServerInviteFrom(uuid, targetUuid)) {
            TradeManager.removeCrossServerInvite(uuid);

            // The original inviter (targetUuid) is the initiator
            Trade trade = new Trade(targetUuid, uuid);
            TradeManager.registerTrade(trade);
            TradeManager.markCrossServer(trade);
            TradeManager.startAnimation(trade);
            new GuiTrade(player, trade).openGui();

            AranarthPlayer targetAp = AranarthUtils.getPlayer(targetUuid);
            String targetNick = targetAp != null ? targetAp.getNickname() : args[0];
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.accepted", "player", targetNick)));

            // Notify the initiator's server to open their GUI
            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishTradeAccept(targetUuid, uuid);
            }
            return true;
        }

        // Invite flow
        AranarthPlayer targetAp = AranarthUtils.getPlayer(targetUuid);
        String targetNick = targetAp != null ? targetAp.getNickname() : args[0];

        if (targetOnThisServer) {
            // Same-server invite
            TradeManager.addInvite(targetUuid, uuid);
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.invite_sent", "player", targetNick)));

            Component line1 = LegacyComponentSerializer.legacySection().deserialize(
                    ChatUtils.chatMessage(Lang.getFor(target, "trade.invite_received", "player", senderAp.getNickname())));
            Component line2Pre = LegacyComponentSerializer.legacySection().deserialize(
                    ChatUtils.chatMessage(Lang.getFor(target, "trade.invite_line2_pre") + " "));
            Component line2Cmd = ChatUtils.clickableCommand(
                    LegacyComponentSerializer.legacySection().deserialize(
                            ChatUtils.translateToColor("&e/trade " + player.getName())),
                    ChatUtils.translateToColor(Lang.getFor(target, "trade.invite_hover")),
                    "/trade " + player.getName(),
                    true);
            Component line2Suf = LegacyComponentSerializer.legacySection().deserialize(
                    ChatUtils.translateToColor(Lang.getFor(target, "trade.invite_line2_suf")));
            Component hereLink = LegacyComponentSerializer.legacySection()
                    .deserialize(ChatUtils.translateToColor(Lang.getFor(target, "trade.invite_here")))
                    .clickEvent(ClickEvent.runCommand("/trade " + player.getName()));

            Component message = line1
                    .append(Component.newline())
                    .append(line2Pre)
                    .append(line2Cmd)
                    .append(line2Suf)
                    .append(Component.text(" "))
                    .append(hereLink);
            target.sendMessage(message);
            AranarthUtils.playPingSound(target);
        } else {
            // Cross-server invite
            TradeManager.addInvite(targetUuid, uuid); // Store locally in case target joins this server
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.invite_sent", "player", targetNick)));

            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishTradeInvite(uuid, senderAp.getNickname(), targetUuid);
            }
        }
        return true;
    }
}
