package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;

/**
 * Handles formatting chat messages from Discord to in-game.
 */
public class DiscordChatListener {

    public DiscordChatListener(AranarthCore plugin) {
        DiscordSRV.api.subscribe(this);
    }

    public void unsubscribe() {
        DiscordSRV.api.unsubscribe(this);
    }

    @Subscribe
    public void onDiscordMessage(DiscordGuildMessageReceivedEvent e) {
        // Only handle messages from the linked chat channel
        TextChannel chatChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("chat");
        if (chatChannel == null || !e.getChannel().getId().equals(chatChannel.getId())) {
            return;
        }

        // Ignore bot messages
        if (e.getMessage().getAuthor().isBot()) {
            return;
        }

        Member member = e.getMember();
        String discordName = member != null ? member.getEffectiveName() : e.getMessage().getAuthor().getName();
        String content = ChatUtils.stripColorFormatting(e.getMessage().getContentDisplay());

        // Player list commands are answered by the Aranarth Support bot (/aranarth online), so they are never relayed in-game
        String trimmed = content.trim().toLowerCase();
        if (trimmed.equals("!online") || trimmed.equals("!playerlist") || trimmed.equals("!list")) {
            return;
        }

        String formatted = ChatUtils.translateToColor("&8[&6Discord&8] &e" + discordName + " &7» &r" + ChatUtils.discordMarkdownToFormatCodes(content));
        Component message = ChatUtils.containsSpoiler(content)
                ? buildSpoilerMessage(discordName, content)
                : LegacyComponentSerializer.legacySection().deserialize(formatted);

        for (Player player : Bukkit.getOnlinePlayers()) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (aranarthPlayer.isTogglingChat()) {
                continue;
            }
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().deserialize(formatted));
    }

    /**
     * Builds the in-game message for a Discord message containing ||spoilers||, replacing each one with a
     * placeholder that shows the hidden text when hovered over.
     *
     * @param discordName The name of the Discord user.
     * @param content     The Discord message, without color codes.
     * @return The message, including the Discord prefix.
     */
    private Component buildSpoilerMessage(String discordName, String content) {
        Component result = LegacyComponentSerializer.legacySection().deserialize(
                ChatUtils.translateToColor("&8[&6Discord&8] &e" + discordName + " &7» &r"));
        Matcher matcher = ChatUtils.SPOILER_PATTERN.matcher(content);
        int lastEnd = 0;
        int spoilerCount = 0;
        // Only the first few spoilers are hidden, the rest are left as typed
        while (spoilerCount < ChatUtils.MAX_SPOILERS_PER_MESSAGE && matcher.find()) {
            spoilerCount++;
            if (matcher.start() > lastEnd) {
                result = result.append(deserializeMarkdown(content.substring(lastEnd, matcher.start())));
            }
            String hidden = ChatUtils.translateToColor("&r" + ChatUtils.discordMarkdownToFormatCodes(matcher.group(1)));
            result = result.append(ChatUtils.buildSpoilerPlaceholder(hidden));
            lastEnd = matcher.end();
        }
        if (lastEnd < content.length()) {
            result = result.append(deserializeMarkdown(content.substring(lastEnd)));
        }
        return result;
    }

    /**
     * Converts a part of a Discord message to a Component, keeping its bold and italics.
     */
    private Component deserializeMarkdown(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(
                ChatUtils.translateToColor("&r" + ChatUtils.discordMarkdownToFormatCodes(text)));
    }
}
