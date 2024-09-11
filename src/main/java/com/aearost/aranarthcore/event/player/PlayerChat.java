package com.aearost.aranarthcore.event.player;

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

public class PlayerChat implements Listener {
	
	public PlayerChat(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

    /**
     * Handles formatting chat messages.
     * Based on <a href="https://www.spigotmc.org/threads/editing-message-to-player-from-asyncplayerchatevent.362198/">Spigot URL</a>
     * @param event The event.
     */
	@EventHandler
    public void chatEvent(final AsyncPlayerChatEvent event) {
        String message = event.getMessage();

        for (Player p : event.getRecipients()) {
            if (message.contains(p.getDisplayName()) && !event.getPlayer().getDisplayName().equals(p.getDisplayName())) {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
            }
        }
        
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(event.getPlayer().getUniqueId());
        String prefix = aranarthPlayer.getPrefix();
        String nickname = aranarthPlayer.getNickname();
        
        String chatMessage = "⊰";
        if (!prefix.isEmpty()) {
        	chatMessage += ChatUtils.translateToColor(prefix + "&r") + " ";
        }
        if (!nickname.isEmpty()) {
        	chatMessage += ChatUtils.translateToColor(nickname + "&r");
        } else {
        	chatMessage += event.getPlayer().getName();
        }
        chatMessage += "⊱ " + ChatUtils.translateToColor(event.getMessage() + "&r");
        chatMessage = chatMessage.replaceAll("%", "%%");
        
        event.setFormat(chatMessage);
    }
	
}
