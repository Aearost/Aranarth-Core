package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
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

        String formatted = ChatUtils.translateToColor("&8[&6Discord&8] &e" + discordName + " &7» &r" + content);
        Component message = LegacyComponentSerializer.legacySection().deserialize(formatted);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().deserialize(formatted));
    }
}
