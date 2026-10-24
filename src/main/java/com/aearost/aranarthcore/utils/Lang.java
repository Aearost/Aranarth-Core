package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Static accessor for player-facing messages defined in lang property files.
 * Every message is resolved for an explicit recipient so it always matches that player's locale.
 * Examples:
 *   chatMessage(Lang.getFor(player, "general.no_permission"))
 *   Lang.broadcast("dominion.created_broadcast", "player", nickname, "name", dominionName)
 */
public class Lang {

    private Lang() {}

    /**
     * Returns the translated message for the given key in the player's preferred locale.
     * Falls back to en_US if their locale is unavailable or a key is missing.
     */
    public static String getFor(Player player, String key, Object... args) {
        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        String locale = (ap != null) ? ap.getLanguage() : "en_US";
        return LangManager.getInstance().get(locale, key, args);
    }

    /**
     * Returns the translated message for the given key in the preferred locale of the player with this UUID,
     * for players who are not online on this server.
     */
    public static String getFor(UUID uuid, String key, Object... args) {
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        String locale = (ap != null) ? ap.getLanguage() : "en_US";
        return LangManager.getInstance().get(locale, key, args);
    }

    /**
     * Returns the translated message for the given key in the sender's preferred locale.
     * Non-player senders such as the console always receive en_US.
     */
    public static String getFor(CommandSender sender, String key, Object... args) {
        if (sender instanceof Player player) {
            return getFor(player, key, args);
        }
        return LangManager.getInstance().get(key, args);
    }

    /**
     * Wraps a lang key (and optional placeholder pairs) so it can be passed as a placeholder value and resolved
     * in each recipient's locale, including on the other servers when the message is broadcast.
     */
    public static String key(String key, Object... args) {
        StringBuilder nested = new StringBuilder(LangManager.NESTED_PREFIX).append(key);
        // Placeholder values must not be nested keys with arguments of their own, as the separators would collide
        for (Object arg : args) {
            nested.append(LangManager.NESTED_SEPARATOR).append(arg);
        }
        return nested.toString();
    }

    /**
     * Resolves a value that may have been built with key() in the recipient's language, for places that print it
     * without a lang template.
     */
    public static String resolve(CommandSender recipient, String value) {
        String locale = "en_US";
        if (recipient instanceof Player player) {
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            if (ap != null) {
                locale = ap.getLanguage();
            }
        }
        return LangManager.getInstance().resolveNested(locale, value);
    }

    /**
     * Whether the value was built with key() and must be resolved per recipient.
     */
    public static boolean isNested(String value) {
        return value != null && value.startsWith(LangManager.NESTED_PREFIX);
    }

    /**
     * Sends a value that may have been built with key() to every online player in their own language, and to the
     * console in en_US.
     */
    public static void sendToAll(String value) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatUtils.translateToColor(resolve(player, value)));
        }
        Bukkit.getConsoleSender().sendMessage(ChatUtils.translateToColor(plain(value)));
    }

    /**
     * Resolves a value that may have been built with key() to its en_US text, e.g. for console logs.
     */
    public static String plain(String value) {
        return LangManager.getInstance().resolveNested("en_US", value);
    }

    /**
     * Returns the en_US message for the given key.
     * Use this for text that does not go to a player, such as Discord notifications.
     */
    public static String getDefault(String key, Object... args) {
        return LangManager.getInstance().get(key, args);
    }

    /**
     * Sends the prefixed message to every online player in their own locale and to the console in en_US,
     * then relays it so players on the other servers also receive it in their own locale.
     */
    public static void broadcast(String key, Object... args) {
        broadcastLocal(key, args);
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishLangBroadcast(key, args);
        }
    }

    /**
     * Sends the prefixed message to every online player on this server in their own locale and to the console in en_US.
     */
    public static void broadcastLocal(String key, Object... args) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatUtils.chatMessage(getFor(player, key, args)));
        }
        Bukkit.getConsoleSender().sendMessage(ChatUtils.chatMessage(getDefault(key, args)));
    }
}
