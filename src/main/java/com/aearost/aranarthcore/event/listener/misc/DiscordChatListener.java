package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        // Handle player list commands from Discord
        String trimmed = content.trim().toLowerCase();
        if (trimmed.equals("!online") || trimmed.equals("!playerlist") || trimmed.equals("!list")) {
            e.getMessage().delete().queue();
            sendOnlinePlayersToDiscord(chatChannel);
            return;
        }

        String formatted = ChatUtils.translateToColor("&8[&6Discord&8] &e" + discordName + " &7» &r" + content);
        Component message = LegacyComponentSerializer.legacySection().deserialize(formatted);

        for (Player player : Bukkit.getOnlinePlayers()) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (aranarthPlayer.isTogglingChat()) {
                continue;
            }
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().deserialize(formatted));
    }

    private static void sendOnlinePlayersToDiscord(TextChannel channel) {
        List<Player> localPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
                    return ap != null && !ap.isVanished();
                })
                .collect(Collectors.toList());

        List<NetworkPlayer> remotePlayers = new ArrayList<>();
        if (NetworkManager.isActive()) {
            for (NetworkPlayer np : NetworkManager.getInstance().getRemoteRoster().values()) {
                if (!np.isVanished()) {
                    remotePlayers.add(np);
                }
            }
        }

        int total = localPlayers.size() + remotePlayers.size();
        int max = Bukkit.getMaxPlayers();

        Avatar currentAvatar = AvatarUtils.getCurrentAvatar();

        record PlayerEntry(int roleGroup, int roleTier, int playerRank, String displayName) {
        }

        List<PlayerEntry> entries = new ArrayList<>();
        for (Player p : localPlayers) {
            AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
            int[] fields = sortFields(p.getUniqueId(), ap.getCouncilRank(), ap.getArchitectRank(), ap.getSaintRank(), ap.getRank(), currentAvatar);
            String display = formatPlayerName(ap.getNickname(), ap.getUsername());
            entries.add(new PlayerEntry(fields[0], fields[1], fields[2], display));
        }
        for (NetworkPlayer np : remotePlayers) {
            int[] fields = sortFields(np.getUuid(), np.getCouncilRank(), np.getArchitectRank(), np.getSaintRank(), np.getRank(), currentAvatar);
            String rawNick = np.getNickname();
            String effectiveNick = (rawNick != null && !rawNick.isEmpty()) ? rawNick : np.getUsername();
            String display = formatPlayerName(effectiveNick, np.getUsername());
            entries.add(new PlayerEntry(fields[0], fields[1], fields[2], display));
        }

        entries.sort((a, b) -> {
            if (a.roleGroup() != b.roleGroup()) {
                return Integer.compare(b.roleGroup(), a.roleGroup());
            }
            if (a.roleTier() != b.roleTier()) {
                return Integer.compare(b.roleTier(), a.roleTier());
            }
            return Integer.compare(b.playerRank(), a.playerRank());
        });

        String playerList = entries.stream().map(PlayerEntry::displayName).collect(Collectors.joining(", "));
        String body = playerList.isEmpty() ? "No players are currently online." : playerList;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Online Players (" + total + "/" + max + ")")
                .setDescription(body)
                .setColor(new Color(255, 170, 0));

        channel.sendMessageEmbeds(embed.build()).queue(sent -> sent.delete().queueAfter(10, TimeUnit.SECONDS));
    }

    private static int[] sortFields(UUID uuid, int councilRank, int architectRank, int saintRank, int rank, Avatar currentAvatar) {
        if (councilRank >= 1) {
            return new int[]{5, councilRank, rank};
        }
        if (architectRank >= 1) {
            return new int[]{4, architectRank, rank};
        }
        if (saintRank >= 1) {
            return new int[]{3, saintRank, rank};
        }
        if (currentAvatar != null && uuid.equals(currentAvatar.getUuid())) {
            return new int[]{2, 0, rank};
        }
        return new int[]{1, 0, rank};
    }

    private static String formatPlayerName(String nickname, String username) {
        String cleanNick = ChatUtils.stripColorFormatting(nickname);
        String cleanUsername = ChatUtils.stripColorFormatting(username);
        if (cleanNick.equalsIgnoreCase(cleanUsername)) {
            return cleanUsername;
        }
        return cleanNick + " (" + cleanUsername + ")";
    }


}
