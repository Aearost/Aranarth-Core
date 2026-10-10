package com.aearost.aranarthcore.voice;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the /vc channels (global, local, dominion) across Survival and SMP.
 */
public class VoiceChatManager {

    private static final long HANDOFF_EXPIRY_MS = 60_000;

    private static final Map<UUID, VoiceMember> localMembers = new ConcurrentHashMap<>();
    private static final Map<UUID, VoiceMember> remoteMembers = new ConcurrentHashMap<>();

    private static final Map<UUID, DominionInfo> dominionInfoCache = new HashMap<>();
    private static final Map<UUID, VoiceMember> lastSentStates = new HashMap<>();
    private static final Map<UUID, PendingHandoff> pendingHandoffs = new HashMap<>();
    private static final Map<UUID, Set<UUID>> dominionConnections = new HashMap<>();
    private static final Map<UUID, VoiceChannel> lastComputedChannels = new HashMap<>();
    private static final Set<UUID> silentChanges = new HashSet<>();
    private static final Map<UUID, String> lastKnownNicknames = new HashMap<>();

    private static boolean isAvailable = false;

    private record DominionInfo(UUID dominionId, Set<UUID> allowedDominions) {
    }

    private record PendingHandoff(VoiceChannel channel, VoiceMember previousState, long expiry) {
    }

    /**
     * Hooks into Simple Voice Chat if it is installed and starts the voice link and snapshot tasks.
     */
    public static void initialize(AranarthCore plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("voicechat")) {
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Voice] Simple Voice Chat is not installed, so /vc is disabled");
            return;
        }
        VoicechatBridge.register();
        isAvailable = true;

        if (AranarthCore.isPublicServer()) {
            VoiceLink.start();
        }

        // Positions are refreshed often so local 3D audio follows the speaker smoothly
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> refreshSnapshots(false), 2L, 2L);
        // Dominion relations change rarely, so they are recomputed (and synced to the other server) once a second
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            refreshSnapshots(true);
            expireHandoffs();
        }, 20L, 20L);
    }

    public static void shutdown() {
        VoiceLink.shutdown();
    }

    /**
     * Determines whether Simple Voice Chat is installed and ready on this server.
     */
    public static boolean isAvailable() {
        return isAvailable && VoicechatBridge.isApiReady();
    }

    public static boolean isInstalled(UUID uuid) {
        return isAvailable() && VoicechatBridge.isInstalled(uuid);
    }

    public static boolean isConnected(UUID uuid) {
        return isAvailable() && VoicechatBridge.isConnected(uuid);
    }

    public static VoiceMember getLocalMember(UUID uuid) {
        return localMembers.get(uuid);
    }

    public static Collection<VoiceMember> getLocalMembers() {
        return localMembers.values();
    }

    /**
     * Provides the nicknames of every player in the channel across both servers, sorted alphabetically.
     *
     * @param channel         The channel.
     * @param canSeeVanished  Whether vanished players should be included.
     */
    public static List<String> getNicknamesInChannel(VoiceChannel channel, boolean canSeeVanished) {
        List<String> nicknames = new ArrayList<>();
        for (VoiceMember member : localMembers.values()) {
            if (member.channel() != channel) {
                continue;
            }
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(member.uuid());
            if (canSeeVanished || aranarthPlayer == null || !aranarthPlayer.isVanished()) {
                nicknames.add(member.nickname());
            }
        }
        for (VoiceMember member : remoteMembers.values()) {
            if (member.channel() != channel) {
                continue;
            }
            NetworkPlayer networkPlayer = NetworkManager.isActive()
                    ? NetworkManager.getInstance().getRemotePlayer(member.uuid()) : null;
            if (canSeeVanished || networkPlayer == null || !networkPlayer.isVanished()) {
                nicknames.add(member.nickname());
            }
        }
        nicknames.sort(Comparator.comparing(nickname -> ChatUtils.stripColorFormatting(nickname).toLowerCase()));
        return nicknames;
    }

    /**
     * Provides the channel the player is currently in, or null if they are not in one.
     */
    public static VoiceChannel getChannel(UUID uuid) {
        VoiceMember member = localMembers.get(uuid);
        return member == null ? null : member.channel();
    }

    /**
     * Places the player in the channel, leaving their previous channel if they were in one.
     */
    public static void joinChannel(Player player, VoiceChannel channel) {
        UUID uuid = player.getUniqueId();
        VoiceMember previous = localMembers.get(uuid);
        dominionInfoCache.put(uuid, computeDominionInfo(player));
        VoiceMember member = buildMember(player, channel);
        localMembers.put(uuid, member);
        VoicechatBridge.clearRemoteChannels();

        if (previous != null) {
            notifyLocalPlayers(previous, false);
        }
        notifyLocalPlayers(member, true);
        sendState(uuid, member, true);

        player.sendMessage(ChatUtils.chatMessage(Lang.get("voicechat.joined", "channel", Lang.get(channel.getLangKey()))));
        playJingle(player, true);
        updateDominionConnections();
    }

    /**
     * Removes the player from their current channel.
     *
     * @param player   The player leaving.
     * @param isManual Whether the player used /vc leave, in which case they are sent a confirmation.
     */
    public static void leaveChannel(Player player, boolean isManual) {
        UUID uuid = player.getUniqueId();
        VoiceMember previous = localMembers.remove(uuid);
        dominionInfoCache.remove(uuid);
        if (previous == null) {
            return;
        }
        VoicechatBridge.clearRemoteChannels();
        notifyLocalPlayers(previous, false);
        sendState(uuid, null, true);

        if (isManual) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("voicechat.left", "channel", Lang.get(previous.channel().getLangKey()))));
            playJingle(player, false);
        }
        updateDominionConnections();
    }

    /**
     * Removes the player from their channel when they leave the server.
     */
    public static void onPlayerQuit(Player player, boolean isTransferring) {
        UUID uuid = player.getUniqueId();
        pendingHandoffs.remove(uuid);
        VoiceMember member = localMembers.get(uuid);
        if (member == null || !isAvailable) {
            return;
        }
        VoiceLink link = VoiceLink.getInstance();
        if (isTransferring && VoiceLink.isConnected()) {
            localMembers.remove(uuid);
            dominionInfoCache.remove(uuid);
            lastSentStates.remove(uuid);
            VoicechatBridge.clearRemoteChannels();
            link.sendHandoff(uuid, member.channel());
            silentChanges.add(uuid);
            updateDominionConnections();
        } else {
            leaveChannel(player, false);
        }
    }

    /**
     * Called when a player's voice chat connects, which happens a few seconds after they join.
     */
    public static void onVoiceConnected(UUID uuid) {
        PendingHandoff handoff = pendingHandoffs.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (handoff == null || player == null || handoff.expiry() < System.currentTimeMillis()) {
            return;
        }
        dominionInfoCache.put(uuid, computeDominionInfo(player));
        VoiceMember member = buildMember(player, handoff.channel());
        localMembers.put(uuid, member);
        VoicechatBridge.clearRemoteChannels();
        sendState(uuid, member, false);
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.rejoined", "channel",
                Lang.getFor(player, handoff.channel().getLangKey()))));
        silentChanges.add(uuid);
        updateDominionConnections();
    }

    /**
     * Applies a channel state change for a player on the other server.
     */
    public static void onRemoteState(UUID uuid, VoiceMember member, boolean announce) {
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            // A stale state for a player who has since moved to this server
            if (Bukkit.getPlayer(uuid) != null) {
                return;
            }
            VoiceMember previous = member == null ? remoteMembers.remove(uuid) : remoteMembers.put(uuid, member);
            if (isAvailable) {
                VoicechatBridge.clearRemoteChannels();
            }
            if (!announce) {
                silentChanges.add(uuid);
                updateDominionConnections();
                return;
            }
            boolean hasChangedChannel = previous == null || member == null || previous.channel() != member.channel();
            if (previous != null && hasChangedChannel) {
                notifyLocalPlayers(previous, false);
            }
            if (member != null && hasChangedChannel) {
                notifyLocalPlayers(member, true);
            }
            updateDominionConnections();
        });
    }

    /**
     * Plays audio from a player on the other server.
     */
    public static void onRemoteAudio(UUID uuid, byte[] opus) {
        VoiceMember speaker = remoteMembers.get(uuid);
        if (speaker != null && isAvailable) {
            VoicechatBridge.playRemoteAudio(speaker, opus);
        }
    }

    /**
     * Stores the channel of a player transferring here from the other server, so it can be restored once their voice chat connects.
     */
    public static void onRemoteHandoff(UUID uuid, VoiceChannel channel) {
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            VoiceMember previous = remoteMembers.remove(uuid);
            pendingHandoffs.put(uuid, new PendingHandoff(channel, previous, System.currentTimeMillis() + HANDOFF_EXPIRY_MS));
            silentChanges.add(uuid);
            updateDominionConnections();
            // Their voice chat may have already connected before the handoff arrived
            if (isConnected(uuid)) {
                onVoiceConnected(uuid);
            }
        });
    }

    /**
     * Sends every local member's state to the other server once the voice link connects.
     */
    public static void onLinkConnected() {
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            lastSentStates.clear();
            for (VoiceMember member : localMembers.values()) {
                sendState(member.uuid(), member, false);
            }
        });
    }

    /**
     * Forgets every player on the other server when the voice link drops. They are re-synced when it reconnects.
     */
    public static void onLinkDisconnected() {
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            silentChanges.addAll(remoteMembers.keySet());
            remoteMembers.clear();
            if (isAvailable) {
                VoicechatBridge.clearRemoteChannels();
            }
            updateDominionConnections();
        });
    }

    /**
     * Rebuilds the snapshots of all local members.
     *
     * @param isFullRefresh Whether dominion relations should be recomputed and changes synced to the other server.
     */
    private static void refreshSnapshots(boolean isFullRefresh) {
        for (UUID uuid : new ArrayList<>(localMembers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            VoiceMember current = localMembers.get(uuid);
            if (current == null) {
                continue;
            }
            if (player == null) {
                localMembers.remove(uuid);
                dominionInfoCache.remove(uuid);
                continue;
            }
            if (isFullRefresh) {
                dominionInfoCache.put(uuid, computeDominionInfo(player));
            }
            VoiceMember member = buildMember(player, current.channel());
            localMembers.put(uuid, member);
            if (isFullRefresh && !member.hasSameStateAs(lastSentStates.get(uuid))) {
                // Announced so the other server tells players when dominion chat type changes connect or disconnect them
                sendState(uuid, member, true);
            }
        }
        if (isFullRefresh) {
            updateDominionConnections();
        }
    }

    /**
     * Drops handoffs for players who never reconnected their voice chat, announcing that they left the call.
     */
    private static void expireHandoffs() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PendingHandoff>> iterator = pendingHandoffs.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingHandoff handoff = iterator.next().getValue();
            if (handoff.expiry() < now) {
                iterator.remove();
                if (handoff.previousState() != null) {
                    notifyLocalPlayers(handoff.previousState(), false);
                }
            }
        }
    }

    private static void sendState(UUID uuid, VoiceMember member, boolean announce) {
        if (member == null) {
            lastSentStates.remove(uuid);
        } else {
            lastSentStates.put(uuid, member);
        }
        VoiceLink link = VoiceLink.getInstance();
        if (link != null) {
            // Local members are synced so /vc who can list them, but they are never heard across servers
            // since players on the other server have no world in their snapshot
            link.sendState(uuid, member, announce);
        }
    }

    private static VoiceMember buildMember(Player player, VoiceChannel channel) {
        DominionInfo info = dominionInfoCache.get(player.getUniqueId());
        if (info == null) {
            info = computeDominionInfo(player);
        }
        Location eye = player.getEyeLocation();
        return new VoiceMember(player.getUniqueId(), AranarthUtils.getNickname(player), channel,
                info.dominionId(), info.allowedDominions(), eye.getWorld().getName(), eye.getX(), eye.getY(), eye.getZ());
    }

    /**
     * Computes the player's dominion and the dominions included by their toggled dominion chat type.
     */
    private static DominionInfo computeDominionInfo(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return new DominionInfo(null, Set.of());
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        String chatType = aranarthPlayer == null || aranarthPlayer.getDominionChatType() == null
                ? "dominion" : aranarthPlayer.getDominionChatType();

        Set<UUID> allowed = new HashSet<>();
        allowed.add(dominion.getId());
        if (chatType.equals("ally") || chatType.equals("allytruce")) {
            for (UUID alliedLeader : dominion.getAllied()) {
                Dominion alliedDominion = DominionUtils.getPlayerDominion(alliedLeader);
                if (alliedDominion != null && dominion.isAllied(alliedDominion)) {
                    allowed.add(alliedDominion.getId());
                }
            }
        }
        if (chatType.equals("truce") || chatType.equals("allytruce")) {
            for (UUID trucedLeader : dominion.getTruced()) {
                Dominion trucedDominion = DominionUtils.getPlayerDominion(trucedLeader);
                if (trucedDominion != null && dominion.isTruced(trucedDominion)) {
                    allowed.add(trucedDominion.getId());
                }
            }
        }
        return new DominionInfo(dominion.getId(), Set.copyOf(allowed));
    }

    /**
     * Tells every local player in the global call that the subject joined or left, with a jingle.
     */
    private static void notifyLocalPlayers(VoiceMember subject, boolean isJoining) {
        if (subject.channel() != VoiceChannel.GLOBAL) {
            return;
        }
        String key = isJoining ? "voicechat.player_joined" : "voicechat.player_left";
        for (VoiceMember listener : localMembers.values()) {
            if (!subject.canBeHeardBy(listener)) {
                continue;
            }
            Player player = Bukkit.getPlayer(listener.uuid());
            if (player == null) {
                continue;
            }
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, key, "player", subject.nickname(),
                    "channel", Lang.getFor(player, subject.channel().getLangKey()))));
            playJingle(player, isJoining);
        }
    }

    /**
     * Compares who each local dominion member can hear against the last check, and tells them about every change with a jingle.
     */
    private static void updateDominionConnections() {
        List<VoiceMember> dominionMembers = new ArrayList<>();
        for (VoiceMember member : localMembers.values()) {
            if (member.channel() == VoiceChannel.DOMINION) {
                dominionMembers.add(member);
            }
        }
        for (VoiceMember member : remoteMembers.values()) {
            if (member.channel() == VoiceChannel.DOMINION) {
                dominionMembers.add(member);
            }
        }

        Map<UUID, Set<UUID>> newConnections = new HashMap<>();
        for (VoiceMember listener : localMembers.values()) {
            if (listener.channel() != VoiceChannel.DOMINION) {
                continue;
            }
            Set<UUID> connected = new HashSet<>();
            for (VoiceMember speaker : dominionMembers) {
                if (speaker.canBeHeardBy(listener)) {
                    connected.add(speaker.uuid());
                }
            }
            newConnections.put(listener.uuid(), connected);

            Player player = Bukkit.getPlayer(listener.uuid());
            boolean hasListenerJustJoined = lastComputedChannels.get(listener.uuid()) != VoiceChannel.DOMINION;
            if (player == null || hasListenerJustJoined || silentChanges.contains(listener.uuid())) {
                continue;
            }

            Set<UUID> previous = dominionConnections.getOrDefault(listener.uuid(), Set.of());
            for (UUID added : connected) {
                if (!previous.contains(added) && !silentChanges.contains(added)) {
                    VoiceMember speaker = getMember(added);
                    boolean hasJustJoined = lastComputedChannels.get(added) != VoiceChannel.DOMINION;
                    sendConnectionMessage(player, speaker.nickname(), hasJustJoined ? "voicechat.player_joined" : "voicechat.player_connected", true);
                }
            }
            for (UUID removed : previous) {
                if (!connected.contains(removed) && !silentChanges.contains(removed)) {
                    VoiceMember speaker = getMember(removed);
                    boolean hasLeft = speaker == null || speaker.channel() != VoiceChannel.DOMINION;
                    String nickname = speaker != null ? speaker.nickname() : getLastKnownNickname(removed);
                    sendConnectionMessage(player, nickname, hasLeft ? "voicechat.player_left" : "voicechat.player_disconnected", false);
                }
            }
        }

        dominionConnections.clear();
        dominionConnections.putAll(newConnections);
        lastComputedChannels.clear();
        for (VoiceMember member : localMembers.values()) {
            lastComputedChannels.put(member.uuid(), member.channel());
            lastKnownNicknames.put(member.uuid(), member.nickname());
        }
        for (VoiceMember member : remoteMembers.values()) {
            lastComputedChannels.put(member.uuid(), member.channel());
            lastKnownNicknames.put(member.uuid(), member.nickname());
        }
        silentChanges.clear();
    }

    private static void sendConnectionMessage(Player player, String nickname, String key, boolean isJoining) {
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, key, "player", nickname,
                "channel", Lang.getFor(player, VoiceChannel.DOMINION.getLangKey()))));
        playJingle(player, isJoining);
    }

    private static VoiceMember getMember(UUID uuid) {
        VoiceMember member = localMembers.get(uuid);
        return member != null ? member : remoteMembers.get(uuid);
    }

    private static String getLastKnownNickname(UUID uuid) {
        String nickname = lastKnownNicknames.get(uuid);
        return nickname != null ? nickname : AranarthUtils.getNickname(Bukkit.getOfflinePlayer(uuid));
    }

    /**
     * Plays the voice call jingle: a rising chime when joining, a falling chime when leaving.
     */
    public static void playJingle(Player player, boolean isJoining) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        int vol = aranarthPlayer == null ? 100 : aranarthPlayer.getVoiceChatSoundVolume();
        if (vol <= 0) {
            return;
        }
        float volume = vol / 100f;
        float firstPitch = isJoining ? 1.0F : 1.5F;
        float secondPitch = isJoining ? 1.5F : 1.0F;
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, volume, firstPitch);
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (player.isOnline()) {
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, volume, secondPitch);
            }
        }, 4L);
    }
}
