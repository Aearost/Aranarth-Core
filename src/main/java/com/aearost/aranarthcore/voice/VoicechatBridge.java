package com.aearost.aranarthcore.voice;

import com.aearost.aranarthcore.AranarthCore;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The only class that touches the Simple Voice Chat API, so AranarthCore still loads on servers without the voicechat plugin.
 * Everything here runs on Simple Voice Chat's own network threads, so it only reads the immutable snapshots in VoiceChatManager.
 */
public class VoicechatBridge implements VoicechatPlugin {

    private static final String CATEGORY_GLOBAL = "ac_global";
    private static final String CATEGORY_LOCAL = "ac_local";
    private static final String CATEGORY_DOMINION = "ac_dominion";
    private static final String CATEGORY_COUNCIL = "ac_council";

    private static volatile VoicechatServerApi api;
    private static final Map<String, StaticAudioChannel> remoteChannels = new ConcurrentHashMap<>();

    /**
     * Registers AranarthCore with Simple Voice Chat.
     */
    public static void register() {
        BukkitVoicechatService service = Bukkit.getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            service.registerPlugin(new VoicechatBridge());
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Voice] Registered with Simple Voice Chat");
        } else {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Voice] Simple Voice Chat is installed but its service could not be found");
        }
    }

    @Override
    public String getPluginId() {
        return "aranarthcore";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        // Simple Voice Chat disables itself if its voice server fails to start (i.e the UDP port is taken)
        registration.registerEvent(VoicechatServerStoppedEvent.class, e -> {
            api = null;
            remoteChannels.clear();
        });
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(PlayerConnectedEvent.class, this::onPlayerConnected);
    }

    private void onServerStarted(VoicechatServerStartedEvent e) {
        api = e.getVoicechat();
        // Lets players adjust the volume of each /vc channel separately in their voice chat settings
        api.registerVolumeCategory(api.volumeCategoryBuilder().setId(CATEGORY_GLOBAL).setName("Global")
                .setDescription("/vc global").build());
        api.registerVolumeCategory(api.volumeCategoryBuilder().setId(CATEGORY_LOCAL).setName("Local")
                .setDescription("/vc local").build());
        api.registerVolumeCategory(api.volumeCategoryBuilder().setId(CATEGORY_DOMINION).setName("Dominion")
                .setDescription("/vc dominion").build());
        api.registerVolumeCategory(api.volumeCategoryBuilder().setId(CATEGORY_COUNCIL).setName("Council")
                .setDescription("/vc council").build());
    }

    private void onPlayerConnected(PlayerConnectedEvent e) {
        UUID uuid = e.getConnection().getPlayer().getUuid();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> VoiceChatManager.onVoiceConnected(uuid));
    }

    /**
     * Replaces Simple Voice Chat's proximity voice entirely: audio only reaches players in the speaker's /vc channel.
     */
    private void onMicrophonePacket(MicrophonePacketEvent e) {
        e.cancel();
        VoicechatServerApi currentApi = api;
        VoicechatConnection senderConnection = e.getSenderConnection();
        if (currentApi == null || senderConnection == null) {
            return;
        }

        UUID speakerUuid = senderConnection.getPlayer().getUuid();
        VoiceMember speaker = VoiceChatManager.getLocalMember(speakerUuid);
        if (speaker == null) {
            return;
        }

        MicrophonePacket packet = e.getPacket();
        for (VoiceMember listener : VoiceChatManager.getLocalMembers()) {
            if (!speaker.canBeHeardBy(listener)) {
                continue;
            }
            VoicechatConnection listenerConnection = getListeningConnection(currentApi, listener.uuid());
            if (listenerConnection == null) {
                continue;
            }
            switch (speaker.channel()) {
                case LOCAL ->
                        currentApi.sendLocationalSoundPacketTo(listenerConnection, packet.locationalSoundPacketBuilder()
                                .position(currentApi.createPosition(speaker.x(), speaker.y(), speaker.z()))
                                .distance((float) VoiceChannel.LOCAL_RADIUS)
                                .category(CATEGORY_LOCAL)
                                .build());
                case GLOBAL, DOMINION, COUNCIL ->
                        currentApi.sendStaticSoundPacketTo(listenerConnection, packet.staticSoundPacketBuilder()
                                .category(getCategory(speaker.channel()))
                                .build());
            }
        }

        // Local voice never leaves this server
        if (speaker.channel() != VoiceChannel.LOCAL) {
            VoiceLink link = VoiceLink.getInstance();
            if (link != null) {
                link.sendAudio(speakerUuid, packet.getOpusEncodedData());
            }
        }
    }

    /**
     * Plays audio spoken by a player on the other server to everyone here who can hear them.
     */
    public static void playRemoteAudio(VoiceMember speaker, byte[] opus) {
        VoicechatServerApi currentApi = api;
        if (currentApi == null) {
            return;
        }
        for (VoiceMember listener : VoiceChatManager.getLocalMembers()) {
            if (!speaker.canBeHeardBy(listener)) {
                continue;
            }
            VoicechatConnection listenerConnection = getListeningConnection(currentApi, listener.uuid());
            if (listenerConnection == null) {
                continue;
            }
            String key = listener.uuid() + ":" + speaker.uuid() + ":" + listener.world();
            StaticAudioChannel channel = remoteChannels.computeIfAbsent(key, k -> {
                StaticAudioChannel created = currentApi.createStaticAudioChannel(speaker.uuid(),
                        listenerConnection.getPlayer().getServerLevel(), listenerConnection);
                if (created != null) {
                    created.setCategory(getCategory(speaker.channel()));
                }
                return created;
            });
            if (channel != null) {
                channel.send(opus);
            }
        }
    }

    /**
     * Clears the cached audio channels for remote speakers, i.e when players change channel or leave.
     */
    public static void clearRemoteChannels() {
        remoteChannels.clear();
    }

    /**
     * Determines whether the player has the Simple Voice Chat mod and is connected to the voice server.
     */
    public static boolean isConnected(UUID uuid) {
        VoicechatServerApi currentApi = api;
        if (currentApi == null) {
            return false;
        }
        VoicechatConnection connection = currentApi.getConnectionOf(uuid);
        return connection != null && connection.isInstalled() && connection.isConnected();
    }

    /**
     * Determines whether the player has the Simple Voice Chat mod installed at all.
     */
    public static boolean isInstalled(UUID uuid) {
        VoicechatServerApi currentApi = api;
        if (currentApi == null) {
            return false;
        }
        VoicechatConnection connection = currentApi.getConnectionOf(uuid);
        return connection != null && connection.isInstalled();
    }

    public static boolean isApiReady() {
        return api != null && Bukkit.getPluginManager().isPluginEnabled("voicechat");
    }

    /**
     * Provides the player's connection if they can currently hear voice chat, otherwise null.
     */
    private static VoicechatConnection getListeningConnection(VoicechatServerApi currentApi, UUID uuid) {
        VoicechatConnection connection = currentApi.getConnectionOf(uuid);
        if (connection == null || !connection.isConnected() || connection.isDisabled()) {
            return null;
        }
        return connection;
    }

    private static String getCategory(VoiceChannel channel) {
        return switch (channel) {
            case GLOBAL -> CATEGORY_GLOBAL;
            case LOCAL -> CATEGORY_LOCAL;
            case DOMINION -> CATEGORY_DOMINION;
            case COUNCIL -> CATEGORY_COUNCIL;
        };
    }
}
