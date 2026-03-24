package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Handles preventing or adding custom logic to commands being executed by players.
 */
public class CommandOverrides {

    public void execute(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        String[] parts = e.getMessage().split(" ");

        if (!parts[0].equals("/afk")) {
            if (aranarthPlayer.getAfkLocation() != null && aranarthPlayer.getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount()) {
                Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        AranarthUtils.toggleAfkStatus(player.getUniqueId(), false);
                    }
                }, 1);
            }
        }

        if (aranarthPlayer.getCouncilRank() != 3) {
            if (parts[0].equals("/w")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
                e.setCancelled(true);
                return;
            }
        }

        boolean isSurvivalWorld = player.getWorld().getName().startsWith("world") || player.getWorld().getName().startsWith("smp")
                || player.getWorld().getName().startsWith("resource");
        // Prevent the command entirely
        if (parts[0].equals("/time") && isSurvivalWorld) {
            if (aranarthPlayer.getCouncilRank() < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cUse &e/ac time <time> &cinstead!"));
            }
            e.setCancelled(true);
            return;
        }

        // Adding and removing the sub-elements upon changing element without relogging
        if (parts[0].startsWith("/b")) {
            if (parts[0].equalsIgnoreCase("/b") || parts[0].toLowerCase().startsWith("/bend")) {
                if (parts.length > 1) {
                    if (parts[1].equalsIgnoreCase("ch") || parts[1].equalsIgnoreCase("choose")) {
                        // Player executing this in the arena world prevents sub-elements from being removed when changing world
                        if (!player.getWorld().getName().equalsIgnoreCase("arena")) {
                            PermissionUtils.evaluatePlayerPermissions(player);
                        }
                    }
                }
            }
        }
    }


}
