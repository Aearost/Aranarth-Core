package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class PlayerChat implements Listener {
	
	public PlayerChat(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}


	// Via https://www.spigotmc.org/threads/editing-message-to-player-from-asyncplayerchatevent.362198/
	@EventHandler
    public void chatEvent(AsyncPlayerChatEvent event) {
        String message = event.getMessage();

        for (Player p : event.getRecipients()) {
            if (message.contains(p.getDisplayName()) && event.getPlayer().getDisplayName() != p.getDisplayName()) {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
            }
        }
        
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(event.getPlayer().getUniqueId());
        String prefix = aranarthPlayer.getPrefix();
        String nickname = aranarthPlayer.getNickname();
        
        String chatMessage = "⊰";
        if (!prefix.equals("")) {
        	chatMessage += ChatUtils.translateToColor(prefix + "&r") + " ";
        }
        if (!nickname.equals("")) {
        	chatMessage += ChatUtils.translateToColor(nickname + "&r");
        } else {
        	chatMessage += event.getPlayer().getName();
        }
        chatMessage += "⊱ " + ChatUtils.translateToColor(event.getMessage() + "&r");
        
        event.setFormat(chatMessage);
    }
	
}
