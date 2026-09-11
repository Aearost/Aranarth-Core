package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiHeadExchange;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.HeadsDatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class CommandHeads implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        // Using /heads <username>
        if (args.length >= 1) {
            if (!player.hasPermission("aranarth.skull")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                return true;
            }
            UUID skullUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
            if (skullUuid == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[0])));
                return true;
            }
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(skullUuid);
            OfflinePlayer skullPlayer = Bukkit.getOfflinePlayer(skullUuid);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(skullPlayer);
            String displayName = aranarthPlayer != null ? aranarthPlayer.getNickname() : skullPlayer.getName();
            meta.setDisplayName(ChatUtils.translateToColor("&e" + displayName + "&e's Skull"));
            skull.setItemMeta(meta);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("heads.skull_given", "player", skullPlayer.getName())));
            player.getInventory().addItem(skull);
            return true;
        }

        // Using /heads
        if (!player.hasPermission("aranarth.customheads")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return true;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.getSaintRank() < 1 && aranarthPlayer.getCouncilRank() < 1) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return true;
        }

        if (!HeadsDatabaseManager.isLoaded()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("heads.loading")));
            return true;
        }

        if (HeadsDatabaseManager.getExchangeableHeads().isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("heads.unavailable")));
            return true;
        }

        new GuiHeadExchange(player).openGui();
        return true;
    }
}
