package com.aearost.aranarthcore.network;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatGameUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.ItemUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages cross-server communication for the Aranarth network via MySQL polling.
 * Only active when is-public-server=true in config. All methods are safe to call
 * regardless; callers should check {@link #isActive()} if they want to skip the call
 * entirely, but a null instance check is not required.
 */
public class NetworkManager {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static NetworkManager instance;

    public static NetworkManager getInstance() { return instance; }
    public static boolean isActive() { return instance != null; }

    /**
     * Initializes NetworkManager backed by the already-connected DatabaseManager.
     */
    public static void initialize(String thisServer) {
        if (instance != null) {
            instance.doShutdown();
        }
        try {
            instance = new NetworkManager(thisServer);
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "NetworkManager initialized (MySQL polling) for server: " + thisServer);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, AranarthCore.LOG_PREFIX + "Failed to initialize NetworkManager: " + e.getMessage(), e);
            instance = null;
        }
    }

    public static void shutdown() {
        if (instance != null) {
            instance.doShutdown();
            instance = null;
        }
    }

    // -------------------------------------------------------------------------
    // Channel names  (kept identical for compatibility)
    // -------------------------------------------------------------------------

    public static final String CH_CHAT        = "aranarth:chat";
    public static final String CH_JOIN        = "aranarth:join";
    public static final String CH_JOIN_MSG    = "aranarth:join_msg";
    public static final String CH_QUIT        = "aranarth:quit";
    public static final String CH_TP_REQUEST  = "aranarth:tp_request";
    public static final String CH_TP_ACCEPT   = "aranarth:tp_accept";
    public static final String CH_TP_DENY     = "aranarth:tp_deny";
    public static final String CH_TRANSFER    = "aranarth:transfer";
    public static final String CH_SYNC_TIME    = "aranarth:sync_time";
    public static final String CH_SYNC_WEATHER = "aranarth:sync_weather";
    public static final String CH_DM           = "aranarth:dm";
    public static final String CH_SLEEP        = "aranarth:sleep";
    public static final String CH_AFK          = "aranarth:afk";
    public static final String CH_BROADCAST    = "aranarth:broadcast";
    public static final String CH_BOOST_SYNC   = "aranarth:boost_sync";
    public static final String CH_SOUND_ALL    = "aranarth:sound_all";
    public static final String CH_MAIL_NOTIFY       = "aranarth:mail_notify";
    public static final String CH_COUNCIL_MSG       = "aranarth:council_msg";
    public static final String CH_DOMINION_DISBAND  = "aranarth:dominion_disband";
    public static final String CH_DOMINION_CREATE   = "aranarth:dominion_create";
    public static final String CH_CHAT_GAME_START   = "aranarth:chat_game_start";
    public static final String CH_CHAT_GAME_WIN     = "aranarth:chat_game_win";
    public static final String CH_CHAT_GAME_EXPIRE  = "aranarth:chat_game_expire";

    // Temp-data key prefixes
    private static final String KEY_PENDING_TP = "pending_tp:";
    private static final String KEY_RETURN_LOC = "return_loc:";
    private static final String KEY_LAST_MSG   = "last_msg:";

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final String thisServer;
    private final DatabaseManager db;
    private final Gson gson = new Gson();
    private long lastProcessedMessageId;
    private BukkitTask pollingTask;
    private BukkitTask cleanupTask;

    /** Players currently online on OTHER servers. */
    private final Map<UUID, NetworkPlayer> remoteRoster = new ConcurrentHashMap<>();

    /**
     * Players that are currently mid-transfer to another server.
     * Used to suppress the quit message/sound on the outgoing server.
     */
    private final Set<UUID> transferringPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Players whose cross-server quit should suppress DiscordSRV's leave announcement.
     * Populated in PlayerServerQuitListener, consumed in DiscordChatListener.
     */
    private final Set<UUID> crossServerQuitPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Players whose cross-server join should suppress DiscordSRV's join announcement.
     * Populated in PlayerServerJoinListener, consumed in DiscordChatListener.
     */
    private final Set<UUID> crossServerJoinPlayers = ConcurrentHashMap.newKeySet();

    /** Pending cross-server TP requests received from another server. */
    private final Map<UUID, CrossServerTpContext> pendingCrossServerRequests = new ConcurrentHashMap<>();

    /**
     * Cross-server /back locations for players who arrived from another server.
     * Format: "serverKey|world|x|y|z|yaw|pitch"
     * Populated by loadAndApplyCrossServerBack(); consumed by CommandBack.
     */
    private final Map<UUID, String> crossServerBackLocations = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Constructor / lifecycle
    // -------------------------------------------------------------------------

    private NetworkManager(String thisServer) {
        this.thisServer = thisServer;
        this.db = DatabaseManager.getInstance();

        // Snapshot the current max message id so we don't replay old history
        this.lastProcessedMessageId = db.getMaxMessageId();

        startPolling();
        startCleanup();
    }

    private void startPolling() {
        // Poll every 10 ticks (500ms) on an async thread
        pollingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                AranarthCore.getInstance(),
                this::pollAndDispatch,
                20L, // initial delay 1s
                10L  // every 10 ticks = 500ms
        );
    }

    private void startCleanup() {
        // Cleanup old messages every 5 minutes (6000 ticks) and reconcile the remote roster to
        // remove stale entries left by a crashed remote server (no CH_QUIT was ever sent).
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                AranarthCore.getInstance(),
                () -> {
                    db.cleanupMessages();
                    db.cleanupTempData();
                    reconcileRemoteRoster();
                },
                6000L,
                6000L
        );
    }

    /**
     * Re-reads the DB roster and removes any in-memory remote-roster entries that are no longer
     * present in the database. This corrects stale entries caused by a remote server crashing
     * (which prevented CH_QUIT messages from being published).
     * Must be called from an async thread.
     */
    private void reconcileRemoteRoster() {
        try {
            Map<UUID, NetworkPlayer> dbRoster = db.loadRemoteRoster(thisServer);
            List<UUID> stale = new ArrayList<>();
            for (UUID uuid : remoteRoster.keySet()) {
                if (!dbRoster.containsKey(uuid)) {
                    stale.add(uuid);
                }
            }
            if (!stale.isEmpty()) {
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                    for (UUID uuid : stale) {
                        remoteRoster.remove(uuid);
                        NetworkTabManager.removeFromTab(uuid);
                    }
                    AranarthUtils.updateTab();
                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX
                            + "Reconciled " + stale.size() + " stale remote roster entr"
                            + (stale.size() == 1 ? "y" : "ies"));
                });
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Roster reconciliation failed: " + e.getMessage());
        }
    }

    private void pollAndDispatch() {
        try {
            List<DatabaseManager.MessageRow> rows = db.pollMessages(lastProcessedMessageId, thisServer);
            for (DatabaseManager.MessageRow row : rows) {
                lastProcessedMessageId = Math.max(lastProcessedMessageId, row.id);
                final String channel = row.channel;
                final String payload = row.payload;
                // Jump to main thread for all Bukkit API interactions
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                    try {
                        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                        dispatch(channel, json);
                    } catch (Exception e) {
                        Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                                + "Bad DB message on " + channel + ": " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Polling error: " + e.getMessage());
        }
    }

    private void doShutdown() {
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        // Clear this server's roster entries from the DB so stale entries don't appear on other servers
        try {
            db.clearRosterForServer(thisServer);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to clear roster on shutdown: " + e.getMessage());
        }
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "NetworkManager shut down");
    }

    // -------------------------------------------------------------------------
    // Subscriber dispatch
    // -------------------------------------------------------------------------

    private void dispatch(String channel, JsonObject json) {
        switch (channel) {
            case CH_CHAT         -> handleChat(json);
            case CH_JOIN         -> handleJoin(json);
            case CH_JOIN_MSG     -> handleJoinMsg(json);
            case CH_QUIT         -> handleQuit(json);
            case CH_TP_REQUEST   -> handleTpRequest(json);
            case CH_TP_ACCEPT    -> handleTpAccept(json);
            case CH_TP_DENY      -> handleTpDeny(json);
            case CH_TRANSFER     -> handleTransfer(json);
            case CH_SYNC_TIME    -> handleSyncTime(json);
            case CH_SYNC_WEATHER -> handleSyncWeather(json);
            case CH_DM            -> handleDirectMessage(json);
            case CH_SLEEP         -> handleSleepMessage(json);
            case CH_AFK           -> handleAfkStatus(json);
            case CH_BROADCAST     -> handleBroadcast(json);
            case CH_BOOST_SYNC    -> handleBoostSync(json);
            case CH_SOUND_ALL     -> handleSoundAll(json);
            case CH_MAIL_NOTIFY      -> handleMailNotification(json);
            case CH_COUNCIL_MSG      -> handleCouncilMessage(json);
            case CH_DOMINION_DISBAND -> handleDominionDisband(json);
            case CH_DOMINION_CREATE  -> handleDominionCreate(json);
            case CH_CHAT_GAME_START  -> handleChatGameStart(json);
            case CH_CHAT_GAME_WIN    -> handleChatGameWin(json);
            case CH_CHAT_GAME_EXPIRE -> handleChatGameExpire(json);
        }
    }

    // -------------------------------------------------------------------------
    // Publishers
    // -------------------------------------------------------------------------

    /** Publishes a public chat message so the other server can relay it to its players. */
    public void publishChat(String prefix, String chatMessage) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("prefix", prefix);
        json.addProperty("message", chatMessage);
        publish(CH_CHAT, json);
    }

    /** Called from PlayerServerJoinListener after the player's AranarthPlayer is ready. */
    public void publishPlayerJoin(UUID uuid, AranarthPlayer ap) {
        String nickname = ap.getNickname().isEmpty() ? ap.getUsername() : ap.getNickname();

        // Update roster in DB
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
            db.upsertRosterEntry(uuid, ap.getUsername(), nickname, thisServer,
                    ap.getRank(), ap.getCouncilRank(), ap.getSaintRank(), ap.getArchitectRank(), ap.isVanished())
        );

        // Extract skin texture so other servers can render this player's head in the tab list
        String textureValue = "", textureSignature = "";
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            String[] tex = NetworkTabManager.extractPlayerTexture(onlinePlayer);
            textureValue = tex[0];
            textureSignature = tex[1];
            if (textureValue.isEmpty()) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Net] publishPlayerJoin: texture extraction returned empty for " + ap.getUsername() + " — remote tab may show default skin");
            }
        }

        // Publish event
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("username", ap.getUsername());
        json.addProperty("nickname", nickname);
        json.addProperty("server", thisServer);
        json.addProperty("rank", ap.getRank());
        json.addProperty("councilRank", ap.getCouncilRank());
        json.addProperty("saintRank", ap.getSaintRank());
        json.addProperty("architectRank", ap.getArchitectRank());
        json.addProperty("vanished", ap.isVanished());
        json.addProperty("textureValue", textureValue);
        json.addProperty("textureSignature", textureSignature);
        publish(CH_JOIN, json);
    }

    /**
     * Called after the join message is determined (non-transfer joins only).
     * Notifies other servers to display the join message and play the join sound.
     * @param isNewPlayer true if this is the player's first ever join (plays challenge-complete sound instead of xylophone).
     */
    public void publishJoinMsg(String joinMessage, boolean isNewPlayer) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("joinMessage", joinMessage != null ? joinMessage : "");
        json.addProperty("isNewPlayer", isNewPlayer);
        publish(CH_JOIN_MSG, json);
    }

    /**
     * Called from PlayerServerQuitListener.
     * @param uuid          The UUID of the player who disconnected.
     * @param quitMessage   The formatted quit message to broadcast, or null for cross-server transfers.
     * @param isVanished    Whether the player was vanished (suppresses the public message).
     */
    public void publishPlayerQuit(UUID uuid, String quitMessage, boolean isVanished) {
        // Remove from roster in DB
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
            db.removeRosterEntry(uuid)
        );

        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("server", thisServer);
        json.addProperty("quitMessage", quitMessage != null ? quitMessage : "");
        json.addProperty("vanished", isVanished);
        publish(CH_QUIT, json);
    }

    /**
     * Sends a cross-server /tp or /tphere request to another server where the target player lives.
     */
    public void publishTpRequest(UUID fromUuid, String fromNickname, UUID toUuid, boolean isTpHere) {
        JsonObject json = new JsonObject();
        json.addProperty("fromUuid", fromUuid.toString());
        json.addProperty("fromNickname", fromNickname);
        json.addProperty("fromServer", thisServer);
        json.addProperty("toUuid", toUuid.toString());
        json.addProperty("isTpHere", isTpHere);
        publish(CH_TP_REQUEST, json);
    }

    /**
     * Publishes that the local player accepted a cross-server TP request.
     */
    public void publishTpAccepted(UUID accepterUuid, String accepterNickname,
                                  UUID requesterUuid, boolean isTpHere) {
        JsonObject json = new JsonObject();
        json.addProperty("accepterUuid", accepterUuid.toString());
        json.addProperty("accepterNickname", accepterNickname);
        json.addProperty("accepterServer", thisServer);
        json.addProperty("requesterUuid", requesterUuid.toString());
        json.addProperty("isTpHere", isTpHere);
        publish(CH_TP_ACCEPT, json);
    }

    /**
     * Publishes that the local player denied a cross-server TP request.
     */
    public void publishTpDenied(UUID denierUuid, String denierNickname, UUID requesterUuid) {
        JsonObject json = new JsonObject();
        json.addProperty("denierUuid", denierUuid.toString());
        json.addProperty("denierNickname", denierNickname);
        json.addProperty("requesterUuid", requesterUuid.toString());
        publish(CH_TP_DENY, json);
    }

    /**
     * Broadcasts a time change to all other servers so their sync worlds stay in lockstep.
     * @param time The new world time in ticks.
     */
    public void publishSyncTime(long time) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("time", time);
        publish(CH_SYNC_TIME, json);
    }

    /**
     * Sends a private message from a player on this server to a player on another server.
     */
    public void publishDirectMessage(UUID fromUuid, String fromNickname, UUID toUuid, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("fromUuid", fromUuid.toString());
        json.addProperty("fromNickname", fromNickname);
        json.addProperty("fromServer", thisServer);
        json.addProperty("toUuid", toUuid.toString());
        json.addProperty("message", message);
        publish(CH_DM, json);
    }

    /**
     * Publishes the current sleep status action-bar message to all other servers.
     * @param message  The formatted message, e.g. "Players sleeping: 1/2".
     * @param sleeping Number of players currently sleeping.
     * @param required Number required to skip the night.
     */
    public void publishSleepMessage(String message, int sleeping, int required) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("message", message);
        json.addProperty("sleeping", sleeping);
        json.addProperty("required", required);
        publish(CH_SLEEP, json);
    }

    /** Returns the number of remote players that should count toward the sleep threshold. */
    public int getRemoteSleepEligibleCount() {
        // All remote players count — they are on survival/SMP gameplay servers
        return remoteRoster.size();
    }

    /** Number of players currently sleeping on other servers. Updated by handleSleepMessage. */
    private volatile int remoteSleepingCount = 0;

    /** Callback invoked on the main thread whenever the remote sleeping count changes. */
    private Runnable remoteSleepCallback = null;

    public void setRemoteSleepCallback(Runnable callback) {
        this.remoteSleepCallback = callback;
    }

    /** Returns the number of players currently sleeping on other servers. */
    public int getRemoteSleepingCount() {
        return remoteSleepingCount;
    }

    public void publishSyncWeather(String weatherType, int duration, boolean isThunder, int stormDuration, int stormDelay) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("weatherType", weatherType);
        json.addProperty("duration", duration);
        json.addProperty("isThunder", isThunder);
        json.addProperty("stormDuration", stormDuration);
        json.addProperty("stormDelay", stormDelay);
        publish(CH_SYNC_WEATHER, json);
    }

    /**
     * Relays a server-wide broadcast (from Bukkit.broadcastMessage) to all other servers.
     * The message should already contain translated § color codes.
     */
    public void publishBroadcast(String rawMessage) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("message", rawMessage);
        publish(CH_BROADCAST, json);
    }

    /**
     * Broadcasts an AFK status change for a local player to all other servers.
     */
    public void publishAfkStatus(UUID uuid, String nickname, boolean isAfk) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("nickname", nickname);
        json.addProperty("afk", isAfk);
        publish(CH_AFK, json);
    }

    /**
     * Plays a sound for all players on every other server in the network.
     */
    public void publishSoundAll(String soundKey, float volume, float pitch) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("sound", soundKey);
        json.addProperty("volume", volume);
        json.addProperty("pitch", pitch);
        publish(CH_SOUND_ALL, json);
    }

    /**
     * Syncs a boost add or removal to all other servers so they stay in sync without posting
     * a second Discord message or in-game broadcast.
     * @param boostName  Boost enum name (e.g. "MINER").
     * @param endTimeStr ISO-8601 LocalDateTime string for the new end time, or "" when removing.
     * @param removing   true = remove the boost, false = add/update it.
     */
    public void publishBoostSync(String boostName, String endTimeStr, boolean removing) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("boost", boostName);
        json.addProperty("endTime", endTimeStr);
        json.addProperty("removing", removing);
        publish(CH_BOOST_SYNC, json);
    }

    /**
     * Forces a remotely-online player to transfer to the specified server.
     * Use alongside {@link #setPendingTeleport(UUID, PendingTeleport)} when the pending TP
     * must be saved to the DB before the transfer message is received.
     */
    public void publishTransfer(UUID uuid, String targetServer) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("targetServer", targetServer);
        publish(CH_TRANSFER, json);
    }

    /**
     * Relays a council (/ac msg) message to the other server so council members there see it.
     * @param formattedMessage The fully colour-translated message string (already includes prefix).
     */
    public void publishCouncilMessage(String formattedMessage) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("message", formattedMessage);
        publish(CH_COUNCIL_MSG, json);
    }

    /**
     * Notifies a player on another server that they have received a new mail message.
     */
    public void publishMailNotification(UUID toUuid, String fromNickname) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("toUuid", toUuid.toString());
        json.addProperty("fromNickname", fromNickname);
        publish(CH_MAIL_NOTIFY, json);
    }

    // -------------------------------------------------------------------------
    // Pending teleport queue (MySQL-backed, survives reconnects)
    // -------------------------------------------------------------------------

    /** Stores a pending teleport for {@code uuid} to be executed when they arrive on this server. TTL 5 minutes. */
    public void setPendingTeleport(UUID uuid, PendingTeleport pending) {
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                db.saveTempData(KEY_PENDING_TP + uuid, gson.toJson(pending), 300);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "DB: failed to set pending TP for " + uuid);
            }
        });
    }

    /** Returns the pending teleport for {@code uuid}, or null if none. Must be called synchronously. */
    public PendingTeleport getPendingTeleport(UUID uuid) {
        try {
            String data = db.loadTempData(KEY_PENDING_TP + uuid);
            if (data == null) return null;
            return gson.fromJson(data, PendingTeleport.class);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "DB: failed to get pending TP for " + uuid);
            return null;
        }
    }

    /** Removes the pending teleport for {@code uuid}. */
    public void clearPendingTeleport(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                db.deleteTempData(KEY_PENDING_TP + uuid);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "DB: failed to clear pending TP for " + uuid);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Return location (saved when player transfers to SMP, restored on /survival)
    // -------------------------------------------------------------------------

    /** Saves the player's current survival location before they transfer to SMP. TTL 1 hour. */
    public void saveReturnLocation(UUID uuid, Location loc) {
        JsonObject json = new JsonObject();
        json.addProperty("world", loc.getWorld().getName());
        json.addProperty("x", loc.getX());
        json.addProperty("y", loc.getY());
        json.addProperty("z", loc.getZ());
        json.addProperty("yaw", (double) loc.getYaw());
        json.addProperty("pitch", (double) loc.getPitch());
        final String jsonStr = json.toString();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                db.saveTempData(KEY_RETURN_LOC + uuid, jsonStr, 3600);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "DB: failed to save return location for " + uuid);
            }
        });
    }

    /**
     * Retrieves and removes the saved return location. Returns null if none stored
     * or if the world no longer exists. Must be called synchronously.
     */
    public Location getAndClearReturnLocation(UUID uuid) {
        try {
            String data = db.loadTempData(KEY_RETURN_LOC + uuid);
            if (data == null) return null;
            db.deleteTempData(KEY_RETURN_LOC + uuid);
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            World world = Bukkit.getWorld(json.get("world").getAsString());
            if (world == null) return null;
            return new Location(world,
                    json.get("x").getAsDouble(),
                    json.get("y").getAsDouble(),
                    json.get("z").getAsDouble(),
                    (float) json.get("yaw").getAsDouble(),
                    (float) json.get("pitch").getAsDouble());
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "DB: failed to get return location for " + uuid);
            return null;
        }
    }

    /**
     * Serializes the player's current location (with server-key prefix) for /back storage.
     * Returns null if location or world is unavailable.
     */
    private String buildBackLocationJson(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("world", loc.getWorld().getName());
        json.addProperty("x", loc.getX());
        json.addProperty("y", loc.getY());
        json.addProperty("z", loc.getZ());
        json.addProperty("yaw", (double) loc.getYaw());
        json.addProperty("pitch", (double) loc.getPitch());
        return json.toString();
    }

    /**
     * Reads the stored lastReceivedMessage UUID from DB on cross-server arrival and applies it.
     * Must be called on the main thread.
     */
    public void loadAndApplyCrossServerLastMsg(UUID uuid) {
        if (!DatabaseManager.isActive()) return;
        try {
            String data = db.loadTempData(KEY_LAST_MSG + uuid);
            if (data == null || data.isEmpty()) return;
            db.deleteTempData(KEY_LAST_MSG + uuid);
            UUID lastMsg = UUID.fromString(data);
            AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
            if (ap != null) {
                ap.setLastReceivedMessage(lastMsg);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to apply last-msg for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Called on cross-server arrival (main thread). Reads the stored /back location from the DB,
     * then either:
     * - Sets it as the player's lastKnownTeleportLocation (same-server world), or
     * - Stores it in crossServerBackLocations (different server) for CommandBack to use.
     */
    public void loadAndApplyCrossServerBack(UUID uuid) {
        if (!DatabaseManager.isActive()) return;
        try {
            String data = db.loadTempData(KEY_RETURN_LOC + uuid);
            if (data == null) return;
            db.deleteTempData(KEY_RETURN_LOC + uuid);
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            String server = json.has("server") ? json.get("server").getAsString() : thisServer;
            String worldName = json.get("world").getAsString();
            double x = json.get("x").getAsDouble();
            double y = json.get("y").getAsDouble();
            double z = json.get("z").getAsDouble();
            float yaw = (float) json.get("yaw").getAsDouble();
            float pitch = (float) json.get("pitch").getAsDouble();

            if (server.equals(thisServer)) {
                // Back location is on this server
                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
                    if (ap != null) ap.setLastKnownTeleportLocation(new Location(w, x, y, z, yaw, pitch));
                }
            } else {
                // Back location is on a different server — store for CommandBack to route cross-server.
                // Also clear the local lastKnownTeleportLocation (set by the pending-TP spawn teleport)
                // so /back falls through to the cross-server routing instead of going to the spawn point.
                crossServerBackLocations.put(uuid, server + "|" + worldName + "|" + x + "|" + y + "|" + z + "|" + yaw + "|" + pitch);
                AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
                if (ap != null) ap.setLastKnownTeleportLocation(null);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to apply back location for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Returns and removes the stored cross-server /back location for the player, or null if none.
     * Format: "serverKey|world|x|y|z|yaw|pitch"
     */
    public String consumeCrossServerBack(UUID uuid) {
        return crossServerBackLocations.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Server transfer (BungeeCord plugin messaging)
    // -------------------------------------------------------------------------

    /**
     * Marks a player as being transferred to another server so the quit listener
     * can suppress the goodbye message and sound.
     */
    public void markTransferring(UUID uuid) {
        transferringPlayers.add(uuid);
    }

    /**
     * Returns true and removes the flag if the player was mid-transfer, false otherwise.
     */
    public boolean consumeTransferring(UUID uuid) {
        return transferringPlayers.remove(uuid);
    }

    /**
     * Returns true if the player is currently mid-transfer, without consuming the flag.
     * Use {@link #consumeTransferring(UUID)} to both check and clear the flag.
     */
    public boolean isTransferring(UUID uuid) {
        return transferringPlayers.contains(uuid);
    }

    /** Marks this player's quit as a cross-server transfer for DiscordSRV suppression. */
    public void markCrossServerQuit(UUID uuid) {
        crossServerQuitPlayers.add(uuid);
    }

    /** Returns true and removes if the player's quit was a cross-server transfer. */
    public boolean consumeCrossServerQuit(UUID uuid) {
        return crossServerQuitPlayers.remove(uuid);
    }

    /** Marks this player's join as a cross-server transfer arrival for DiscordSRV suppression. */
    public void markCrossServerJoin(UUID uuid) {
        crossServerJoinPlayers.add(uuid);
    }

    /** Returns true and removes if the player's join was a cross-server transfer arrival. */
    public boolean consumeCrossServerJoin(UUID uuid) {
        return crossServerJoinPlayers.remove(uuid);
    }

    /**
     * Saves the player's survival inventory and ender chest to their AranarthPlayer, persists
     * both the player data and the pending teleport to MySQL, then transfers the player to the
     * target server only once both writes have completed.
     *
     * <p>This eliminates the race condition where the BungeeCord transfer message was sent before
     * the async MySQL writes finished, causing stale inventory data to be loaded on arrival.</p>
     *
     * <p>{@code pending.setApplyInventory(true)} is set automatically.</p>
     */
    public void saveInventoryAndTransfer(Player player, String targetServer, PendingTeleport pending) {
        UUID uuid = player.getUniqueId();

        // Serialize inventory and ender chest into AranarthPlayer. handleTeleportLogic() already
        // does this for teleports that go through the normal countdown, but callers like
        // handleTransfer/handleTpAccept bypass that path, so we always do it here as a fallback.
        String transferFromWorld = player.getWorld() != null ? player.getWorld().getName() : "";
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap != null) {
            if (AranarthUtils.isSurvivalWorld(transferFromWorld)) {
                try {
                    ap.setSurvivalInventory(ItemUtils.itemStackArrayToBase64(player.getInventory().getContents()));
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to serialize inventory for " + player.getName() + ": " + e.getMessage());
                }
                try {
                    ap.setSurvivalEnderChest(ItemUtils.itemStackArrayToBase64(player.getEnderChest().getContents()));
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to serialize ender chest for " + player.getName() + ": " + e.getMessage());
                }
                ap.setSurvivalHealth(player.getHealth());
                ap.setSurvivalFoodLevel(player.getFoodLevel());
                ap.setSurvivalSaturation(player.getSaturation());
                ap.setSurvivalExpLevel(player.getLevel());
                ap.setSurvivalExpProgress(player.getExp());
            } else if (transferFromWorld.startsWith("creative")) {
                // Save creative inventory so it persists across servers
                try {
                    ap.setCreativeInventory(ItemUtils.itemStackArrayToBase64(player.getInventory().getContents()));
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to serialize creative inventory for " + player.getName() + ": " + e.getMessage());
                }
            } else if (transferFromWorld.startsWith("arena")) {
                // Save arena inventory so it persists across servers
                try {
                    ap.setArenaInventory(ItemUtils.itemStackArrayToBase64(player.getInventory().getContents()));
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to serialize arena inventory for " + player.getName() + ": " + e.getMessage());
                }
            } else {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv]   → WARNING: world '" + transferFromWorld
                        + "' matched no inventory branch for " + player.getName() + " — inventory NOT saved before transfer");
            }
            AranarthUtils.setPlayer(uuid, ap);
        }

        pending.setApplyInventory(true);

        // Build the serialized forms now, on the main thread, so the async task only does I/O.
        final String rawRow = PersistenceUtils.buildPlayerRowForTransfer(uuid);
        final String toggleJson = PersistenceUtils.buildPlayerToggleJson(uuid);
        final String pendingJson = gson.toJson(pending);

        // Capture the player's current location as the /back destination on the destination server.
        final Location backLoc = player.getLocation();
        final String backJson = buildBackLocationJson(backLoc);

        // Capture the lastReceivedMessage UUID so /r works after arriving on the new server.
        AranarthPlayer apForMsg = AranarthUtils.getPlayer(uuid);
        final UUID lastMsgUuid = apForMsg != null ? apForMsg.getLastReceivedMessage() : null;

        // NOTE: transferringPlayers is NOT set here. It is set only just before sendPluginMessage
        // so that a crash or disconnect during the async DB write does NOT suppress the quit
        // message — the server still needs to broadcast the player's departure in that case.

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            boolean dbWriteSucceeded = true;
            try {
                if (rawRow != null && DatabaseManager.isActive()) {
                    DatabaseManager.getInstance().saveAranarthPlayerRaw(uuid, rawRow);
                }
                if (toggleJson != null && DatabaseManager.isActive()) {
                    DatabaseManager.getInstance().savePlayerToggles(uuid, toggleJson);
                }
                db.saveTempData(KEY_PENDING_TP + uuid, pendingJson, 300);
                if (backJson != null) {
                    db.saveTempData(KEY_RETURN_LOC + uuid, backJson, 3600);
                }
                if (lastMsgUuid != null) {
                    db.saveTempData(KEY_LAST_MSG + uuid, lastMsgUuid.toString(), 300);
                }
            } catch (Exception e) {
                dbWriteSucceeded = false;
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                        + "DB write before transfer failed for " + player.getName() + ": " + e.getMessage());
            }
            final boolean transferAllowed = dbWriteSucceeded;
            // Send the BungeeCord Connect message on the main thread after both writes complete.
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (!player.isOnline()) {
                    // Player disconnected during the async write — quit message was not suppressed
                    // (flag was never set), so no cleanup needed here.
                    return;
                }
                if (!transferAllowed) {
                    // The pending-TP write failed: transferring now would leave the player on the
                    // destination with no inventory state. Abort and notify the player instead.
                    player.sendMessage(ChatUtils.chatMessage(
                            "&cTransfer failed due to a database error. Please try again."));
                    return;
                }
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(bos);
                    dos.writeUTF("Connect");
                    dos.writeUTF(targetServer);
                    // Mark as transferring immediately before sending so that the imminent
                    // Velocity-triggered quit is treated as a server switch, not a real disconnect.
                    transferringPlayers.add(uuid);
                    player.sendPluginMessage(AranarthCore.getInstance(), "BungeeCord", bos.toByteArray());
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                            + "Failed to transfer " + player.getName() + " to " + targetServer + ": " + e.getMessage());
                    transferringPlayers.remove(uuid);
                }
            });
        });
    }

    /**
     * Saves a pending teleport to MySQL then transfers the player to the target server.
     * The BungeeCord Connect message is sent only after the DB write confirms, preventing
     * a race where the player arrives before their pending teleport is readable.
     */
    public void setPendingAndTransfer(Player player, String targetServer, PendingTeleport pending) {
        UUID uuid = player.getUniqueId();
        final String pendingJson = gson.toJson(pending);
        // NOTE: transferringPlayers is NOT set here — see saveInventoryAndTransfer for rationale.

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            boolean dbWriteSucceeded = true;
            try {
                db.saveTempData(KEY_PENDING_TP + uuid, pendingJson, 300);
            } catch (Exception e) {
                dbWriteSucceeded = false;
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                        + "DB write before transfer failed for " + player.getName() + ": " + e.getMessage());
            }
            final boolean transferAllowed = dbWriteSucceeded;
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (!transferAllowed) {
                    player.sendMessage(ChatUtils.chatMessage(
                            "&cTransfer failed due to a database error. Please try again."));
                    return;
                }
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(bos);
                    dos.writeUTF("Connect");
                    dos.writeUTF(targetServer);
                    transferringPlayers.add(uuid);
                    player.sendPluginMessage(AranarthCore.getInstance(), "BungeeCord", bos.toByteArray());
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                            + "Failed to transfer " + player.getName() + " to " + targetServer + ": " + e.getMessage());
                    transferringPlayers.remove(uuid);
                }
            });
        });
    }

    /**
     * Instructs the proxy to move {@code player} to {@code targetServer}.
     */
    public void transferPlayer(Player player, String targetServer) {
        transferringPlayers.add(player.getUniqueId());
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF("Connect");
            dos.writeUTF(targetServer);
            player.sendPluginMessage(AranarthCore.getInstance(), "BungeeCord", bos.toByteArray());
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                    + "Failed to transfer " + player.getName() + " to " + targetServer + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Cross-server TP context (in-memory)
    // -------------------------------------------------------------------------

    public void storeCrossServerTpContext(UUID localPlayerUuid, CrossServerTpContext ctx) {
        pendingCrossServerRequests.put(localPlayerUuid, ctx);
    }

    public CrossServerTpContext getCrossServerTpContext(UUID localPlayerUuid) {
        return pendingCrossServerRequests.get(localPlayerUuid);
    }

    public void clearCrossServerTpContext(UUID localPlayerUuid) {
        pendingCrossServerRequests.remove(localPlayerUuid);
    }

    // -------------------------------------------------------------------------
    // Remote roster access
    // -------------------------------------------------------------------------

    /** Returns all players currently on OTHER servers. */
    public Map<UUID, NetworkPlayer> getRemoteRoster() {
        return Collections.unmodifiableMap(remoteRoster);
    }

    public NetworkPlayer getRemotePlayer(UUID uuid) {
        return remoteRoster.get(uuid);
    }

    public String getThisServer() {
        return thisServer;
    }

    /**
     * Re-populates the remote roster from MySQL on startup, in case another server was already running.
     */
    public void syncRosterFromDatabase() {
        try {
            // Clear any stale entries this server may have left in a previous crash
            db.clearRosterForServer(thisServer);
            Map<UUID, NetworkPlayer> loaded = db.loadRemoteRoster(thisServer);
            remoteRoster.putAll(loaded);
            if (!remoteRoster.isEmpty()) {
                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX
                        + "Synced " + remoteRoster.size() + " remote player(s) from MySQL");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to sync roster from MySQL: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Handlers (run on main thread)
    // -------------------------------------------------------------------------

    private void handleChat(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String prefix  = json.get("prefix").getAsString();
        String message = json.get("message").getAsString();

        String formatted = ChatUtils.translateToColor(prefix + message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage(ChatUtils.translateToColor(
                "&8[" + originServer.toUpperCase() + "] " + prefix + message));
    }

    private void handleJoin(JsonObject json) {
        String server = json.get("server").getAsString();
        if (server.equals(thisServer)) return;

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        boolean vanished = json.get("vanished").getAsBoolean();
        String username = json.has("username") ? json.get("username").getAsString() : "";
        String textureValue = json.has("textureValue") ? json.get("textureValue").getAsString() : "";
        String textureSignature = json.has("textureSignature") ? json.get("textureSignature").getAsString() : "";
        NetworkPlayer np = new NetworkPlayer(
                uuid,
                username,
                json.get("nickname").getAsString(),
                server,
                json.get("rank").getAsInt(),
                json.get("councilRank").getAsInt(),
                json.get("saintRank").getAsInt(),
                json.get("architectRank").getAsInt(),
                vanished,
                textureValue,
                textureSignature
        );
        remoteRoster.put(uuid, np);
        if (!vanished) {
            NetworkTabManager.addToTab(np);
        }
        AranarthUtils.updateTab();
    }

    private void handleJoinMsg(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String joinMessage = json.has("joinMessage") ? json.get("joinMessage").getAsString() : "";
        if (joinMessage.isEmpty()) {
            return;
        }

        Bukkit.getConsoleSender().sendMessage(joinMessage);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(joinMessage);
        }
        boolean isNewPlayer = json.has("isNewPlayer") && json.get("isNewPlayer").getAsBoolean();
        if (isNewPlayer) {
            // New player first join — play the challenge-complete fanfare
            for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1F, 0.8F);
        } else {
            // Regular join — play the ascending note-block xylophone
            new BukkitRunnable() {
                int runs = 0;
                @Override
                public void run() {
                    if (runs == 0) {
                        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1F);
                        runs++;
                    } else if (runs == 1) {
                        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.2F);
                        runs++;
                    } else {
                        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.6F);
                        cancel();
                    }
                }
            }.runTaskTimer(AranarthCore.getInstance(), 0, 5);
        }
    }

    private void handleQuit(JsonObject json) {
        String server = json.get("server").getAsString();
        if (server.equals(thisServer)) return;

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        remoteRoster.remove(uuid);
        // Guard: if the player just transferred to THIS server, their vanilla tab entry is
        // already present — removing it would blank their skin and hide them from their own tab.
        if (Bukkit.getPlayer(uuid) == null) {
            NetworkTabManager.removeFromTab(uuid);
        }
        AranarthUtils.updateTab();

        // Broadcast quit message and play sound if this was a real disconnect (not a server transfer)
        String quitMessage = json.has("quitMessage") ? json.get("quitMessage").getAsString() : "";
        boolean vanished = json.has("vanished") && json.get("vanished").getAsBoolean();
        if (!quitMessage.isEmpty() && !vanished) {
            Bukkit.getConsoleSender().sendMessage(quitMessage);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(quitMessage);
            }
            // Play the descending note-block quit sound
            new BukkitRunnable() {
                int runs = 0;
                @Override
                public void run() {
                    if (runs == 0) {
                        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.6F);
                        runs++;
                    } else if (runs == 1) {
                        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 1.2F);
                        runs++;
                    } else {
                        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1F, 0.8F);
                        cancel();
                    }
                }
            }.runTaskTimer(AranarthCore.getInstance(), 0, 5);
        }
    }

    private void handleTpRequest(JsonObject json) {
        UUID fromUuid       = UUID.fromString(json.get("fromUuid").getAsString());
        String fromNickname = json.get("fromNickname").getAsString();
        String fromServer   = json.get("fromServer").getAsString();
        UUID toUuid         = UUID.fromString(json.get("toUuid").getAsString());
        boolean isTpHere    = json.get("isTpHere").getAsBoolean();

        if (fromServer.equals(thisServer)) return;

        Player target = Bukkit.getPlayer(toUuid);
        if (target == null) return;

        AranarthPlayer targetAp = AranarthUtils.getPlayer(toUuid);
        if (targetAp.isTogglingTp()) {
            publishTpDenied(toUuid, targetAp.getNickname().isEmpty() ? target.getName() : targetAp.getNickname(), fromUuid);
            return;
        }

        storeCrossServerTpContext(toUuid, new CrossServerTpContext(fromUuid, fromNickname, fromServer, isTpHere));

        // Do NOT set teleportToUuid/teleportFromUuid here — those fields resolve the UUID via
        // Bukkit.getPlayer(), which only works for locally-online players. The remote requester is
        // on a different server, so that lookup returns null and /tpaccept would falsely report
        // "player is no longer online". The CrossServerTpContext above is the sole routing
        // mechanism; /tpaccept falls through to the NetworkManager block when neither local field
        // is populated.

        if (isTpHere) {
            target.sendMessage(ChatUtils.chatMessage("&e" + fromNickname + " &7has requested you teleport to them"));
        } else {
            target.sendMessage(ChatUtils.chatMessage("&e" + fromNickname + " &7has requested to teleport to you"));
        }
        target.sendMessage(ChatUtils.chatMessage("&7Use &e/tpaccept &7or &e/tpdeny"));
        AranarthUtils.playTeleportSound(target);
    }

    private void handleTpAccept(JsonObject json) {
        UUID accepterUuid     = UUID.fromString(json.get("accepterUuid").getAsString());
        String accepterNick   = json.get("accepterNickname").getAsString();
        String accepterServer = json.get("accepterServer").getAsString();
        UUID requesterUuid    = UUID.fromString(json.get("requesterUuid").getAsString());
        boolean isTpHere      = json.get("isTpHere").getAsBoolean();

        if (accepterServer.equals(thisServer)) return;

        if (isTpHere) {
            // The accepter (remote player) is coming TO the requester (local player).
            // The subtitle must name the requester, not the accepter — otherwise the
            // accepter sees "You have teleported to [yourself]".
            Player requester = Bukkit.getPlayer(requesterUuid);
            String requesterNick = requester != null
                    ? AranarthUtils.getNickname(requester)
                    : accepterNick; // fallback to accepter nick if requester somehow offline
            setPendingTeleport(accepterUuid,
                    new PendingTeleport(requesterUuid.toString(), accepterNick, "&7You have teleported to " + requesterNick));
            JsonObject transfer = new JsonObject();
            transfer.addProperty("uuid", accepterUuid.toString());
            transfer.addProperty("targetServer", thisServer);
            publish(CH_TRANSFER, transfer);
            if (requester != null) {
                requester.sendMessage(ChatUtils.chatMessage("&e" + accepterNick + " &7has accepted your teleport request"));
            }
        } else {
            Player requester = Bukkit.getPlayer(requesterUuid);
            if (requester != null) {
                requester.sendMessage(ChatUtils.chatMessage("&e" + accepterNick + " &7has accepted your teleport request"));
                String targetServer = AranarthCore.getInstance().getConfig()
                        .getString("network.servers." + accepterServer, accepterServer);
                PendingTeleport ptForRequester = new PendingTeleport(accepterUuid.toString(),
                        "&e&l" + accepterServer.toUpperCase(), "&7You have teleported to " + accepterNick);
                saveInventoryAndTransfer(requester, targetServer, ptForRequester);
            }
        }
    }

    private void handleTpDeny(JsonObject json) {
        UUID denierUuid    = UUID.fromString(json.get("denierUuid").getAsString());
        String denierNick  = json.get("denierNickname").getAsString();
        UUID requesterUuid = UUID.fromString(json.get("requesterUuid").getAsString());

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null) {
            requester.sendMessage(ChatUtils.chatMessage("&e" + denierNick + " &7has denied your teleport request"));
        }
        clearCrossServerTpContext(requesterUuid);
    }

    private void handleTransfer(JsonObject json) {
        UUID uuid       = UUID.fromString(json.get("uuid").getAsString());
        String toServer = json.get("targetServer").getAsString();

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        String velocityName = AranarthCore.getInstance().getConfig()
                .getString("network.servers." + toServer, toServer);

        // Read the pending TP that was set on the requester's server so we can embed this
        // player's inventory into it before the DB write + transfer sequence fires.
        PendingTeleport existing = getPendingTeleport(uuid);
        if (existing != null) {
            saveInventoryAndTransfer(player, velocityName, existing);
        } else {
            transferPlayer(player, velocityName);
        }
    }

    private void handleSyncTime(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        long time = json.get("time").getAsLong();
        List<World> syncWorlds = AranarthUtils.getSyncWorlds();
        for (World w : syncWorlds) {
            w.setTime(time);
        }
        if (AranarthUtils.getWeather() != Weather.CLEAR) {
            AranarthUtils.setStormDuration(0);
            for (World w : syncWorlds) {
                w.setThunderDuration(0);
                w.setWeatherDuration(0);
            }
        }
    }

    private void handleSyncWeather(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String weatherType = json.get("weatherType").getAsString();
        int duration       = json.get("duration").getAsInt();
        boolean isThunder  = json.get("isThunder").getAsBoolean();
        int stormDuration  = json.get("stormDuration").getAsInt();
        int stormDelay     = json.get("stormDelay").getAsInt();

        List<World> syncWorlds = AranarthUtils.getSyncWorlds();

        World mainWorld = Bukkit.getWorld("world");
        boolean isNewDay = mainWorld != null && (int) (mainWorld.getTime() / 20) < 5;

        switch (weatherType) {
            case "CLEAR" -> {
                AranarthUtils.setStormDuration(0);
                AranarthUtils.setWeather(Weather.CLEAR);
                for (World w : syncWorlds) {
                    w.setThunderDuration(0);
                    w.setWeatherDuration(0);
                    w.setThundering(false);
                    w.setStorm(false);
                    w.setClearWeatherDuration(duration);
                }
                AranarthUtils.setStormDelay(stormDelay);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pWorld = p.getWorld().getName();
                    if (pWorld.equals("arena") || pWorld.equals("creative")) continue;
                    if (AranarthUtils.getPlayer(p.getUniqueId()).isWeatherMessageDisabled()) continue;
                    p.sendMessage(ChatUtils.chatMessage("&7&oThe storm has subsided..."));
                    if (!isNewDay) {
                        DateUtils.playClearSound(p);
                    }
                }
            }
            case "RAIN", "THUNDER" -> {
                Weather type = isThunder ? Weather.THUNDER : Weather.RAIN;
                AranarthUtils.setWeather(type);
                AranarthUtils.setStormDelay(0);
                for (World w : syncWorlds) {
                    w.setClearWeatherDuration(0);
                    w.setStorm(true);
                    w.setThundering(isThunder);
                    w.setWeatherDuration(duration);
                    if (isThunder) {
                        w.setThunderDuration(duration);
                    }
                }
                AranarthUtils.setStormDuration(stormDuration);
                String broadcastMsg = isThunder ? "&7&oA thunderstorm has started..." : "&7&oIt has started to rain...";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pWorld = p.getWorld().getName();
                    if (pWorld.equals("arena") || pWorld.equals("creative")) continue;
                    if (AranarthUtils.getPlayer(p.getUniqueId()).isWeatherMessageDisabled()) continue;
                    p.sendMessage(ChatUtils.chatMessage(broadcastMsg));
                    if (!isNewDay) {
                        if (isThunder) {
                            DateUtils.playThunderStartSound(p);
                        } else {
                            DateUtils.playRainStartSound(p);
                        }
                    }
                }
            }
            case "SNOW" -> {
                AranarthUtils.setWeather(Weather.SNOW);
                AranarthUtils.setStormDuration(0);
                AranarthUtils.setStormDelay(0);
                for (World w : syncWorlds) {
                    w.setThunderDuration(0);
                    w.setWeatherDuration(0);
                    w.setThundering(false);
                    w.setStorm(false);
                    w.setClearWeatherDuration(duration);
                }
                AranarthUtils.setStormDuration(stormDuration);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pWorld = p.getWorld().getName();
                    if (pWorld.equals("arena") || pWorld.equals("creative")) continue;
                    if (AranarthUtils.getPlayer(p.getUniqueId()).isWeatherMessageDisabled()) continue;
                    p.sendMessage(ChatUtils.chatMessage("&7&oIt has started to snow..."));
                    if (!isNewDay) {
                        DateUtils.playSnowStartSound(p);
                    }
                }
            }
        }
    }

    private void handleDirectMessage(JsonObject json) {
        String fromServer = json.get("fromServer").getAsString();
        if (fromServer.equals(thisServer)) return;

        UUID toUuid = UUID.fromString(json.get("toUuid").getAsString());
        Player target = Bukkit.getPlayer(toUuid);
        if (target == null) return;

        String fromNickname = json.get("fromNickname").getAsString();
        UUID fromUuid = UUID.fromString(json.get("fromUuid").getAsString());
        String message = json.get("message").getAsString();

        String prefixStart = "&7⊰&r";
        String prefixEnd = "&7⊱&r";
        String targetPrefix = ChatUtils.translateToColor(prefixStart + "&7&l&oFrom: &r&e" + fromNickname + prefixEnd + " &7&o>> &e&o") + message;
        target.sendMessage(targetPrefix);
        target.playSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1f);

        // Store the sender UUID for /reply (uses last received message UUID)
        AranarthPlayer targetAp = AranarthUtils.getPlayer(toUuid);
        if (targetAp != null) {
            targetAp.setLastReceivedMessage(fromUuid);
            AranarthUtils.setPlayer(toUuid, targetAp);
        }
    }

    private void handleAfkStatus(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String nickname = json.get("nickname").getAsString();
        boolean isAfk = json.get("afk").getAsBoolean();

        // Update the in-memory roster entry and refresh that player's tab display name
        NetworkPlayer np = remoteRoster.get(uuid);
        if (np != null) {
            np.setAfk(isAfk);
            NetworkTabManager.addToTab(np);
        }

        // Broadcast the AFK message to locally online players
        String message = isAfk
                ? "&e" + nickname + " &7is now AFK"
                : "&e" + nickname + " &7is no longer AFK";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatUtils.chatMessage(message));
        }
    }

    private void handleBroadcast(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;
        String message = json.get("message").getAsString();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void handleSleepMessage(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String message = json.get("message").getAsString();
        // Show the sleep action bar to all locally-online players in survival-type worlds
        for (Player player : Bukkit.getOnlinePlayers()) {
            String worldName = player.getLocation().getWorld().getName();
            if (worldName.equals("world") || AranarthUtils.isSmpWorld(worldName) || worldName.equals("resource")) {
                long time = player.getLocation().getWorld().getTime();
                if (time > 12500 && time <= 23980) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                }
            }
        }

        // Update the remote sleeping count and notify SleepSkipListener so it can re-evaluate
        // whether the combined (local + remote) sleeping players are enough to skip the night.
        if (json.has("sleeping")) {
            remoteSleepingCount = json.get("sleeping").getAsInt();
            if (remoteSleepCallback != null) {
                remoteSleepCallback.run();
            }
        }
    }

    private void handleSoundAll(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;
        String soundKey = json.get("sound").getAsString();
        float volume = json.get("volume").getAsFloat();
        float pitch = json.get("pitch").getAsFloat();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p, soundKey, volume, pitch);
        }
    }

    private void handleBoostSync(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;
        String boostName = json.get("boost").getAsString();
        boolean removing = json.get("removing").getAsBoolean();
        Boost boost = null;
        for (Boost b : Boost.values()) {
            if (b.name().equals(boostName)) { boost = b; break; }
        }
        if (boost == null) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "handleBoostSync: unknown boost: " + boostName);
            return;
        }
        if (removing) {
            AranarthUtils.getServerBoosts().remove(boost);
        } else {
            String endTimeStr = json.get("endTime").getAsString();
            try {
                AranarthUtils.getServerBoosts().put(boost, LocalDateTime.parse(endTimeStr));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "handleBoostSync: failed to parse endTime: " + endTimeStr);
            }
        }
    }

    private void handleMailNotification(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        UUID toUuid = UUID.fromString(json.get("toUuid").getAsString());
        Player target = Bukkit.getPlayer(toUuid);
        if (target == null) return;

        String fromNickname = json.get("fromNickname").getAsString();
        target.sendMessage(ChatUtils.chatMessage("&7You have received mail from &e" + fromNickname));
        target.sendMessage(ChatUtils.chatMessage("&7View it with &e/mail read"));
        target.playSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1f);
    }

    private void handleCouncilMessage(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String message = json.get("message").getAsString();
        for (Player p : Bukkit.getOnlinePlayers()) {
            AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
            if (ap != null && (ap.getCouncilRank() > 0 || ap.getArchitectRank() >= 1)) {
                p.sendMessage(ChatUtils.translateToColor(message));
            }
        }
    }

    /** Notifies all other servers that a new word-scramble game has started on this server. */
    public void publishChatGameStart(String scrambled, String answer) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("scrambled", scrambled);
        json.addProperty("answer", answer);
        publish(CH_CHAT_GAME_START, json);
    }

    /** Notifies all other servers that the word-scramble game was won on this server. */
    public void publishChatGameWin(String winnerNickname, String answer, java.util.UUID winnerUUID,
            double elapsedSeconds, boolean newGlobalRecord, String newHolderNickname,
            java.util.UUID newHolderUUID, double newGlobalBestTime) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("winner", winnerNickname);
        json.addProperty("answer", answer);
        json.addProperty("winnerUUID", winnerUUID.toString());
        json.addProperty("elapsedSeconds", elapsedSeconds);
        json.addProperty("newGlobalRecord", newGlobalRecord);
        if (newGlobalRecord && newHolderUUID != null) {
            json.addProperty("newHolderNickname", newHolderNickname);
            json.addProperty("newHolderUUID", newHolderUUID.toString());
            json.addProperty("newGlobalBestTime", newGlobalBestTime);
        }
        publish(CH_CHAT_GAME_WIN, json);
    }

    /** Notifies all other servers that the word-scramble game expired with no winner. */
    public void publishChatGameExpire(String answer) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("answer", answer);
        publish(CH_CHAT_GAME_EXPIRE, json);
    }

    /** Publishes a dominion disband event so the other server evicts it from memory. */
    public void publishDominionDisband(UUID dominionId) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("dominionId", dominionId.toString());
        publish(CH_DOMINION_DISBAND, json);
    }

    private void handleDominionDisband(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        UUID dominionId = UUID.fromString(json.get("dominionId").getAsString());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            Dominion dominion = DominionUtils.getDominionById(dominionId);
            if (dominion == null) return;
            DominionUtils.evictDominionFromMemory(dominion);
        });
    }

    /** Publishes a dominion creation event so other servers load it into memory. */
    public void publishDominionCreate(UUID dominionId) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("dominionId", dominionId.toString());
        publish(CH_DOMINION_CREATE, json);
    }

    private void handleDominionCreate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        UUID dominionId = UUID.fromString(json.get("dominionId").getAsString());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
            PersistenceUtils.loadSingleDominionFromDatabase(dominionId));
    }

    private void handleChatGameStart(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String scrambled = json.get("scrambled").getAsString();
        String answer = json.get("answer").getAsString();
        ChatGameUtils.applyNetworkGameStart(scrambled, answer, originServer);
    }

    private void handleChatGameWin(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String winner = json.get("winner").getAsString();
        String answer = json.get("answer").getAsString();
        java.util.UUID winnerUUID = java.util.UUID.fromString(json.get("winnerUUID").getAsString());
        double elapsedSeconds = json.has("elapsedSeconds") ? json.get("elapsedSeconds").getAsDouble() : 0;
        boolean newGlobalRecord = json.has("newGlobalRecord") && json.get("newGlobalRecord").getAsBoolean();
        String newHolderNickname = newGlobalRecord && json.has("newHolderNickname") ? json.get("newHolderNickname").getAsString() : "";
        java.util.UUID newHolderUUID = newGlobalRecord && json.has("newHolderUUID")
                ? java.util.UUID.fromString(json.get("newHolderUUID").getAsString()) : null;
        double newGlobalBestTime = newGlobalRecord && json.has("newGlobalBestTime") ? json.get("newGlobalBestTime").getAsDouble() : 0;
        ChatGameUtils.applyNetworkGameWin(AranarthCore.getInstance(), winner, answer, winnerUUID,
                elapsedSeconds, newGlobalRecord, newHolderNickname, newHolderUUID, newGlobalBestTime);
    }

    private void handleChatGameExpire(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String answer = json.get("answer").getAsString();
        ChatGameUtils.applyNetworkGameExpire(AranarthCore.getInstance(), answer);
    }

    // -------------------------------------------------------------------------
    // Internal publish helper
    // -------------------------------------------------------------------------

    private void publish(String channel, JsonObject json) {
        final String payload = json.toString();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                db.publishMessage(channel, payload, thisServer);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                        + "DB publish failed on " + channel + ": " + e.getMessage());
            }
        });
    }
}
