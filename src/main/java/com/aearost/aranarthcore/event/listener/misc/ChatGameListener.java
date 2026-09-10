package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatGameUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listens for chat messages and checks them against the active word-scramble game.
 */
public class ChatGameListener implements Listener {

    public ChatGameListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!ChatGameUtils.isGameActive()) {
            return;
        }
        Player player = e.getPlayer();
        if (ChatUtils.isPlayerMuted(player)) {
            return;
        }
        if (ChatGameUtils.tryAnswer(player, e.getMessage())) {
            e.setCancelled(true);
            // Un-AFK the player since a correct answer counts as real activity.
            // This must be done here because PlayerChatListener returns early on cancelled events,
            // skipping its own AFK removal logic.
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            if (ap.getAfkLocation() != null) {
                if (ap.getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount()) {
                    AranarthUtils.toggleAfkStatus(player.getUniqueId(), false);
                } else {
                    ap.setAfkLocation(null);
                    AranarthUtils.setPlayer(player.getUniqueId(), ap);
                }
            }
        }
    }
}
