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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        // Prevents chat messages from going through if the receiving user has toggled their chat
        List<Player> toRemove = new ArrayList<>();
        Iterator<Player> recipientIterator = e.getRecipients().iterator();
        for (int i = 0; i < e.getRecipients().size(); i++) {
            Player recipient = recipientIterator.next();
            AranarthPlayer recipientAranarthPlayer = AranarthUtils.getPlayer(recipient.getUniqueId());
            if (recipientAranarthPlayer.isTogglingChat()) {
                // Only block non-council messages
                if (aranarthPlayer.getCouncilRank() == 0) {
                    toRemove.add(recipient);
                    continue;
                }
            }

            if (message.contains(recipient.getDisplayName()) && !player.getDisplayName().equals(recipient.getDisplayName())) {
                recipient.playSound(recipient, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
            }
        }
        e.getRecipients().removeAll(toRemove);

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
