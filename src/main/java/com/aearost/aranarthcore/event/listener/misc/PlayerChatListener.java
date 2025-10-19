package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Pronouns;
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
        String message = e.getMessage();

        for (Player p : e.getRecipients()) {
            if (message.contains(p.getDisplayName()) && !e.getPlayer().getDisplayName().equals(p.getDisplayName())) {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
            }
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
        String nickname = aranarthPlayer.getNickname();
        String saintRank = getSaintRank(aranarthPlayer);
        String councilRank = getCouncilRank(aranarthPlayer);
        String rank = getRank(aranarthPlayer);

        String prefix = "⊰";

        prefix += saintRank;
        prefix += councilRank;
        prefix += rank;

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
     * Provides the String portion of the player's rank.
     * @param aranarthPlayer The AranarthPlayer that is being analyzed.
     * @return The String portion of the player's rank.
     */
    private String getRank(AranarthPlayer aranarthPlayer) {
        int rank = aranarthPlayer.getRank();
        if (aranarthPlayer.getPronouns() == Pronouns.MALE) {
            switch (rank) {
                case 1: return "&d[&aEsquire&d] &r";
                case 2: return "&7[&fKnight&7] &r";
                case 3: return "&5[&dBaron&5] &r";
                case 4: return "&8[&7Count&8] &r";
                case 5: return "&6[&eDuke&6] &r";
                case 6: return "&6[&bPrince&6] &r";
                case 7: return "&6[&9King&6] &r";
                case 8: return "&6[&4Emperor&6] &r";
            }
        } else {
            switch (rank) {
                case 1: return "&d[&aEsquire&d] &r";
                case 2: return "&7[&fKnight&7] &r";
                case 3: return "&5[&dBaroness&5] &r";
                case 4: return "&8[&7Countess&8] &r";
                case 5: return "&6[&eDuchess&6] &r";
                case 6: return "&6[&bPrincess&6] &r";
                case 7: return "&6[&9Queen&6] &r";
                case 8: return "&6[&4Empress&6] &r";
            }
        }
        return "&8[&aPeasant&8] &r";
    }

    /**
     * Provides the String portion of the player's Saint rank.
     * @param aranarthPlayer The AranarthPlayer that is being analyzed.
     * @return The String portion of the player's Saint rank.
     */
    private String getSaintRank(AranarthPlayer aranarthPlayer) {
        int saintRank = aranarthPlayer.getSaintRank();
        return switch (saintRank) {
            case 1 -> "&b⚜&r";
            case 2 -> "&e⚜&r";
            case 3 -> "&c⚜&r";
            default -> "";
        };
    }

    /**
     * Provides the String portion of the player's Council rank.
     * @param aranarthPlayer The AranarthPlayer that is being analyzed.
     * @return The String portion of the player's Council rank.
     */
    private String getCouncilRank(AranarthPlayer aranarthPlayer) {
        int councilRank = aranarthPlayer.getCouncilRank();
        return switch (councilRank) {
            case 1 -> "&3۞ &r";
            case 2 -> "&6۞ &r";
            case 3 -> "&4۞ &r";
            default -> "";
        };
    }

}
