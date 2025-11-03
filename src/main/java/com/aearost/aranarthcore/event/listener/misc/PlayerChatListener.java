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

import java.time.LocalDateTime;

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
        String message = e.getMessage();

        for (Player p : e.getRecipients()) {
            if (message.contains(p.getDisplayName()) && !e.getPlayer().getDisplayName().equals(p.getDisplayName())) {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
            }
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());

        if (isPlayerMuted(aranarthPlayer)) {
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot send any messages as you are muted!"));
            e.setCancelled(true);
            return;
        }

        String nickname = aranarthPlayer.getNickname();
        String prefix = "⊰";

        prefix += AranarthUtils.getSaintRank(aranarthPlayer);
        prefix += AranarthUtils.getArchitectRank(aranarthPlayer);
        prefix += AranarthUtils.getCouncilRank(aranarthPlayer);
        prefix += AranarthUtils.getRank(aranarthPlayer);

        if (!nickname.isEmpty()) {
        	prefix += nickname + "&r";
        } else {
        	prefix += e.getPlayer().getName();
        }

        prefix += "⊱ &r";
        prefix = ChatUtils.translateToColor(prefix);
        String chatMessage = e.getMessage();

        if (e.getPlayer().hasPermission("aranarth.chat.hex")) {
            chatMessage = ChatUtils.translateToColor(chatMessage);
        } else if (e.getPlayer().hasPermission("aranarth.chat.color")) {
            chatMessage = ChatUtils.playerColorChat(chatMessage);
        }
        e.setFormat(prefix + chatMessage);
    }



    /**
     * Confirms if the player is currently muted.
     * @param aranarthPlayer The player to be verified.
     * @return Confirmation if the player is currently muted.
     */
    private boolean isPlayerMuted(AranarthPlayer aranarthPlayer) {
        // YYMMDDhhmm
        String muteEndDate = aranarthPlayer.getMuteEndDate();

        if (muteEndDate.isEmpty()) {
            return false;
        }
        if (muteEndDate.equals("none")) {
            return true;
        }

        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime definedMuteDate = null;
        try {
            int year = Integer.parseInt(muteEndDate.substring(0, 2));
            int month = Integer.parseInt(trimZero(muteEndDate.substring(2, 4)));
            int day = Integer.parseInt(trimZero(muteEndDate.substring(4, 6)));
            int hour = Integer.parseInt(trimZero(muteEndDate.substring(6, 8)));
            int minute = Integer.parseInt(trimZero(muteEndDate.substring(8, 10)));
            definedMuteDate = LocalDateTime.of(year, month, day, hour, minute);
        } catch (NumberFormatException e) {
            Bukkit.getLogger().info("Something went wrong with parsing the player's mute date...");
            return false;
        }

        return definedMuteDate.isBefore(currentDate);
    }

    /**
     * Trims the leading zero if the value is only one digit.
     * @param value The value.
     * @return The trimmed value.
     */
    private String trimZero(String value) {
        if (value.startsWith("0")) {
            return value.substring(1);
        } else {
            return value;
        }
    }

}
