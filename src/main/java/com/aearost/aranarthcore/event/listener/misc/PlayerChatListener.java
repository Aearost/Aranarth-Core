package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Handles formatting chat messages.
 * Based on <a href="https://www.spigotmc.org/threads/editing-message-to-player-from-asyncplayerchatevent.362198/">Spigot URL</a>
 */
public class PlayerChatListener implements Listener {

	public PlayerChatListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
    public void chatEvent(final AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        String message = e.getMessage();

        for (Player p : e.getRecipients()) {
            if (message.contains(p.getDisplayName()) && !player.getDisplayName().equals(p.getDisplayName())) {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
            }
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        if (ChatUtils.isPlayerMuted(player)) {
            player.sendMessage(ChatUtils.chatMessage("&cYou cannot send any messages as you are muted!"));
            e.setCancelled(true);
            return;
        }

        String prefix = ChatUtils.formatChatPrefix(player);
        String chatMessage = ChatUtils.formatChatMessage(player, message);
        String msg = prefix + chatMessage;
        msg = msg.replaceAll("%", "%%"); // Throws exception with only one
        e.setFormat(msg);
    }
}
