package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Static accessor for player-facing messages defined in lang property files.
 * The active-player thread-local is set by PlayerCommandPreprocessEventListener before
 * each command runs, so all synchronous Lang.get() calls during command processing
 * automatically return messages in the player's preferred locale without needing
 * per-call changes at every call site.
 * Examples:
 *   chatMessage(Lang.get("general.no_permission"))
 *   chatMessage(Lang.getFor(player, "general.no_permission"))
 */
public class Lang {

    private static final ThreadLocal<UUID> activePlayer = new ThreadLocal<>();

    private Lang() {}

    /**
     * Sets the active player for the current thread.
     * Should be called before a command handler runs so that Lang.get() uses their locale.
     */
    public static void setActivePlayer(UUID uuid) {
        activePlayer.set(uuid);
    }

    /**
     * Clears the active player for the current thread.
     */
    public static void clearActivePlayer() {
        activePlayer.remove();
    }

    /**
     * Returns the translated message for the given key.
     * If an active player is set on this thread (e.g. during command processing),
     * the message is returned in that player's preferred locale.
     * Any {placeholder} tokens are replaced with the provided pairs (name, value, ...).
     */
    public static String get(String key, Object... args) {
        UUID uuid = activePlayer.get();
        if (uuid != null) {
            AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
            if (ap != null) {
                return LangManager.getInstance().get(ap.getLanguage(), key, args);
            }
        }
        return LangManager.getInstance().get(key, args);
    }

    /**
     * Returns the translated message for the given key in the player's preferred locale.
     * Falls back to en_US if their locale is unavailable or a key is missing.
     * Use this when the player context is explicit and you do not want to rely on
     * the thread-local active player.
     */
    public static String getFor(Player player, String key, Object... args) {
        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        String locale = (ap != null) ? ap.getLanguage() : "en_US";
        return LangManager.getInstance().get(locale, key, args);
    }
}
