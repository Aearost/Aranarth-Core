package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiHeadExchange;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
            sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be executed in-game"));
            return true;
        }

        // Using /heads <username>
        if (args.length >= 1) {
            if (!player.hasPermission("aranarth.skull")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command"));
                return true;
            }
            UUID skullUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
            if (skullUuid == null) {
                player.sendMessage(ChatUtils.chatMessage("&e" + args[0] + " &ccould not be found"));
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
            player.sendMessage(ChatUtils.chatMessage("&7You have given yourself &e" + skullPlayer.getName() + "'s &7skull"));
            player.getInventory().addItem(skull);
            return true;
        }

        // Using /heads
        if (!player.hasPermission("aranarth.customheads")) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
            return true;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.getSaintRank() < 1 || aranarthPlayer.getCouncilRank() < 1) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
            return true;
        }

        if (!HeadsDatabaseManager.isLoaded()) {
            player.sendMessage(ChatUtils.chatMessage("&eHead database is still loading, please try again in a moment"));
            return true;
        }

        if (HeadsDatabaseManager.getExchangeableHeads().isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage("&cNo heads are available - check that the API categories are configured and the server has internet access"));
            return true;
        }

        new GuiHeadExchange(player).openGui();
        return true;
    }
}
