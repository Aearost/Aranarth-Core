package com.aearost.aranarthcore.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Translates text-based emoji shorthands into Unicode emoji characters.
 */
public class EmojiUtils {

    private static final char VS16 = '\uFE0F';

    private static final Map<String, String> DISCORD_SHORTHANDS = new LinkedHashMap<>();
    private static final Map<String, String> TEXT_EMOTICONS = new LinkedHashMap<>();
    private static final Map<Pattern, String> WORD_BOUNDARY_EMOTICONS = new LinkedHashMap<>();

    static {
        DISCORD_SHORTHANDS.put(":sob:", "\uD83D\uDE2D");            // 😭
        DISCORD_SHORTHANDS.put(":cry:", "\uD83D\uDE22");            // 😢
        DISCORD_SHORTHANDS.put(":pleading_face:", "\uD83E\uDD7A");  // 🥺
        DISCORD_SHORTHANDS.put(":joy:", "\uD83D\uDE02");            // 😂
        DISCORD_SHORTHANDS.put(":rofl:", "\uD83E\uDD23");           // 🤣
        DISCORD_SHORTHANDS.put(":skull:", "\uD83D\uDC80");          // 💀
        DISCORD_SHORTHANDS.put(":fire:", "\uD83D\uDD25");           // 🔥
        DISCORD_SHORTHANDS.put(":100:", "\uD83D\uDCAF");            // 💯
        DISCORD_SHORTHANDS.put(":broken_heart:", "\uD83D\uDC94");   // 💔
        DISCORD_SHORTHANDS.put(":heart:", "\u2764");                 // ❤
        DISCORD_SHORTHANDS.put(":thumbsup:", "\uD83D\uDC4D");       // 👍
        DISCORD_SHORTHANDS.put(":thumbsdown:", "\uD83D\uDC4E");     // 👎
        DISCORD_SHORTHANDS.put(":wave:", "\uD83D\uDC4B");           // 👋
        DISCORD_SHORTHANDS.put(":clap:", "\uD83D\uDC4F");           // 👏
        DISCORD_SHORTHANDS.put(":raised_hands:", "\uD83D\uDE4C");   // 🙌
        DISCORD_SHORTHANDS.put(":eyes:", "\uD83D\uDC40");           // 👀
        DISCORD_SHORTHANDS.put(":pray:", "\uD83D\uDE4F");           // 🙏
        DISCORD_SHORTHANDS.put(":sparkles:", "\u2728");              // ✨
        DISCORD_SHORTHANDS.put(":star:", "\u2B50");                  // ⭐
        DISCORD_SHORTHANDS.put(":thinking:", "\uD83E\uDD14");       // 🤔
        DISCORD_SHORTHANDS.put(":flushed:", "\uD83D\uDE33");        // 😳
        DISCORD_SHORTHANDS.put(":rage:", "\uD83D\uDE21");           // 😡
        DISCORD_SHORTHANDS.put(":ok_hand:", "\uD83D\uDC4C");        // 👌
        DISCORD_SHORTHANDS.put(":pensive:", "\uD83D\uDE14");        // 😔
        DISCORD_SHORTHANDS.put(":sweat_smile:", "\uD83D\uDE05");    // 😅
        DISCORD_SHORTHANDS.put(":muscle:", "\uD83D\uDCAA");         // 💪
        DISCORD_SHORTHANDS.put(":heart_eyes:", "\uD83D\uDE0D");     // 😍

        // Text emoticons - longer patterns before shorter ones that share a prefix
        TEXT_EMOTICONS.put(":-)", "\uD83D\uDE0A");   // 😊
        TEXT_EMOTICONS.put(":)", "\uD83D\uDE0A");    // 😊
        TEXT_EMOTICONS.put("=)", "\uD83D\uDE0A");    // 😊
        TEXT_EMOTICONS.put(":-(", "\uD83D\uDE22");   // 😢
        TEXT_EMOTICONS.put(":'(", "\uD83D\uDE22");   // 😢
        TEXT_EMOTICONS.put(":(", "\uD83D\uDE22");    // 😢
        TEXT_EMOTICONS.put(":-D", "\uD83D\uDE04");   // 😄
        TEXT_EMOTICONS.put(":D", "\uD83D\uDE04");    // 😄
        TEXT_EMOTICONS.put(";-)", "\uD83D\uDE09");   // 😉
        TEXT_EMOTICONS.put(";)", "\uD83D\uDE09");    // 😉
        TEXT_EMOTICONS.put(":-P", "\uD83D\uDE1B");   // 😛
        TEXT_EMOTICONS.put(":-p", "\uD83D\uDE1B");   // 😛
        TEXT_EMOTICONS.put(":P", "\uD83D\uDE1B");    // 😛
        TEXT_EMOTICONS.put(":p", "\uD83D\uDE1B");    // 😛
        TEXT_EMOTICONS.put(":-O", "\uD83D\uDE2E");   // 😮
        TEXT_EMOTICONS.put(":-o", "\uD83D\uDE2E");   // 😮
        TEXT_EMOTICONS.put(":O", "\uD83D\uDE2E");    // 😮
        TEXT_EMOTICONS.put(":o", "\uD83D\uDE2E");    // 😮
        TEXT_EMOTICONS.put(">.<", "\uD83D\uDE23");   // 😣
        TEXT_EMOTICONS.put(">_<", "\uD83D\uDE23");   // 😣
        TEXT_EMOTICONS.put("</3", "\uD83D\uDC94");   // 💔
        TEXT_EMOTICONS.put("<3", "\u2764");           // ❤
        TEXT_EMOTICONS.put(":*", "\uD83D\uDE18");    // 😘
        TEXT_EMOTICONS.put("B-)", "\uD83D\uDE0E");   // 😎
        TEXT_EMOTICONS.put("B)", "\uD83D\uDE0E");    // 😎

        WORD_BOUNDARY_EMOTICONS.put(Pattern.compile("\\bo7\\b"), "\uD83E\uDEE1");        // 🫡
        WORD_BOUNDARY_EMOTICONS.put(Pattern.compile("\\b[xX][dD]\\b"), "\uD83D\uDE06"); // 😆
    }

    /**
     * Translates all recognised emoji shorthands in the given message and strips VS16.
     */
    public static String translateEmojis(String message) {
        for (Map.Entry<String, String> entry : DISCORD_SHORTHANDS.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : TEXT_EMOTICONS.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Pattern, String> entry : WORD_BOUNDARY_EMOTICONS.entrySet()) {
            message = entry.getKey().matcher(message).replaceAll(entry.getValue());
        }
        // Strip VS16 so Minecraft doesn't render it as a visible glyph
        return message.replace(String.valueOf(VS16), "");
    }
}
