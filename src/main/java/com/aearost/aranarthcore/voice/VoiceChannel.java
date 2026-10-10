package com.aearost.aranarthcore.voice;

/**
 * The /vc channels a player can be in.
 */
public enum VoiceChannel {

    /** Everyone in the global channel, across both servers. */
    GLOBAL,
    /** Everyone in the local channel within LOCAL_RADIUS blocks, with 3D audio. Same server only. */
    LOCAL,
    /** Dominion members, filtered by both players' toggled dominion chat type. Across both servers. */
    DOMINION;

    public static final double LOCAL_RADIUS = 250;

    /**
     * Provides the channel matching the input name, or null if there is none.
     */
    public static VoiceChannel fromName(String name) {
        for (VoiceChannel channel : values()) {
            if (channel.name().equalsIgnoreCase(name)) {
                return channel;
            }
        }
        return null;
    }

    /**
     * Provides the lang key of the channel's display name.
     */
    public String getLangKey() {
        return "voicechat.channel_" + name().toLowerCase();
    }
}
