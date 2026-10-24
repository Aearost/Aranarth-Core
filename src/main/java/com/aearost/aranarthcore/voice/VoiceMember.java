package com.aearost.aranarthcore.voice;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * An immutable snapshot of a player in a /vc channel.
 *
 * @param uuid             The player's UUID.
 * @param nickname         The player's nickname, used in join/leave messages.
 * @param channel          The channel the player is in.
 * @param dominionId       The player's dominion ID, or null if they are not in a dominion.
 * @param allowedDominions The dominion IDs included by the player's toggled dominion chat type.
 * @param world            The player's world name, or null for players on the other server.
 * @param x                The player's X coordinate.
 * @param y                The player's eye Y coordinate.
 * @param z                The player's Z coordinate.
 */
public record VoiceMember(UUID uuid, String nickname, VoiceChannel channel, UUID dominionId,
                          Set<UUID> allowedDominions, String world, double x, double y, double z) {

    /**
     * Determines whether the listener can hear this member when they speak.
     *
     * @param listener The member that would hear the audio.
     * @return Whether the listener can hear this member.
     */
    public boolean canBeHeardBy(VoiceMember listener) {
        if (listener.uuid.equals(uuid) || listener.channel != channel) {
            return false;
        }
        return switch (channel) {
            case GLOBAL, COUNCIL -> true;
            case LOCAL -> {
                if (world == null || !world.equals(listener.world)) {
                    yield false;
                }
                double dx = x - listener.x;
                double dy = y - listener.y;
                double dz = z - listener.z;
                yield dx * dx + dy * dy + dz * dz <= VoiceChannel.LOCAL_RADIUS * VoiceChannel.LOCAL_RADIUS;
            }
            // Both players must include each other's dominion in their toggled dominion chat type
            case DOMINION -> dominionId != null && listener.dominionId != null
                    && allowedDominions.contains(listener.dominionId)
                    && listener.allowedDominions.contains(dominionId);
        };
    }

    /**
     * Determines whether the voice-relevant parts of the member differ, ignoring their position.
     */
    public boolean hasSameStateAs(VoiceMember other) {
        return other != null && channel == other.channel && nickname.equals(other.nickname)
                && Objects.equals(dominionId, other.dominionId)
                && allowedDominions.equals(other.allowedDominions);
    }
}
