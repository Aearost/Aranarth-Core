package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiHeadExchange;
import com.aearost.aranarthcore.gui.GuiHeads;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.HeadsDatabaseManager;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CommandHeads implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
            return true;
        }

        // Using /heads adminbuy
        if (args.length >= 1 && args[0].equalsIgnoreCase("adminbuy")) {
            if (!player.hasPermission("aranarth.heads.admin")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
                return true;
            }
            if (!HeadsDatabaseManager.isLoaded()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "heads.loading")));
                return true;
            }
            if (HeadsDatabaseManager.getExchangeableHeads().isEmpty()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "heads.unavailable")));
                return true;
            }
            new GuiHeads(player, 0, true).openGui();
            return true;
        }

        // Using /heads <username>
        if (args.length >= 1) {
            if (!player.hasPermission("aranarth.heads.player")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
                return true;
            }
            UUID skullUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
            if (skullUuid == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "player.not_found", "name", args[0])));
                return true;
            }
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(skullUuid);
            OfflinePlayer skullPlayer = Bukkit.getOfflinePlayer(skullUuid);
            String displayName = aranarthPlayer != null ? aranarthPlayer.getNickname() : skullPlayer.getName();
            ItemStack skull = MobHeadUtils.createPlayerHead(skullPlayer, "&e" + displayName + "&e's Skull");
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "heads.skull_given", "player", skullPlayer.getName())));
            player.getInventory().addItem(skull);
            return true;
        }

        // Using /heads
        if (!HeadsDatabaseManager.isLoaded()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "heads.loading")));
            return true;
        }

        if (HeadsDatabaseManager.getExchangeableHeads().isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "heads.unavailable")));
            return true;
        }

        new GuiHeadExchange(player).openGui();
        return true;
    }
}
