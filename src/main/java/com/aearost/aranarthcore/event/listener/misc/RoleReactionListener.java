package com.aearost.aranarthcore.event.listener.misc;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReaction;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles self-assignable roles via emoji reactions on the info channel message.
 */
public class RoleReactionListener {

    private static final String CHANNEL_ID = "1440037933129138238";
    private static final String MESSAGE_ID = "1516255972883169340";

    /**
     * Maps each emoji unicode string to the Discord role ID it controls.
     */
    private static final Map<String, String> EMOJI_TO_ROLE_ID = new LinkedHashMap<>();

    static {
        EMOJI_TO_ROLE_ID.put("⚡", "1515810206741823508"); // Boosts
        EMOJI_TO_ROLE_ID.put("🔔", "1516255725205192855"); // Announcements
    }

    /** Tracks who reacted last poll so we only act on changes. */
    private final Map<String, Set<String>> previousReactors = new HashMap<>();

    private static String normalizeEmoji(String emoji) {
        return emoji.replace("\uFE0F", "").replace("\uFE0E", "");
    }

    /**
     * Adds the configured emoji reactions to the role message.
     */
    public void initReactions() {
        JDA jda = DiscordSRV.getPlugin().getJda();
        TextChannel channel = jda.getTextChannelById(CHANNEL_ID);
        if (channel == null) {
            return;
        }
        channel.retrieveMessageById(MESSAGE_ID).queue(message -> {
            for (String emoji : EMOJI_TO_ROLE_ID.keySet()) {
                message.addReaction(emoji).queue(null, t -> {});
            }
        });
    }

    public void pollReactions() {
        JDA jda = DiscordSRV.getPlugin().getJda();
        TextChannel channel = jda.getTextChannelById(CHANNEL_ID);
        if (channel == null) {
            return;
        }
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (guild == null) {
            return;
        }

        channel.retrieveMessageById(MESSAGE_ID).queue(message -> {
            for (Map.Entry<String, String> entry : EMOJI_TO_ROLE_ID.entrySet()) {
                String emoji = entry.getKey();
                String roleId = entry.getValue();

                Role role = guild.getRoleById(roleId);
                if (role == null) {
                    continue;
                }

                Optional<MessageReaction> reactionOpt = message.getReactions().stream()
                    .filter(r -> r.getReactionEmote().isEmoji()
                            && normalizeEmoji(r.getReactionEmote().getEmoji()).equals(emoji))
                    .findFirst();

                if (reactionOpt.isPresent()) {
                    reactionOpt.get().retrieveUsers().queue(users -> {
                        Set<String> current = users.stream()
                            .filter(u -> !u.isBot())
                            .map(u -> u.getId())
                            .collect(Collectors.toSet());

                        Set<String> previous = previousReactors.getOrDefault(emoji, new HashSet<>());

                        for (String userId : current) {
                            if (!previous.contains(userId)) {
                                guild.addRoleToMember(userId, role).queue(null, t -> {});
                            }
                        }

                        for (String userId : previous) {
                            if (!current.contains(userId)) {
                                guild.removeRoleFromMember(userId, role).queue(null, t -> {});
                            }
                        }

                        previousReactors.put(emoji, current);
                    }, t -> {});
                } else {
                    Set<String> previous = previousReactors.remove(emoji);
                    if (previous != null) {
                        for (String userId : previous) {
                            guild.removeRoleFromMember(userId, role).queue(null, t -> {});
                        }
                    }
                }
            }
        }, t -> {});
    }
}
