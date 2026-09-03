package com.aearost.aranarthcore.network;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.*;
import com.aearost.aranarthcore.utils.*;
import org.bukkit.inventory.ItemStack;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.aearost.aranarthcore.utils.InteractiveChatManager;
import com.google.gson.JsonArray;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    public static final String CH_CHAT = "aranarth:chat";
    public static final String CH_CHAT_INTERACTIVE = "aranarth:chat_interactive";
    public static final String CH_JOIN = "aranarth:join";
    public static final String CH_JOIN_MSG = "aranarth:join_msg";
    public static final String CH_QUIT = "aranarth:quit";
    public static final String CH_TP_REQUEST = "aranarth:tp_request";

    // -------------------------------------------------------------------------
    // Channel names  (kept identical for compatibility)
    // -------------------------------------------------------------------------
    public static final String CH_TP_ACCEPT = "aranarth:tp_accept";
    public static final String CH_TP_DENY = "aranarth:tp_deny";
    public static final String CH_TRANSFER = "aranarth:transfer";
    public static final String CH_SYNC_TIME = "aranarth:sync_time";
    public static final String CH_SYNC_WEATHER = "aranarth:sync_weather";
    public static final String CH_NEW_DAY = "aranarth:new_day";
    public static final String CH_DM = "aranarth:dm";
    public static final String CH_SLEEP = "aranarth:sleep";
    public static final String CH_AFK = "aranarth:afk";
    public static final String CH_BROADCAST = "aranarth:broadcast";
    public static final String CH_BOOST_SYNC = "aranarth:boost_sync";
    public static final String CH_SOUND_ALL = "aranarth:sound_all";
    public static final String CH_MAIL_NOTIFY = "aranarth:mail_notify";
    public static final String CH_COUNCIL_MSG = "aranarth:council_msg";
    public static final String CH_DOMINION_DISBAND = "aranarth:dominion_disband";
    public static final String CH_DOMINION_CREATE = "aranarth:dominion_create";
    public static final String CH_DOMINION_DIPLO_REQUEST = "aranarth:dominion_diplo_request";
    public static final String CH_DOMINION_RELATION_UPDATE = "aranarth:dominion_relation_update";
    public static final String CH_DOMINION_CONQUEST_UPDATE = "aranarth:dominion_conquest_update";
    public static final String CH_DOMINION_BALANCE_ADJUST = "aranarth:dominion_balance_adjust";
    public static final String CH_CHAT_GAME_START = "aranarth:chat_game_start";
    public static final String CH_CHAT_GAME_WIN = "aranarth:chat_game_win";
    public static final String CH_CHAT_GAME_EXPIRE = "aranarth:chat_game_expire";
    public static final String CH_CHAT_GAME_CLAIM = "aranarth:chat_game_claim";
    public static final String CH_DEATH = "aranarth:death";
    public static final String CH_BREW_UNLOCK = "aranarth:brew_unlock";
    public static final String CH_BALANCE_ADJUST = "aranarth:balance_adjust";
    public static final String CH_PAY_NOTIFY = "aranarth:pay_notify";
    public static final String CH_SENTINEL_SUMMON = "aranarth:sentinel_summon";
    public static final String CH_SENTINEL_SPAWN = "aranarth:sentinel_spawn";
    public static final String CH_SENTINEL_DEATH = "aranarth:sentinel_death";
    public static final String CH_OUTPOST_DISBAND = "aranarth:outpost_disband";
    public static final String CH_OUTPOST_CREATE = "aranarth:outpost_create";
    public static final String CH_OUTPOST_UPDATE = "aranarth:outpost_update";
    public static final String CH_JOB_UPDATE = "aranarth:job_update";
    public static final String CH_VOTE_AWARD = "aranarth:vote_award";
    public static final String CH_VOTE_KEY = "aranarth:vote_key";
    public static final String CH_VP_TRANSFER = "aranarth:vp_transfer";
    public static final String CH_INVSEE_REQUEST = "aranarth:invsee_request";
    public static final String CH_INVSEE_RESPONSE = "aranarth:invsee_response";
    public static final String CH_INVSEE_UPDATE = "aranarth:invsee_update";
    public static final String CH_INVSEE_UNWATCH = "aranarth:invsee_unwatch";
    public static final String CH_RANK_UPDATE = "aranarth:rank_update";
    public static final String CH_MARKET_UPDATE = "aranarth:market_update";
    public static final String CH_PERM_RELOAD = "aranarth:perm_reload";
    // Temp-data key prefixes
    private static final String KEY_PENDING_TP = "pending_tp:";
    private static final String KEY_RETURN_LOC = "return_loc:";
    private static final String KEY_LAST_MSG = "last_msg:";
    private static NetworkManager instance;
    private final String thisServer;
    private final DatabaseManager db;
    private final Gson gson = new Gson();
    /**
     * Players currently online on OTHER servers.
     */
    private final Map<UUID, NetworkPlayer> remoteRoster = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
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
    /**
     * Pending cross-server TP requests received from another server.
     */
    private final Map<UUID, CrossServerTpContext> pendingCrossServerRequests = new ConcurrentHashMap<>();
    /**
     * Tracks viewer UUIDs watching each local target player's inventory from another server.
     * Keyed by target UUID. Populated by handleInvseeRequest, cleared on unwatch or target quit.
     */
    private final Map<UUID, Set<UUID>> remoteInvseeWatchers = new ConcurrentHashMap<>();

    /**
     * Cross-server /back locations for players who arrived from another server.
     * Format: "serverKey|world|x|y|z|yaw|pitch"
     * Populated by loadAndApplyCrossServerBack(); consumed by CommandBack.
     */
    private final Map<UUID, String> crossServerBackLocations = new ConcurrentHashMap<>();
    private long lastProcessedMessageId;
    private BukkitTask pollingTask;
    private BukkitTask cleanupTask;
    private final ExecutorService publishExecutor = Executors.newSingleThreadExecutor();
    /**
     * Number of players currently sleeping on other servers. Updated by handleSleepMessage.
     */
    private volatile int remoteSleepingCount = 0;
    /**
     * Callback invoked on the main thread whenever the remote sleeping count changes.
     */
    private Runnable remoteSleepCallback = null;

    private NetworkManager(String thisServer) {
        this.thisServer = thisServer;
        this.db = DatabaseManager.getInstance();

        // Snapshot the current max message id so we don't replay old history
        this.lastProcessedMessageId = db.getMaxMessageId();

        startPolling();
        startCleanup();
    }

    public static NetworkManager getInstance() {
        return instance;
    }

    // -------------------------------------------------------------------------
    // Constructor / lifecycle
    // -------------------------------------------------------------------------

    public static boolean isActive() {
        return instance != null;
    }

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

    // -------------------------------------------------------------------------
    // Subscriber dispatch
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Publishers
    // -------------------------------------------------------------------------

    private void doShutdown() {
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        publishExecutor.shutdown();
        // Clear this server's roster entries from the DB so stale entries don't appear on other servers
        try {
            db.clearRosterForServer(thisServer);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to clear roster on shutdown: " + e.getMessage());
        }
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "NetworkManager shut down");
    }

    private void dispatch(String channel, JsonObject json) {
        switch (channel) {
            case CH_CHAT -> handleChat(json);
            case CH_CHAT_INTERACTIVE -> handleInteractiveChat(json);
            case CH_JOIN -> handleJoin(json);
            case CH_JOIN_MSG -> handleJoinMsg(json);
            case CH_QUIT -> handleQuit(json);
            case CH_TP_REQUEST -> handleTpRequest(json);
            case CH_TP_ACCEPT -> handleTpAccept(json);
            case CH_TP_DENY -> handleTpDeny(json);
            case CH_TRANSFER -> handleTransfer(json);
            case CH_SYNC_TIME -> handleSyncTime(json);
            case CH_SYNC_WEATHER -> handleSyncWeather(json);
            case CH_NEW_DAY -> handleNewDay(json);
            case CH_DM -> handleDirectMessage(json);
            case CH_SLEEP -> handleSleepMessage(json);
            case CH_AFK -> handleAfkStatus(json);
            case CH_BROADCAST -> handleBroadcast(json);
            case CH_BOOST_SYNC -> handleBoostSync(json);
            case CH_SOUND_ALL -> handleSoundAll(json);
            case CH_MAIL_NOTIFY -> handleMailNotification(json);
            case CH_COUNCIL_MSG -> handleCouncilMessage(json);
            case CH_DOMINION_DISBAND -> handleDominionDisband(json);
            case CH_DOMINION_CREATE -> handleDominionCreate(json);
            case CH_DOMINION_DIPLO_REQUEST -> handleDominionDiploRequest(json);
            case CH_DOMINION_RELATION_UPDATE -> handleDominionRelationUpdate(json);
            case CH_DOMINION_CONQUEST_UPDATE -> handleDominionConquestUpdate(json);
            case CH_DOMINION_BALANCE_ADJUST -> handleDominionBalanceAdjust(json);
            case CH_CHAT_GAME_START -> handleChatGameStart(json);
            case CH_CHAT_GAME_WIN -> handleChatGameWin(json);
            case CH_CHAT_GAME_EXPIRE -> handleChatGameExpire(json);
            case CH_CHAT_GAME_CLAIM -> handleChatGameClaim(json);
            case CH_DEATH -> handleDeath(json);
            case CH_BREW_UNLOCK -> handleBrewUnlock(json);
            case CH_BALANCE_ADJUST -> handleBalanceAdjust(json);
            case CH_PAY_NOTIFY -> handlePayNotify(json);
            case CH_SENTINEL_SUMMON -> handleSentinelSummon(json);
            case CH_SENTINEL_SPAWN -> handleSentinelSpawn(json);
            case CH_SENTINEL_DEATH -> handleSentinelDeath(json);
            case CH_OUTPOST_DISBAND -> handleOutpostDisband(json);
            case CH_OUTPOST_CREATE -> handleOutpostCreate(json);
            case CH_OUTPOST_UPDATE -> handleOutpostUpdate(json);
            case CH_JOB_UPDATE -> handleJobUpdate(json);
            case CH_VOTE_AWARD -> handleVoteAward(json);
            case CH_VOTE_KEY -> handleVoteKey(json);
            case CH_VP_TRANSFER -> handleVpTransfer(json);
            case CH_INVSEE_REQUEST -> handleInvseeRequest(json);
            case CH_INVSEE_RESPONSE -> handleInvseeResponse(json);
            case CH_INVSEE_UPDATE -> handleInvseeUpdate(json);
            case CH_INVSEE_UNWATCH -> handleInvseeUnwatch(json);
            case CH_RANK_UPDATE -> handleRankUpdate(json);
            case CH_MARKET_UPDATE -> handleMarketUpdate(json);
            case CH_PERM_RELOAD -> handlePermReload(json);
        }
    }

    /**
     * Publishes a public chat message so the other server can relay it to its players.
     */
    public void publishChat(String prefix, String chatMessage) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("prefix", prefix);
        json.addProperty("message", chatMessage);
        publish(CH_CHAT, json);
    }

    /**
     * Publishes an interactive chat message (one containing [item]/[inv]/[ec]/[coords]) so the
     * other server can display the same clickable component and serve /ichat view clicks locally.
     *
     * @param prefix       the formatted prefix string (legacy colour codes already applied)
     * @param chatMessage  the plain-text version of the message (for console/discord fallback)
     * @param fullMessage  the fully assembled Adventure Component including prefix and interactive parts
     * @param snapshots    every snapshot that was created while building the message
     */
    public void publishInteractiveChat(String prefix, String chatMessage, Component fullMessage,
                                       List<InteractiveChatManager.Snapshot> snapshots) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("prefix", prefix);
        json.addProperty("message", chatMessage);
        json.addProperty("componentJson", GsonComponentSerializer.gson().serialize(fullMessage));

        JsonArray snapshotArray = new JsonArray();
        for (InteractiveChatManager.Snapshot snap : snapshots) {
            JsonObject snapObj = new JsonObject();
            snapObj.addProperty("id", snap.getId().toString());
            snapObj.addProperty("ownerId", snap.getOwnerId().toString());
            snapObj.addProperty("ownerDisplay", snap.getOwnerDisplayName());
            snapObj.addProperty("type", snap.getType().name());

            JsonArray itemsArray = new JsonArray();
            for (ItemStack item : snap.getItems()) {
                if (item == null || item.getType().isAir()) {
                    itemsArray.add((String) null);
                } else {
                    itemsArray.add(java.util.Base64.getEncoder().encodeToString(item.serializeAsBytes()));
                }
            }
            snapObj.add("items", itemsArray);
            snapshotArray.add(snapObj);
        }
        json.add("snapshots", snapshotArray);
        publish(CH_CHAT_INTERACTIVE, json);
    }

    private void handleInteractiveChat(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        String prefix = json.get("prefix").getAsString();
        String message = json.get("message").getAsString();

        // Rebuild snapshots locally so /ichat view works on this server
        JsonArray snapshotArray = json.getAsJsonArray("snapshots");
        for (int i = 0; i < snapshotArray.size(); i++) {
            JsonObject snapObj = snapshotArray.get(i).getAsJsonObject();
            UUID snapId = UUID.fromString(snapObj.get("id").getAsString());
            UUID ownerId = UUID.fromString(snapObj.get("ownerId").getAsString());
            String ownerDisplay = snapObj.get("ownerDisplay").getAsString();
            InteractiveChatManager.SnapshotType type =
                    InteractiveChatManager.SnapshotType.valueOf(snapObj.get("type").getAsString());

            JsonArray itemsArray = snapObj.getAsJsonArray("items");
            ItemStack[] items = new ItemStack[itemsArray.size()];
            for (int j = 0; j < itemsArray.size(); j++) {
                if (itemsArray.get(j).isJsonNull()) {
                    items[j] = null;
                } else {
                    try {
                        byte[] bytes = java.util.Base64.getDecoder().decode(itemsArray.get(j).getAsString());
                        items[j] = ItemStack.deserializeBytes(bytes);
                    } catch (Exception ex) {
                        items[j] = null;
                    }
                }
            }
            InteractiveChatManager.storeSnapshotWithId(snapId, ownerId, ownerDisplay, type, items);
        }

        // Deserialize the full Component (prefix + interactive message) and send it
        Component component;
        try {
            component = GsonComponentSerializer.gson().deserialize(json.get("componentJson").getAsString());
        } catch (Exception ex) {
            // Fallback to plain text if component JSON is malformed
            component = LegacyComponentSerializer.legacySection().deserialize(
                    ChatUtils.translateToColor(prefix + message));
        }
        final Component finalComponent = component;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(finalComponent);
        }
        Bukkit.getConsoleSender().sendMessage(ChatUtils.translateToColor(
                "&8[" + originServer.toUpperCase() + "] " + prefix + message));
    }

    /**
     * Called from PlayerServerJoinListener after the player's AranarthPlayer is ready.
     */
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
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Net] publishPlayerJoin: texture extraction returned empty for " + ap.getUsername() + " - remote tab may show default skin");
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
     *
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
     *
     * @param uuid        The UUID of the player who disconnected.
     * @param quitMessage The formatted quit message to broadcast, or null for cross-server transfers.
     * @param isVanished  Whether the player was vanished (suppresses the public message).
     */
    public void publishPlayerQuit(UUID uuid, String quitMessage, boolean isVanished) {
        // Remove from roster in DB
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                db.removeRosterEntry(uuid)
        );

        // Clear any remote invsee watchers for this player
        remoteInvseeWatchers.remove(uuid);

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
     *
     * @param time The new world time in ticks.
     */
    public void publishSyncTime(long time) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("time", time);
        publish(CH_SYNC_TIME, json);
    }

    /**
     * Broadcasts the new server date to SMP so it can apply all new-day effects
     * without independently computing the date transition.
     */
    public void publishNewDay(int dayNum, int weekdayNum, Month month, int yearNum, boolean isNewMonth) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("day", dayNum);
        json.addProperty("weekday", weekdayNum);
        json.addProperty("month", month.name());
        json.addProperty("year", yearNum);
        json.addProperty("isNewMonth", isNewMonth);
        publish(CH_NEW_DAY, json);
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
     *
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

    /**
     * Returns the number of remote players that should count toward the sleep threshold.
     */
    public int getRemoteSleepEligibleCount() {
        long deepAfkThresholdMs = AranarthUtils.getAfkSecondsAmount() * 1000L;
        long now = System.currentTimeMillis();
        int count = 0;
        for (NetworkPlayer np : remoteRoster.values()) {
            long startTime = np.getAfkStartTime();
            if (startTime > 0 && (now - startTime) >= deepAfkThresholdMs) {
                continue; // Deeply AFK - skip
            }
            count++;
        }
        return count;
    }

    public void setRemoteSleepCallback(Runnable callback) {
        this.remoteSleepCallback = callback;
    }

    /**
     * Returns the number of players currently sleeping on other servers.
     */
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
     * Relays a server tip message to all other servers.
     */
    public void publishServerTip(String rawMessage) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("message", rawMessage);
        json.addProperty("type", "server_tip");
        publish(CH_BROADCAST, json);
    }

    /**
     * Notifies all other servers that a player has unlocked a brew recipe so their in-memory
     * caches stay in sync. The DB write is handled by the originating server before this publish.
     */
    public void publishBrewUnlock(UUID uuid, String recipeId) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("recipeId", recipeId);
        publish(CH_BREW_UNLOCK, json);
    }

    /**
     * Notifies all other servers of a balance change for a player not online on this server
     * (e.g., a shop sale or purchase). Receiving servers update their in-memory copy so that
     * the next periodic save does not overwrite the change with a stale value.
     *
     * @param uuid  The UUID of the player whose balance changed.
     * @param delta The amount added (positive) or subtracted (negative) from the balance.
     */
    public void publishBalanceAdjust(UUID uuid, double delta) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("delta", delta);
        publish(CH_BALANCE_ADJUST, json);
    }

    /**
     * Broadcasts a dominion balance delta to all other servers.
     *
     * @param dominionId The UUID of the dominion whose balance changed.
     * @param delta      The amount added (positive) or subtracted (negative).
     */
    public void publishDominionBalanceAdjust(UUID dominionId, double delta) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("dominionId", dominionId.toString());
        json.addProperty("delta", delta);
        publish(CH_DOMINION_BALANCE_ADJUST, json);
    }

    /**
     * Notifies the server where the target player is online that they received a payment.
     *
     * @param toUuid          The UUID of the player who received the payment.
     * @param fromNickname    The nickname of the paying player.
     * @param formattedAmount The already-formatted currency string (e.g. "$4,000.00").
     */
    public void publishPayNotify(UUID toUuid, String fromNickname, String formattedAmount) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("toUuid", toUuid.toString());
        json.addProperty("fromNickname", fromNickname);
        json.addProperty("formattedAmount", formattedAmount);
        publish(CH_PAY_NOTIFY, json);
    }

    /**
     * Requests that a remote server despawn and transfer the given sentinel entities to this server.
     */
    public void publishSentinelSummon(UUID playerUuid, String targetServer, List<UUID> entityUuids,
                                      org.bukkit.entity.EntityType type,
                                      String targetWorld, double targetX, double targetY, double targetZ) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("targetServer", targetServer);
        json.addProperty("playerUuid", playerUuid.toString());
        json.addProperty("sentinelType", type.name());
        json.addProperty("targetWorld", targetWorld);
        json.addProperty("targetX", targetX);
        json.addProperty("targetY", targetY);
        json.addProperty("targetZ", targetZ);
        com.google.gson.JsonArray uuids = new com.google.gson.JsonArray();
        for (UUID uuid : entityUuids) uuids.add(uuid.toString());
        json.add("entityUuids", uuids);
        publish(CH_SENTINEL_SUMMON, json);
    }

    /**
     * Notifies other servers that a sentinel entity has died, so the owner's server
     * can remove it from their player data.
     */
    public void publishSentinelDeath(UUID sentinelUuid, org.bukkit.entity.EntityType type) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("sentinelUuid", sentinelUuid.toString());
        json.addProperty("sentinelType", type.name());
        publish(CH_SENTINEL_DEATH, json);
    }

    /**
     * Broadcasts a player death message from this server to all other servers.
     * The message should already contain translated § color codes.
     */
    public void publishDeath(String deathMessage) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("message", deathMessage);
        publish(CH_DEATH, json);
    }

    /**
     * Notifies other servers that a player's rank has changed so they can update the remote roster
     * and refresh the TAB list immediately.
     */
    public void publishRankUpdate(UUID uuid, int rank, int councilRank, int saintRank, int architectRank) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("rank", rank);
        json.addProperty("councilRank", councilRank);
        json.addProperty("saintRank", saintRank);
        json.addProperty("architectRank", architectRank);
        publish(CH_RANK_UPDATE, json);
    }

    /**
     * Tells the other server to re-evaluate permissions for all its online players,
     * or for a specific player if uuid is non-null.
     */
    public void publishPermReload(UUID uuid) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        if (uuid != null) {
            json.addProperty("uuid", uuid.toString());
        }
        publish(CH_PERM_RELOAD, json);
    }

    /**
     * Notifies all other servers that server shop sell prices have been updated
     * so they can reload market data from the database and refresh signs.
     */
    public void publishMarketUpdate() {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        publish(CH_MARKET_UPDATE, json);
    }

    public void publishAfkStatus(UUID uuid, String nickname, boolean isAfk) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("nickname", nickname);
        json.addProperty("afk", isAfk);
        json.addProperty("afkStartTime", isAfk ? System.currentTimeMillis() : 0L);
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
     *
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
     *
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

    /**
     * Stores a pending teleport for {@code uuid} to be executed when they arrive on this server. TTL 5 minutes.
     */
    public void setPendingTeleport(UUID uuid, PendingTeleport pending) {
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                db.saveTempData(KEY_PENDING_TP + uuid, gson.toJson(pending), 300);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "DB: failed to set pending TP for " + uuid);
            }
        });
    }

    /**
     * Returns the pending teleport for {@code uuid}, or null if none. Must be called synchronously.
     */
    public PendingTeleport getPendingTeleport(UUID uuid) {
        try {
            String data = db.loadTempData(KEY_PENDING_TP + uuid);
            if (data == null) {
                return null;
            }
            PendingTeleport pending = gson.fromJson(data, PendingTeleport.class);
            if (pending.isStale()) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Net] Discarding stale pending TP for " + uuid + " (survived server restart)");
                clearPendingTeleport(uuid);
                return null;
            }
            return pending;
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "DB: failed to get pending TP for " + uuid);
            return null;
        }
    }

    /**
     * Removes the pending teleport for {@code uuid}.
     */
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

    /**
     * Saves the player's current survival location before they transfer to SMP. TTL 1 hour.
     */
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
            if (data == null) {
                return null;
            }
            db.deleteTempData(KEY_RETURN_LOC + uuid);
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            World world = Bukkit.getWorld(json.get("world").getAsString());
            if (world == null) {
                return null;
            }
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
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
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
        if (!DatabaseManager.isActive()) {
            return;
        }
        try {
            String data = db.loadTempData(KEY_LAST_MSG + uuid);
            if (data == null || data.isEmpty()) {
                return;
            }
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
        if (!DatabaseManager.isActive()) {
            return;
        }
        try {
            String data = db.loadTempData(KEY_RETURN_LOC + uuid);
            if (data == null) {
                return;
            }
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
                    if (ap != null) {
                        ap.setLastKnownTeleportLocation(new Location(w, x, y, z, yaw, pitch));
                    }
                }
            } else {
                // Back location is on a different server - store for CommandBack to route cross-server.
                // Also clear the local lastKnownTeleportLocation (set by the pending-TP spawn teleport)
                // so /back falls through to the cross-server routing instead of going to the spawn point.
                crossServerBackLocations.put(uuid, server + "|" + worldName + "|" + x + "|" + y + "|" + z + "|" + yaw + "|" + pitch);
                AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
                if (ap != null) {
                    ap.setLastKnownTeleportLocation(null);
                }
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

    /**
     * Marks this player's quit as a cross-server transfer for DiscordSRV suppression.
     */
    public void markCrossServerQuit(UUID uuid) {
        crossServerQuitPlayers.add(uuid);
    }

    /**
     * Returns true and removes if the player's quit was a cross-server transfer.
     */
    public boolean consumeCrossServerQuit(UUID uuid) {
        return crossServerQuitPlayers.remove(uuid);
    }

    /**
     * Marks this player's join as a cross-server transfer arrival for DiscordSRV suppression.
     */
    public void markCrossServerJoin(UUID uuid) {
        crossServerJoinPlayers.add(uuid);
    }

    /**
     * Returns true and removes if the player's join was a cross-server transfer arrival.
     */
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
                        + "' matched no inventory branch for " + player.getName() + " - inventory NOT saved before transfer");
            }
            AranarthUtils.setPlayer(uuid, ap);
        }

        pending.setApplyInventory(true);

        // Build the serialized forms now, on the main thread, so the async task only does I/O.
        final String rawRow = PersistenceUtils.buildPlayerRowForTransfer(uuid);
        // Advance this server's balance snapshot to what we are about to write
        // If the player returns here, the delta calculation starts from the value we just handed off
        AranarthPlayer apForSnapshot = AranarthUtils.getPlayer(uuid);
        if (apForSnapshot != null) {
            apForSnapshot.setBalanceSnapshot(apForSnapshot.getBalance());
        }
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
        // message - the server still needs to broadcast the player's departure in that case.

        final String playerUsername = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            boolean dbWriteSucceeded = true;
            try {
                if (rawRow != null && DatabaseManager.isActive()) {
                    DatabaseManager.getInstance().saveAranarthPlayerRaw(uuid, playerUsername, rawRow);
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
                    // Player disconnected during the async write - quit message was not suppressed
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
        // NOTE: transferringPlayers is NOT set here - see saveInventoryAndTransfer for rationale.

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

    /**
     * Returns all players currently on OTHER servers.
     */
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
        if (originServer.equals(thisServer)) {
            return;
        }

        String prefix = json.get("prefix").getAsString();
        String message = json.get("message").getAsString();

        String formatted = ChatUtils.translateToColor(prefix + message);
        Component component = LegacyComponentSerializer.legacySection().deserialize(formatted);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(ChatUtils.translateToColor(
                "&8[" + originServer.toUpperCase() + "] " + prefix + message));
    }

    private void handleJoin(JsonObject json) {
        String server = json.get("server").getAsString();
        if (server.equals(thisServer)) {
            return;
        }

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
        if (originServer.equals(thisServer)) {
            return;
        }

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
            // New player first join - play the challenge-complete fanfare
            for (Player p : Bukkit.getOnlinePlayers()) {
                int vol = AranarthUtils.getPlayer(p.getUniqueId()).getJoinSoundVolume();
                if (vol > 0) {
                    p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, vol / 100f, 0.8F);
                }
            }
        } else {
            // Regular join - play the ascending note-block xylophone
            new BukkitRunnable() {
                int runs = 0;

                @Override
                public void run() {
                    if (runs == 0) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            int vol = AranarthUtils.getPlayer(p.getUniqueId()).getJoinSoundVolume();
                            if (vol > 0) {
                                p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 1F);
                            }
                        }
                        runs++;
                    } else if (runs == 1) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            int vol = AranarthUtils.getPlayer(p.getUniqueId()).getJoinSoundVolume();
                            if (vol > 0) {
                                p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 1.2F);
                            }
                        }
                        runs++;
                    } else {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            int vol = AranarthUtils.getPlayer(p.getUniqueId()).getJoinSoundVolume();
                            if (vol > 0) {
                                p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 1.6F);
                            }
                        }
                        cancel();
                    }
                }
            }.runTaskTimer(AranarthCore.getInstance(), 0, 5);
        }
    }

    private void handleQuit(JsonObject json) {
        String server = json.get("server").getAsString();
        if (server.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        remoteRoster.remove(uuid);

        // Close any remote invsee GUIs local players have open for this player
        com.aearost.aranarthcore.gui.GuiInvsee.closeAllRemoteFor(uuid);
        // Guard: if the player just transferred to THIS server, their vanilla tab entry is
        // already present - removing it would blank their skin and hide them from their own tab.
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
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            int vol = AranarthUtils.getPlayer(p.getUniqueId()).getLeaveSoundVolume();
                            if (vol > 0) {
                                p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 1.6F);
                            }
                        }
                        runs++;
                    } else if (runs == 1) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            int vol = AranarthUtils.getPlayer(p.getUniqueId()).getLeaveSoundVolume();
                            if (vol > 0) {
                                p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 1.2F);
                            }
                        }
                        runs++;
                    } else {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            int vol = AranarthUtils.getPlayer(p.getUniqueId()).getLeaveSoundVolume();
                            if (vol > 0) {
                                p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 0.8F);
                            }
                        }
                        cancel();
                    }
                }
            }.runTaskTimer(AranarthCore.getInstance(), 0, 5);
        }
    }

    private void handleTpRequest(JsonObject json) {
        UUID fromUuid = UUID.fromString(json.get("fromUuid").getAsString());
        String fromNickname = json.get("fromNickname").getAsString();
        String fromServer = json.get("fromServer").getAsString();
        UUID toUuid = UUID.fromString(json.get("toUuid").getAsString());
        boolean isTpHere = json.get("isTpHere").getAsBoolean();

        if (fromServer.equals(thisServer)) {
            return;
        }

        Player target = Bukkit.getPlayer(toUuid);
        if (target == null) {
            return;
        }

        AranarthPlayer targetAp = AranarthUtils.getPlayer(toUuid);
        if (targetAp.isTogglingTp()) {
            publishTpDenied(toUuid, targetAp.getNickname().isEmpty() ? target.getName() : targetAp.getNickname(), fromUuid);
            return;
        }

        storeCrossServerTpContext(toUuid, new CrossServerTpContext(fromUuid, fromNickname, fromServer, isTpHere));

        // Do NOT set teleportToUuid/teleportFromUuid here - those fields resolve the UUID via
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
        target.sendMessage(ChatUtils.buildTpAcceptDenyPrompt());
        AranarthUtils.playTeleportSound(target);
    }

    private void handleTpAccept(JsonObject json) {
        UUID accepterUuid = UUID.fromString(json.get("accepterUuid").getAsString());
        String accepterNick = json.get("accepterNickname").getAsString();
        String accepterServer = json.get("accepterServer").getAsString();
        UUID requesterUuid = UUID.fromString(json.get("requesterUuid").getAsString());
        boolean isTpHere = json.get("isTpHere").getAsBoolean();

        if (accepterServer.equals(thisServer)) {
            return;
        }

        if (isTpHere) {
            // The accepter (remote player) is coming TO the requester (local player).
            // The subtitle must name the requester, not the accepter - otherwise the
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
        UUID denierUuid = UUID.fromString(json.get("denierUuid").getAsString());
        String denierNick = json.get("denierNickname").getAsString();
        UUID requesterUuid = UUID.fromString(json.get("requesterUuid").getAsString());

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null) {
            requester.sendMessage(ChatUtils.chatMessage("&e" + denierNick + " &7has denied your teleport request"));
        }
        clearCrossServerTpContext(requesterUuid);
    }

    private void handleTransfer(JsonObject json) {
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String toServer = json.get("targetServer").getAsString();

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

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
        if (originServer.equals(thisServer)) {
            return;
        }

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

    private void handleNewDay(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        int dayNum = json.get("day").getAsInt();
        int weekdayNum = json.get("weekday").getAsInt();
        Month month = Month.valueOf(json.get("month").getAsString());
        int yearNum = json.get("year").getAsInt();
        boolean isNewMonth = json.get("isNewMonth").getAsBoolean();

        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                DateUtils.applyNewDay(dayNum, weekdayNum, month, yearNum, isNewMonth)
        );
    }

    private void handleSyncWeather(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        String weatherType = json.get("weatherType").getAsString();
        int duration = json.get("duration").getAsInt();
        boolean isThunder = json.get("isThunder").getAsBoolean();
        int stormDuration = json.get("stormDuration").getAsInt();
        int stormDelay = json.get("stormDelay").getAsInt();

        List<World> syncWorlds = AranarthUtils.getSyncWorlds();

        World mainWorld = Bukkit.getWorld("world");
        boolean isNewDay = mainWorld != null && (int) (mainWorld.getTime() / 20) < 5;

        Weather prevWeather = AranarthUtils.getWeather();
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
                if (prevWeather == Weather.CLEAR) {
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pWorld = p.getWorld().getName();
                    if (pWorld.equals("arena") || pWorld.equals("creative")) {
                        continue;
                    }
                    AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
                    if (ap.isWeatherMessageDisabled()) {
                        continue;
                    }
                    p.sendMessage(ChatUtils.chatMessage("&7&oThe storm has subsided..."));
                    if (!isNewDay) {
                        int wVol = ap.getWeatherSoundVolume();
                        if (wVol > 0) {
                            DateUtils.playClearSound(p, wVol / 100f);
                        }
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
                if (prevWeather == type) {
                    return;
                }
                String broadcastMsg = isThunder ? "&7&oA thunderstorm has started..." : "&7&oIt has started to rain...";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pWorld = p.getWorld().getName();
                    if (pWorld.equals("arena") || pWorld.equals("creative")) {
                        continue;
                    }
                    AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
                    if (ap.isWeatherMessageDisabled()) {
                        continue;
                    }
                    p.sendMessage(ChatUtils.chatMessage(broadcastMsg));
                    if (!isNewDay) {
                        int wVol = ap.getWeatherSoundVolume();
                        if (wVol > 0) {
                            if (isThunder) {
                                DateUtils.playThunderStartSound(p, wVol / 100f);
                            } else {
                                DateUtils.playRainStartSound(p, wVol / 100f);
                            }
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
                if (prevWeather == Weather.SNOW) {
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pWorld = p.getWorld().getName();
                    if (pWorld.equals("arena") || pWorld.equals("creative")) {
                        continue;
                    }
                    AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
                    if (ap.isWeatherMessageDisabled()) {
                        continue;
                    }
                    p.sendMessage(ChatUtils.chatMessage("&7&oIt has started to snow..."));
                    if (!isNewDay) {
                        int wVol = ap.getWeatherSoundVolume();
                        if (wVol > 0) {
                            DateUtils.playSnowStartSound(p, wVol / 100f);
                        }
                    }
                }
            }
        }
    }

    private void handleDirectMessage(JsonObject json) {
        String fromServer = json.get("fromServer").getAsString();
        if (fromServer.equals(thisServer)) {
            return;
        }

        UUID toUuid = UUID.fromString(json.get("toUuid").getAsString());
        Player target = Bukkit.getPlayer(toUuid);
        if (target == null) {
            return;
        }

        String fromNickname = json.get("fromNickname").getAsString();
        UUID fromUuid = UUID.fromString(json.get("fromUuid").getAsString());
        String message = json.get("message").getAsString();

        String prefixStart = "&7⊰&r";
        String prefixEnd = "&7⊱&r";
        Component targetPrefixComp = LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor(prefixStart + "&7&l&oFrom: &r&e" + fromNickname + prefixEnd + " &7&o>> &e&o"));
        Component targetMsgComp = targetPrefixComp.append(LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor("&e&o") + message));
        target.sendMessage(ChatUtils.clickableCommand(targetMsgComp, ChatUtils.translateToColor("&7Reply to &e" + fromNickname), "/r ", true));
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
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String nickname = json.get("nickname").getAsString();
        boolean isAfk = json.get("afk").getAsBoolean();

        long afkStartTime = json.has("afkStartTime") ? json.get("afkStartTime").getAsLong() : 0L;

        // Update the in-memory roster entry and refresh that player's tab display name
        NetworkPlayer np = remoteRoster.get(uuid);
        if (np != null) {
            np.setAfk(isAfk);
            np.setAfkStartTime(afkStartTime);
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

    private void handleRankUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        int rank = json.get("rank").getAsInt();
        int councilRank = json.get("councilRank").getAsInt();
        int saintRank = json.get("saintRank").getAsInt();
        int architectRank = json.get("architectRank").getAsInt();

        NetworkPlayer np = remoteRoster.get(uuid);
        if (np != null) {
            // Player is on a remote server - update the remote roster entry and refresh TAB
            np.setRank(rank);
            np.setCouncilRank(councilRank);
            np.setSaintRank(saintRank);
            np.setArchitectRank(architectRank);
            if (!np.isVanished()) {
                NetworkTabManager.addToTab(np);
            }
            AranarthUtils.updateTab();
        } else {
            // Player is locally online on this server - update their local state and re-evaluate
            Player localPlayer = Bukkit.getPlayer(uuid);
            if (localPlayer != null) {
                AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
                if (ap != null) {
                    ap.setRank(rank);
                    ap.setCouncilRank(councilRank);
                    ap.setSaintRank(saintRank);
                    ap.setArchitectRank(architectRank);
                    AranarthUtils.setPlayer(uuid, ap);
                    Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                            () -> PermissionUtils.evaluatePlayerPermissions(localPlayer));
                }
            }
        }
    }

    private void handlePermReload(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            if (json.has("uuid")) {
                // Reload a specific player if they happen to be online on this server
                UUID uuid = UUID.fromString(json.get("uuid").getAsString());
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    PermissionUtils.evaluatePlayerPermissions(player);
                }
            } else {
                // Reload all online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PermissionUtils.evaluatePlayerPermissions(player);
                }
            }
        });
    }

    private void handleMarketUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        // Reload market dynamics from DB and refresh signs on main thread
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            PersistenceUtils.loadMarketDynamicsFromDatabase();
            List<Shop> serverShops = ShopUtils.getShops().get(null);
            if (serverShops != null) {
                for (Shop shop : serverShops) {
                    MarketUtils.refreshServerShopSign(shop);
                }
            }
        });
    }

    private void handleBroadcast(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        String message = json.get("message").getAsString();
        boolean isServerTip = json.has("type") && json.get("type").getAsString().equals("server_tip");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isServerTip) {
                AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
                if (ap != null && ap.isServerTipsDisabled()) {
                    continue;
                }
            }
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void handleDeath(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        String message = json.get("message").getAsString();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void handleBrewUnlock(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String recipeId = json.get("recipeId").getAsString();
        BrewRecipeUtils.applyRemoteUnlock(uuid, recipeId);
    }

    private void handleBalanceAdjust(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        double delta = json.get("delta").getAsDouble();
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap != null) {
            ap.setBalance(ap.getBalance() + delta);
            // Immediately persist so the next periodic save cannot overwrite
            // the updated balance with a stale value.
            PersistenceUtils.saveAranarthPlayerImmediately(uuid);
        }
    }

    private void handleDominionBalanceAdjust(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID dominionId = UUID.fromString(json.get("dominionId").getAsString());
        double delta = json.get("delta").getAsDouble();
        Dominion dominion = DominionUtils.getDominionById(dominionId);
        if (dominion != null) {
            dominion.setBalance(dominion.getBalance() + delta);
            PersistenceUtils.saveSingleDominionToDatabase(dominion);
        }
    }

    private void handlePayNotify(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID toUuid = UUID.fromString(json.get("toUuid").getAsString());
        Player target = Bukkit.getPlayer(toUuid);
        if (target == null) {
            return;
        }
        String fromNickname = json.get("fromNickname").getAsString();
        String formattedAmount = json.get("formattedAmount").getAsString();
        target.sendMessage(ChatUtils.chatMessage("&7You have received &6" + formattedAmount + " &7from &e" + fromNickname));
    }

    private void handleSleepMessage(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

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
        if (originServer.equals(thisServer)) {
            return;
        }
        String soundKey = json.get("sound").getAsString();
        float volume = json.get("volume").getAsFloat();
        float pitch = json.get("pitch").getAsFloat();
        boolean isVoteSound = soundKey.equals("minecraft:entity.experience_orb.pickup");
        boolean isAvatarSound = soundKey.equals("minecraft:entity.breeze.inhale")
                || soundKey.equals("minecraft:ui.toast.challenge_complete")
                || soundKey.equals("minecraft:entity.wither.death")
                || soundKey.equals("minecraft:entity.breeze.idle_air");
        for (Player p : Bukkit.getOnlinePlayers()) {
            AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
            if (isVoteSound) {
                int voteVol = ap.getVoteSoundVolume();
                if (voteVol == 0) {
                    continue;
                }
                p.playSound(p, soundKey, volume * (voteVol / 100f), pitch);
            } else if (isAvatarSound) {
                int avVol = ap.getAvatarSoundVolume();
                if (avVol == 0) {
                    continue;
                }
                p.playSound(p, soundKey, volume * (avVol / 100f), pitch);
            } else {
                p.playSound(p, soundKey, volume, pitch);
            }
        }
    }

    private void handleBoostSync(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        String boostName = json.get("boost").getAsString();
        boolean removing = json.get("removing").getAsBoolean();
        Boost boost = null;
        for (Boost b : Boost.values()) {
            if (b.name().equals(boostName)) {
                boost = b;
                break;
            }
        }
        if (boost == null) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "handleBoostSync: unknown boost: " + boostName);
            return;
        }
        if (removing) {
            AranarthUtils.removeServerBoost(boost, true);
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
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID toUuid = UUID.fromString(json.get("toUuid").getAsString());
        Player target = Bukkit.getPlayer(toUuid);
        if (target == null) {
            return;
        }

        // Reload this server's in-memory mail cache from MySQL so the new message
        // is immediately readable with /mail read without needing a server switch.
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                () -> PersistenceUtils.reloadPlayerMailFromDatabase(toUuid));

        String fromNickname = json.get("fromNickname").getAsString();
        target.sendMessage(ChatUtils.chatMessage("&7You have received mail from &e" + fromNickname));
        target.sendMessage(ChatUtils.chatMessage("&7View it with &e/mail read"));
        target.playSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1f);
    }

    private void handleCouncilMessage(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        String message = json.get("message").getAsString();
        for (Player p : Bukkit.getOnlinePlayers()) {
            AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
            if (ap != null && (ap.getCouncilRank() > 0 || ap.getArchitectRank() >= 1)) {
                p.sendMessage(ChatUtils.translateToColor(message));
            }
        }
    }

    /**
     * Notifies all other servers that a new word-scramble game has started on this server.
     */
    public void publishChatGameStart(String scrambled, String answer) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("scrambled", scrambled);
        json.addProperty("answer", answer);
        publish(CH_CHAT_GAME_START, json);
    }

    /**
     * Notifies all other servers that the word-scramble game was won on this server.
     */
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

    /**
     * Forwards a correct guess from a non-origin server to the origin for authoritative win processing.
     */
    public void publishChatGameClaim(java.util.UUID playerUUID, String playerNickname, double elapsedSeconds) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("playerUUID", playerUUID.toString());
        json.addProperty("playerNickname", playerNickname);
        json.addProperty("elapsedSeconds", elapsedSeconds);
        publish(CH_CHAT_GAME_CLAIM, json);
    }

    /**
     * Notifies all other servers that the word-scramble game expired with no winner.
     */
    public void publishChatGameExpire(String answer) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("answer", answer);
        publish(CH_CHAT_GAME_EXPIRE, json);
    }

    /**
     * Publishes a dominion disband event so the other server evicts it from memory.
     */
    public void publishDominionDisband(UUID dominionId) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("dominionId", dominionId.toString());
        publish(CH_DOMINION_DISBAND, json);
    }

    private void handleDominionDisband(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID dominionId = UUID.fromString(json.get("dominionId").getAsString());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            Dominion dominion = DominionUtils.getDominionById(dominionId);
            if (dominion == null) {
                return;
            }
            DominionUtils.evictDominionFromMemory(dominion);
        });
    }

    /**
     * Publishes a dominion creation event so other servers load it into memory.
     */
    public void publishDominionCreate(UUID dominionId) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("dominionId", dominionId.toString());
        publish(CH_DOMINION_CREATE, json);
    }

    private void handleDominionCreate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID dominionId = UUID.fromString(json.get("dominionId").getAsString());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                PersistenceUtils.loadSingleDominionFromDatabase(dominionId));
    }

    /**
     * Publishes an outpost disband so other servers remove the stub from memory.
     */
    public void publishOutpostDisband(java.util.UUID outpostId) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("outpostId", outpostId.toString());
        publish(CH_OUTPOST_DISBAND, json);
    }

    private void handleOutpostDisband(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        java.util.UUID outpostId = java.util.UUID.fromString(json.get("outpostId").getAsString());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                OutpostUtils.evictOutpostFromMemory(outpostId));
    }

    /**
     * Publishes a new outpost so other servers can register a cross-server stub.
     */
    public void publishOutpostCreate(Outpost outpost) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.add("data", PersistenceUtils.buildOutpostDataJson(outpost));
        publish(CH_OUTPOST_CREATE, json);
    }

    private void handleOutpostCreate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        JsonObject data = json.getAsJsonObject("data");
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                PersistenceUtils.loadSingleOutpostFromJson(data));
    }

    /**
     * Publishes an outpost update (rename, icon change, chunk change) to keep stubs in sync.
     */
    public void publishOutpostUpdate(Outpost outpost) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.add("data", PersistenceUtils.buildOutpostDataJson(outpost));
        publish(CH_OUTPOST_UPDATE, json);
    }

    private void handleOutpostUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        JsonObject data = json.getAsJsonObject("data");
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            java.util.UUID outpostId = java.util.UUID.fromString(data.get("id").getAsString());
            Outpost existing = OutpostUtils.getOutpostById(outpostId);
            if (existing == null) {
                // Not yet in memory - register it as a new stub
                PersistenceUtils.loadSingleOutpostFromJson(data);
                return;
            }
            existing.setName(data.get("name").getAsString());
            existing.setBoughtChunks(data.has("boughtChunks") ? data.get("boughtChunks").getAsInt() : existing.getBoughtChunks());
            if (data.has("icon")) {
                try {
                    existing.setIcon(org.bukkit.Material.valueOf(data.get("icon").getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (data.has("chunks")) {
                int newCount = data.getAsJsonArray("chunks").size();
                existing.setStoredChunkCount(newCount);
            }
        });
    }

    /**
     * Notifies other servers that a player's job list changed so their in-memory copy is
     * refreshed from MySQL immediately. This prevents the periodic saveAllJobData task on
     * the receiving server from overwriting the updated job list with stale [] data.
     *
     * @param uuid The UUID of the player whose jobs changed.
     */
    public void publishJobUpdate(UUID uuid) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        publish(CH_JOB_UPDATE, json);
    }

    private void handleJobUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        // If this player is currently online on this server, reload their job data from MySQL
        // so the updated list is reflected in memory and won't be overwritten by the next
        // periodic save.
        if (Bukkit.getPlayer(uuid) != null) {
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                    () -> PersistenceUtils.loadJobDataForPlayer(uuid));
        }
    }

    /**
     * Notifies other servers that a vote was awarded so they can update their in-memory vote list.
     * Called after addVote() and the DB sync on the server that received the VotifierEvent.
     */
    public void publishVoteAward(UUID uuid, int amount, long timestamp) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("amount", amount);
        json.addProperty("timestamp", timestamp);
        publish(CH_VOTE_AWARD, json);
    }

    private void handleVoteAward(JsonObject json) {
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        int amount = json.get("amount").getAsInt();
        long timestamp = json.get("timestamp").getAsLong();
        AranarthUtils.addVote(new AranarthVote(uuid, amount, timestamp));
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[VOTE] Received cross-server vote award for " + uuid + ": +" + amount + " points");
    }

    /**
     * Tells the remote server to deliver a vote key directly to the player's inventory.
     */
    public void publishVoteKey(UUID uuid) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        publish(CH_VOTE_KEY, json);
    }

    private void handleVoteKey(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            AranarthUtils.addPendingVoteKeys(uuid, 1);
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[VOTE] Received cross-server vote key for " + uuid + " but player is not online - stored as pending");
            if (DatabaseManager.isActive()) {
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                        () -> PersistenceUtils.syncVoteKeysForPlayerToDatabase(uuid));
            }
            return;
        }
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            // Re-check on main thread - player may have logged off between the initial check and now
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                AranarthUtils.addPendingVoteKeys(uuid, 1);
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[VOTE] Remote key delivery: player " + uuid + " logged off before key could be given - stored as pending");
                if (DatabaseManager.isActive()) {
                    Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                            () -> PersistenceUtils.syncVoteKeysForPlayerToDatabase(uuid));
                }
                return;
            }
            String worldName = onlinePlayer.getWorld().getName();
            boolean validWorld = worldName.startsWith("world") || AranarthUtils.isSmpWorld(worldName)
                    || worldName.startsWith("resource") || worldName.startsWith("spawn");
            if (validWorld) {
                ItemStack key = new KeyVote().getItem();
                HashMap<Integer, ItemStack> remainder = onlinePlayer.getInventory().addItem(key);
                if (!remainder.isEmpty()) {
                    AranarthUtils.addPendingVoteKeys(uuid, 1);
                    onlinePlayer.sendMessage(ChatUtils.chatMessage("&7Your inventory was full! &7Use &e/keyclaim &7to obtain your vote key!"));
                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[VOTE] Remote key delivery: inventory full for " + onlinePlayer.getName() + " - stored as pending");
                } else {
                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[VOTE] Remote key delivery: key given directly to " + onlinePlayer.getName() + " in " + worldName);
                }
            } else {
                AranarthUtils.addPendingVoteKeys(uuid, 1);
                onlinePlayer.sendMessage(ChatUtils.chatMessage("&7You cannot receive crate keys here! &7Use &e/keyclaim &7to obtain your vote key!"));
                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[VOTE] Remote key delivery: " + onlinePlayer.getName() + " is in invalid world (" + worldName + ") - stored as pending");
            }
            if (DatabaseManager.isActive()) {
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                        () -> PersistenceUtils.syncVoteKeysForPlayerToDatabase(uuid));
            }
        });
    }

    /**
     * Tells the receiver's server to credit vote points transferred from another player.
     *
     * @param receiverUuid   The UUID of the player receiving the points.
     * @param amount         The number of vote points being transferred.
     * @param timestamp      The timestamp of the synthetic vote entry.
     * @param senderNickname The display name of the sending player.
     */
    public void publishVpTransfer(UUID receiverUuid, int amount, long timestamp, String senderNickname) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("receiverUuid", receiverUuid.toString());
        json.addProperty("amount", amount);
        json.addProperty("timestamp", timestamp);
        json.addProperty("senderNickname", senderNickname);
        publish(CH_VP_TRANSFER, json);
    }

    private void handleVpTransfer(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID receiverUuid = UUID.fromString(json.get("receiverUuid").getAsString());
        int amount = json.get("amount").getAsInt();
        long timestamp = json.get("timestamp").getAsLong();
        String senderNickname = json.get("senderNickname").getAsString();
        AranarthUtils.addVote(new AranarthVote(receiverUuid, amount, timestamp));
        Player receiver = Bukkit.getPlayer(receiverUuid);
        if (receiver != null) {
            receiver.sendMessage(ChatUtils.chatMessage("&7You have received &e" + amount + " &7vote points from &e" + senderNickname + "&7."));
        }
    }

    /**
     * Requests a snapshot of a remote player's inventory and registers the viewer as a live watcher.
     */
    public void publishInvseeRequest(UUID viewerUuid, UUID targetUuid) {
        JsonObject json = new JsonObject();
        json.addProperty("fromServer", thisServer);
        json.addProperty("viewerUuid", viewerUuid.toString());
        json.addProperty("targetUuid", targetUuid.toString());
        publish(CH_INVSEE_REQUEST, json);
    }

    /**
     * Notifies the target's server that a viewer has closed their remote invsee GUI.
     */
    public void publishInvseeUnwatch(UUID viewerUuid, UUID targetUuid) {
        JsonObject json = new JsonObject();
        json.addProperty("viewerUuid", viewerUuid.toString());
        json.addProperty("targetUuid", targetUuid.toString());
        publish(CH_INVSEE_UNWATCH, json);
    }

    /**
     * Publishes a live inventory update for all remote viewers watching the given target.
     * Must be called on the main thread.
     */
    public void publishRemoteInvseeUpdate(Player target) {
        Set<UUID> watchers = remoteInvseeWatchers.get(target.getUniqueId());
        if (watchers == null || watchers.isEmpty()) {
            return;
        }
        if (!target.isOnline()) {
            remoteInvseeWatchers.remove(target.getUniqueId());
            return;
        }

        try {
            String serialized = com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayToBase64(snapshotGuiItems(target));
            JsonObject json = new JsonObject();
            json.addProperty("targetUuid", target.getUniqueId().toString());
            json.addProperty("items", serialized);
            publish(CH_INVSEE_UPDATE, json);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                    + "Failed to serialize remote invsee update for " + target.getName() + ": " + e.getMessage());
        }
    }

    public boolean hasRemoteWatchers(UUID targetUuid) {
        Set<UUID> watchers = remoteInvseeWatchers.get(targetUuid);
        return watchers != null && !watchers.isEmpty();
    }

    /**
     * Received on the server that holds the target player.
     * Registers the viewer, serializes the current inventory, and sends it back.
     */
    private void handleInvseeRequest(JsonObject json) {
        String fromServer = json.get("fromServer").getAsString();
        if (fromServer.equals(thisServer)) {
            return;
        }

        UUID viewerUuid = UUID.fromString(json.get("viewerUuid").getAsString());
        UUID targetUuid = UUID.fromString(json.get("targetUuid").getAsString());

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            return;
        }

        // Register as a live watcher
        remoteInvseeWatchers.computeIfAbsent(targetUuid, k -> ConcurrentHashMap.newKeySet()).add(viewerUuid);

        try {
            String serialized = com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayToBase64(snapshotGuiItems(target));
            JsonObject response = new JsonObject();
            response.addProperty("targetServer", fromServer);
            response.addProperty("viewerUuid", viewerUuid.toString());
            response.addProperty("targetUuid", targetUuid.toString());
            response.addProperty("targetName", target.getName());
            response.addProperty("items", serialized);
            publish(CH_INVSEE_RESPONSE, response);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                    + "Failed to serialize inventory for remote invsee: " + e.getMessage());
        }
    }

    /**
     * Received on the target's server when a viewer closes their remote invsee GUI.
     */
    private void handleInvseeUnwatch(JsonObject json) {
        UUID viewerUuid = UUID.fromString(json.get("viewerUuid").getAsString());
        UUID targetUuid = UUID.fromString(json.get("targetUuid").getAsString());
        Set<UUID> watchers = remoteInvseeWatchers.get(targetUuid);
        if (watchers != null) {
            watchers.remove(viewerUuid);
            if (watchers.isEmpty()) {
                remoteInvseeWatchers.remove(targetUuid);
            }
        }
    }

    /**
     * Received on the viewer's server. Deserializes the inventory snapshot and opens the GUI.
     */
    private void handleInvseeResponse(JsonObject json) {
        String targetServer = json.get("targetServer").getAsString();
        if (!targetServer.equals(thisServer)) {
            return;
        }

        UUID viewerUuid = UUID.fromString(json.get("viewerUuid").getAsString());
        UUID targetUuid = UUID.fromString(json.get("targetUuid").getAsString());
        String targetName = json.get("targetName").getAsString();
        String serialized = json.get("items").getAsString();

        deserializeAndApply(serialized, targetUuid, guiItems -> {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer != null) {
                com.aearost.aranarthcore.gui.GuiInvsee.openRemote(viewer, targetUuid, targetName, guiItems);
            } else {
                // Viewer left before response arrived - send unwatch so target's server cleans up
                publishInvseeUnwatch(viewerUuid, targetUuid);
            }
        }, () -> {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer != null) {
                viewer.sendMessage(com.aearost.aranarthcore.utils.ChatUtils.chatMessage(
                        "&cFailed to load that player's inventory"));
            }
        });
    }

    /**
     * Received on the viewer's server. Updates the open remote invsee GUI in-place.
     */
    private void handleInvseeUpdate(JsonObject json) {
        UUID targetUuid = UUID.fromString(json.get("targetUuid").getAsString());
        if (!com.aearost.aranarthcore.gui.GuiInvsee.hasRemoteInvseeOpen(targetUuid)) {
            return;
        }
        String serialized = json.get("items").getAsString();
        deserializeAndApply(serialized, targetUuid,
                guiItems -> com.aearost.aranarthcore.gui.GuiInvsee.refreshRemoteForTarget(targetUuid, guiItems),
                null);
    }

    /**
     * Deserializes a base64 inventory snapshot async, then calls onSuccess/onError on the main thread.
     */
    private void deserializeAndApply(String serialized, UUID targetUuid,
                                     java.util.function.Consumer<org.bukkit.inventory.ItemStack[]> onSuccess,
                                     Runnable onError) {
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                org.bukkit.inventory.ItemStack[] guiItems =
                        com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayFromBase64(serialized);
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> onSuccess.accept(guiItems));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                        + "Failed to deserialize remote invsee for " + targetUuid + ": " + e.getMessage());
                if (onError != null) {
                    Bukkit.getScheduler().runTask(AranarthCore.getInstance(), onError);
                }
            }
        });
    }

    /**
     * Builds the 45-slot GUI item array from a player's current inventory.
     * Items are cloned to produce a safe snapshot. Must be called on the main thread.
     */
    private static org.bukkit.inventory.ItemStack[] snapshotGuiItems(Player target) {
        org.bukkit.inventory.ItemStack[] items = new org.bukkit.inventory.ItemStack[45];
        items[1] = clone(target.getInventory().getHelmet());
        items[2] = clone(target.getInventory().getChestplate());
        items[3] = clone(target.getInventory().getLeggings());
        items[4] = clone(target.getInventory().getBoots());
        items[6] = clone(target.getInventory().getItemInOffHand());
        for (int i = 9; i <= 35; i++) items[i] = clone(target.getInventory().getItem(i));
        for (int i = 0; i <= 8; i++) items[i + 36] = clone(target.getInventory().getItem(i));
        return items;
    }

    private static org.bukkit.inventory.ItemStack clone(org.bukkit.inventory.ItemStack item) {
        return (item == null) ? null : item.clone();
    }

    /**
     * Notifies other servers that a dominion has sent a diplomacy request (ally/truce/neutral)
     * so the pending request is added to the target dominion's in-memory list and members
     * on those servers are notified.
     *
     * @param targetDominionId    The UUID of the dominion receiving the request.
     * @param requesterLeaderUUID The leader UUID of the dominion sending the request.
     * @param type                "ally", "truce", or "neutral".
     */
    public void publishDominionDiploRequest(UUID targetDominionId, UUID requesterLeaderUUID, String type) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("targetDominionId", targetDominionId.toString());
        json.addProperty("requesterLeaderUUID", requesterLeaderUUID.toString());
        json.addProperty("type", type);
        publish(CH_DOMINION_DIPLO_REQUEST, json);
    }

    private void handleDominionDiploRequest(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID targetDominionId = UUID.fromString(json.get("targetDominionId").getAsString());
        UUID requesterLeaderUUID = UUID.fromString(json.get("requesterLeaderUUID").getAsString());
        String type = json.get("type").getAsString();

        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            Dominion targetDominion = DominionUtils.getDominionById(targetDominionId);
            if (targetDominion == null) {
                return;
            }
            Dominion requesterDominion = DominionUtils.getPlayerDominion(requesterLeaderUUID);
            if (requesterDominion == null) {
                return;
            }

            switch (type) {
                case "ally" -> {
                    if (!targetDominion.getAllianceRequests().contains(requesterLeaderUUID)) {
                        targetDominion.getAllianceRequests().add(requesterLeaderUUID);
                    }
                }
                case "truce" -> {
                    if (!targetDominion.getTruceRequests().contains(requesterLeaderUUID)) {
                        targetDominion.getTruceRequests().add(requesterLeaderUUID);
                    }
                }
                case "neutral" -> {
                    if (!targetDominion.getNeutralRequests().contains(requesterLeaderUUID)) {
                        targetDominion.getNeutralRequests().add(requesterLeaderUUID);
                    }
                }
            }

            String targetMsg = switch (type) {
                case "ally" ->
                        "&e" + requesterDominion.getName() + " &7has requested an &5Alliance &7with your Dominion";
                case "truce" -> "&e" + requesterDominion.getName() + " &7has requested a &dTruce &7with your Dominion";
                default -> "&e" + requesterDominion.getName() + " &7has requested &fNeutrality &7with your Dominion";
            };
            String requesterMsg = switch (type) {
                case "ally" -> "&7Your Dominion has requested an &5Alliance &7with &e" + targetDominion.getName();
                case "truce" -> "&7Your Dominion has requested a &dTruce &7with &e" + targetDominion.getName();
                default -> "&7Your Dominion has requested &fNeutrality &7with &e" + targetDominion.getName();
            };

            for (Player p : Bukkit.getOnlinePlayers()) {
                int domVol = AranarthUtils.getPlayer(p.getUniqueId()).getDominionSoundVolume();
                if (targetDominion.getMembers().contains(p.getUniqueId())) {
                    if (domVol > 0) {
                        p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                    }
                    p.sendMessage(ChatUtils.chatMessage(targetMsg));
                } else if (requesterDominion.getMembers().contains(p.getUniqueId())) {
                    if (domVol > 0) {
                        p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                    }
                    p.sendMessage(ChatUtils.chatMessage(requesterMsg));
                }
            }
        });
    }

    /**
     * Notifies other servers that a dominion relation has been finalized so they can update
     * their in-memory state. The global broadcast message (if any) is sent separately via
     * {@link #publishBroadcast}.
     *
     * @param dominionAId  UUID of the first dominion.
     * @param dominionBId  UUID of the second dominion.
     * @param relationType "ally", "truce", "enemy", "neutral", or "neutral_members"
     *                     ("neutral_members" means immediate neutrality from ally/truce - sends
     *                     member-specific notifications instead of a global broadcast).
     */
    public void publishDominionRelationUpdate(UUID dominionAId, UUID dominionBId, String relationType) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("dominionAId", dominionAId.toString());
        json.addProperty("dominionBId", dominionBId.toString());
        json.addProperty("relationType", relationType);
        publish(CH_DOMINION_RELATION_UPDATE, json);
    }

    private void handleDominionRelationUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID dominionAId = UUID.fromString(json.get("dominionAId").getAsString());
        UUID dominionBId = UUID.fromString(json.get("dominionBId").getAsString());
        String relationType = json.get("relationType").getAsString();

        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            Dominion a = DominionUtils.getDominionById(dominionAId);
            Dominion b = DominionUtils.getDominionById(dominionBId);
            if (a == null || b == null) {
                return;
            }

            // Clear all existing relations between the two dominions
            a.getAllianceRequests().remove(b.getLeader());
            a.getTruceRequests().remove(b.getLeader());
            a.getNeutralRequests().remove(b.getLeader());
            a.getAllied().remove(b.getLeader());
            a.getTruced().remove(b.getLeader());
            a.getEnemied().remove(b.getLeader());
            b.getAllianceRequests().remove(a.getLeader());
            b.getTruceRequests().remove(a.getLeader());
            b.getNeutralRequests().remove(a.getLeader());
            b.getAllied().remove(a.getLeader());
            b.getTruced().remove(a.getLeader());
            b.getEnemied().remove(a.getLeader());

            switch (relationType) {
                case "ally" -> {
                    a.getAllied().add(b.getLeader());
                    b.getAllied().add(a.getLeader());
                }
                case "truce" -> {
                    a.getTruced().add(b.getLeader());
                    b.getTruced().add(a.getLeader());
                }
                case "enemy" -> {
                    a.getEnemied().add(b.getLeader());
                    b.getEnemied().add(a.getLeader());
                }
                // "neutral" and "neutral_members" just leave both sides fully cleared
            }

            DominionUtils.updateDominion(a);
            DominionUtils.updateDominion(b);

            // "neutral_members" skips the global broadcast (handled locally) but needs
            // member-specific horn + chat notifications on this server.
            if (relationType.equals("neutral_members")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    int domVol = AranarthUtils.getPlayer(p.getUniqueId()).getDominionSoundVolume();
                    if (a.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                        }
                        p.sendMessage(ChatUtils.chatMessage("&7Your Dominion has become &fNeutral &7with &e" + b.getName()));
                    } else if (b.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                        }
                        p.sendMessage(ChatUtils.chatMessage("&7Your Dominion has become &fNeutral &7with &e" + a.getName()));
                    }
                }
            }
        });
    }

    /**
     * Notifies other servers that the conquest/rebellion state of one or two dominions has
     * changed, so their in-memory copies can be updated.
     *
     * <p>The {@code type} string identifies the event for cross-server member notifications:
     * <ul>
     *   <li>"conquer_request" - a=conqueror, b=defender</li>
     *   <li>"rebel_request"   - a=rebel,     b=conqueror</li>
     *   <li>All other types  - no extra notifications (global broadcast is sent via publishBroadcast)</li>
     * </ul>
     *
     * @param type Event type string.
     * @param a    First affected dominion (always present).
     * @param b    Second affected dominion, or null if only one dominion changed.
     */
    public void publishDominionConquestUpdate(String type, Dominion a, Dominion b) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("type", type);
        json.addProperty("dominionAId", a.getId().toString());
        addConquestState(json, a, "a");
        if (b != null) {
            json.addProperty("dominionBId", b.getId().toString());
            addConquestState(json, b, "b");
        }
        publish(CH_DOMINION_CONQUEST_UPDATE, json);
    }

    /**
     * Serialises the conquest/rebellion fields of {@code d} into {@code json} under the given prefix.
     */
    private void addConquestState(JsonObject json, Dominion d, String prefix) {
        json.addProperty(prefix + "ConqueredRequest",
                d.getConqueredRequest() == null ? "null" : d.getConqueredRequest().toString());
        json.addProperty(prefix + "ConqueredRequestTs", d.getConqueredRequestTimestamp());
        json.addProperty(prefix + "ConqueredRequestDefenderLastSeen", d.getConqueredRequestDefenderLastSeen());

        StringBuilder conquered = new StringBuilder();
        for (UUID u : d.getConquered()) {
            if (!conquered.isEmpty()) {
                conquered.append(",");
            }
            conquered.append(u);
        }
        json.addProperty(prefix + "Conquered", conquered.toString());
        json.addProperty(prefix + "LastConquerAttemptTs", d.getLastConquerAttemptTimestamp());
        json.addProperty(prefix + "ConqueredTs", d.getConqueredTimestamp());

        json.addProperty(prefix + "RebelRequest",
                d.getRebelRequest() == null ? "null" : d.getRebelRequest().toString());
        json.addProperty(prefix + "RebelRequestTs", d.getRebelRequestTimestamp());
        json.addProperty(prefix + "RebelRequestConquerorLastSeen", d.getRebelRequestConquerorLastSeen());
        json.addProperty(prefix + "LastRebelAttemptTs", d.getLastRebelAttemptTimestamp());
    }

    private void handleDominionConquestUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID dominionAId = UUID.fromString(json.get("dominionAId").getAsString());
        String type = json.get("type").getAsString();

        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            Dominion a = DominionUtils.getDominionById(dominionAId);
            if (a == null) {
                return;
            }
            applyConquestState(a, json, "a");
            DominionUtils.updateDominion(a);

            Dominion b = null;
            if (json.has("dominionBId")) {
                UUID dominionBId = UUID.fromString(json.get("dominionBId").getAsString());
                b = DominionUtils.getDominionById(dominionBId);
                if (b != null) {
                    applyConquestState(b, json, "b");
                    DominionUtils.updateDominion(b);
                }
            }

            sendConquestMemberNotifications(type, a, b);
        });
    }

    /**
     * Reads the conquest/rebellion state fields from {@code json} and applies them to {@code d}.
     */
    private void applyConquestState(Dominion d, JsonObject json, String prefix) {
        String cr = json.get(prefix + "ConqueredRequest").getAsString();
        d.setConqueredRequest(cr.equals("null") ? null : UUID.fromString(cr));
        d.setConqueredRequestTimestamp(json.get(prefix + "ConqueredRequestTs").getAsLong());
        d.setConqueredRequestDefenderLastSeen(json.get(prefix + "ConqueredRequestDefenderLastSeen").getAsLong());

        List<UUID> conquered = new ArrayList<>();
        String conqueredStr = json.get(prefix + "Conquered").getAsString();
        if (!conqueredStr.isEmpty()) {
            for (String s : conqueredStr.split(",")) conquered.add(UUID.fromString(s));
        }
        d.setConquered(conquered);
        d.setLastConquerAttemptTimestamp(json.get(prefix + "LastConquerAttemptTs").getAsLong());
        d.setConqueredTimestamp(json.get(prefix + "ConqueredTs").getAsLong());

        String rr = json.get(prefix + "RebelRequest").getAsString();
        d.setRebelRequest(rr.equals("null") ? null : UUID.fromString(rr));
        d.setRebelRequestTimestamp(json.get(prefix + "RebelRequestTs").getAsLong());
        d.setRebelRequestConquerorLastSeen(json.get(prefix + "RebelRequestConquerorLastSeen").getAsLong());
        d.setLastRebelAttemptTimestamp(json.get(prefix + "LastRebelAttemptTs").getAsLong());
    }

    /**
     * Sends member-targeted sound+chat notifications on this server for conquest/rebellion events
     * that require them (conquer_request, rebel_request). Broadcast-style events are handled
     * by the CH_BROADCAST channel instead.
     */
    private void sendConquestMemberNotifications(String type, Dominion a, Dominion b) {
        if (b == null) {
            return;
        }
        switch (type) {
            case "conquer_request" -> {
                // a=conqueror, b=defender
                for (Player p : Bukkit.getOnlinePlayers()) {
                    int domVol = AranarthUtils.getPlayer(p.getUniqueId()).getDominionSoundVolume();
                    if (a.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_2, 2F * (domVol / 100f), 1F);
                        }
                        p.sendMessage(ChatUtils.chatMessage("&4Your Dominion is attempting to conquer &e" + b.getName()));
                    } else if (b.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_4, 2F * (domVol / 100f), 1F);
                        }
                        p.sendMessage(ChatUtils.chatMessage("&e" + a.getName() + " &4is attempting to conquer your Dominion"));
                        p.sendMessage(ChatUtils.chatMessage("&4Your Dominion will automatically be conquered if nobody logs on for 3 days during the conquest!"));
                    }
                }
            }
            case "rebel_request" -> {
                // a=rebel, b=conqueror
                for (Player p : Bukkit.getOnlinePlayers()) {
                    int domVol = AranarthUtils.getPlayer(p.getUniqueId()).getDominionSoundVolume();
                    if (a.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_1, 2F * (domVol / 100f), 1F);
                        }
                        p.sendMessage(ChatUtils.chatMessage("&5Your Dominion has started a rebellion against &e" + b.getName()));
                    } else if (b.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_1, 2F * (domVol / 100f), 1F);
                        }
                        p.sendMessage(ChatUtils.chatMessage("&e" + a.getName() + " &5has started a rebellion against your Dominion!"));
                        p.sendMessage(ChatUtils.chatMessage("&5Use &e/dominion retreat " + ChatUtils.stripColorFormatting(a.getName()) + " &5to release them of your conquest"));
                        p.sendMessage(ChatUtils.chatMessage("They will be freed if your Dominion goes 3 days without logging on"));
                    }
                }
            }
        }
    }

    private void handleChatGameStart(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        String scrambled = json.get("scrambled").getAsString();
        String answer = json.get("answer").getAsString();
        ChatGameUtils.applyNetworkGameStart(scrambled, answer, originServer);
    }

    private void handleChatGameWin(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

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
        if (originServer.equals(thisServer)) {
            return;
        }

        String answer = json.get("answer").getAsString();
        ChatGameUtils.applyNetworkGameExpire(AranarthCore.getInstance(), answer);
    }

    private void handleChatGameClaim(JsonObject json) {
        String claimingServer = json.get("server").getAsString();
        if (claimingServer.equals(thisServer)) {
            return; // ignore own claims
        }

        java.util.UUID playerUUID = java.util.UUID.fromString(json.get("playerUUID").getAsString());
        String playerNickname = json.get("playerNickname").getAsString();
        double elapsedSeconds = json.get("elapsedSeconds").getAsDouble();
        ChatGameUtils.processRemoteClaim(AranarthCore.getInstance(), playerUUID, playerNickname, elapsedSeconds);
    }

    // -------------------------------------------------------------------------
    // Internal publish helper
    // -------------------------------------------------------------------------

    /**
     * Received on the server that holds the sentinel entities.
     * Captures their attributes, removes them, and publishes spawn data back to the player's server.
     */
    private void handleSentinelSummon(JsonObject json) {
        String targetServer = json.get("targetServer").getAsString();
        if (!targetServer.equals(thisServer)) {
            return;
        }

        String originServer = json.get("server").getAsString();
        UUID playerUuid = UUID.fromString(json.get("playerUuid").getAsString());
        org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(json.get("sentinelType").getAsString());
        String targetWorld = json.get("targetWorld").getAsString();
        double targetX = json.get("targetX").getAsDouble();
        double targetY = json.get("targetY").getAsDouble();
        double targetZ = json.get("targetZ").getAsDouble();

        com.google.gson.JsonArray spawnData = new com.google.gson.JsonArray();
        for (com.google.gson.JsonElement el : json.get("entityUuids").getAsJsonArray()) {
            UUID entityUuid = UUID.fromString(el.getAsString());
            org.bukkit.entity.Entity entity = Bukkit.getEntity(entityUuid);
            if (!(entity instanceof org.bukkit.entity.LivingEntity living) || entity.isDead()) {
                continue;
            }

            JsonObject data = new JsonObject();
            data.addProperty("oldUuid", entityUuid.toString());
            data.addProperty("health", living.getHealth());

            if (entity instanceof org.bukkit.entity.Wolf wolf) {
                data.addProperty("collarColor", wolf.getCollarColor().name());
            } else if (entity instanceof org.bukkit.entity.AbstractHorse horse) {
                org.bukkit.attribute.AttributeInstance speedAttr = horse.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                org.bukkit.attribute.AttributeInstance jumpAttr = horse.getAttribute(org.bukkit.attribute.Attribute.JUMP_STRENGTH);
                org.bukkit.attribute.AttributeInstance maxHpAttr = horse.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                data.addProperty("movementSpeed", speedAttr != null ? speedAttr.getBaseValue() : 0.225);
                data.addProperty("jumpStrength", jumpAttr != null ? jumpAttr.getBaseValue() : 0.7);
                data.addProperty("maxHealth", maxHpAttr != null ? maxHpAttr.getBaseValue() : 20.0);
                if (entity instanceof org.bukkit.entity.Horse h) {
                    data.addProperty("horseColor", h.getColor().name());
                    data.addProperty("horseStyle", h.getStyle().name());
                    org.bukkit.inventory.ItemStack saddle = h.getInventory().getSaddle();
                    org.bukkit.inventory.ItemStack armor = h.getInventory().getArmor();
                    if (saddle != null && saddle.getType() != org.bukkit.Material.AIR) {
                        data.addProperty("saddleItem", java.util.Base64.getEncoder().encodeToString(saddle.serializeAsBytes()));
                    }
                    if (armor != null && armor.getType() != org.bukkit.Material.AIR) {
                        data.addProperty("armorItem", java.util.Base64.getEncoder().encodeToString(armor.serializeAsBytes()));
                    }
                }
            }
            entity.remove();
            spawnData.add(data);
        }

        if (spawnData.isEmpty()) {
            return;
        }

        JsonObject spawnJson = new JsonObject();
        spawnJson.addProperty("server", thisServer);
        spawnJson.addProperty("targetServer", originServer);
        spawnJson.addProperty("playerUuid", playerUuid.toString());
        spawnJson.addProperty("sentinelType", type.name());
        spawnJson.addProperty("targetWorld", targetWorld);
        spawnJson.addProperty("targetX", targetX);
        spawnJson.addProperty("targetY", targetY);
        spawnJson.addProperty("targetZ", targetZ);
        spawnJson.add("entities", spawnData);
        publish(CH_SENTINEL_SPAWN, spawnJson);
    }

    /**
     * Received on the player's server. Spawns the transferred sentinel entities,
     * updates UUIDs in player data, and plays the summon effects.
     */
    private void handleSentinelSpawn(JsonObject json) {
        String targetServer = json.get("targetServer").getAsString();
        if (!targetServer.equals(thisServer)) {
            return;
        }

        UUID playerUuid = UUID.fromString(json.get("playerUuid").getAsString());
        org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(json.get("sentinelType").getAsString());
        String targetWorld = json.get("targetWorld").getAsString();
        double targetX = json.get("targetX").getAsDouble();
        double targetY = json.get("targetY").getAsDouble();
        double targetZ = json.get("targetZ").getAsDouble();

        AranarthPlayer ap = AranarthUtils.getPlayer(playerUuid);
        if (ap == null) {
            return;
        }

        org.bukkit.entity.Player player = Bukkit.getPlayer(playerUuid);
        World world = Bukkit.getWorld(targetWorld);
        Location spawnLoc = player != null ? player.getLocation()
                : (world != null ? new Location(world, targetX, targetY, targetZ) : null);
        if (spawnLoc == null || spawnLoc.getWorld() == null) {
            return;
        }

        java.util.List<com.aearost.aranarthcore.objects.Sentinel> sentinels =
                ap.getSentinels().getOrDefault(type, new java.util.ArrayList<>());

        for (com.google.gson.JsonElement el : json.get("entities").getAsJsonArray()) {
            JsonObject data = el.getAsJsonObject();
            UUID oldUuid = UUID.fromString(data.get("oldUuid").getAsString());
            double health = data.get("health").getAsDouble();

            org.bukkit.entity.Entity spawned = spawnLoc.getWorld().spawnEntity(spawnLoc, type);

            if (spawned instanceof org.bukkit.entity.Wolf wolf) {
                wolf.setTamed(true);
                wolf.setOwner(Bukkit.getOfflinePlayer(playerUuid));
                wolf.setHealth(Math.min(health, wolf.getMaxHealth()));
                if (data.has("collarColor")) {
                    wolf.setCollarColor(org.bukkit.DyeColor.valueOf(data.get("collarColor").getAsString()));
                }
                wolf.setSitting(false);
            } else if (spawned instanceof org.bukkit.entity.AbstractHorse horse) {
                horse.setTamed(true);
                horse.setOwner(Bukkit.getOfflinePlayer(playerUuid));
                if (data.has("maxHealth")) {
                    horse.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(data.get("maxHealth").getAsDouble());
                }
                horse.setHealth(Math.min(health, horse.getMaxHealth()));
                if (data.has("movementSpeed")) {
                    horse.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(data.get("movementSpeed").getAsDouble());
                }
                if (data.has("jumpStrength")) {
                    horse.getAttribute(org.bukkit.attribute.Attribute.JUMP_STRENGTH).setBaseValue(data.get("jumpStrength").getAsDouble());
                }
                if (spawned instanceof org.bukkit.entity.Horse h) {
                    if (data.has("horseColor")) {
                        h.setColor(org.bukkit.entity.Horse.Color.valueOf(data.get("horseColor").getAsString()));
                    }
                    if (data.has("horseStyle")) {
                        h.setStyle(org.bukkit.entity.Horse.Style.valueOf(data.get("horseStyle").getAsString()));
                    }
                    if (data.has("saddleItem")) {
                        h.getInventory().setSaddle(org.bukkit.inventory.ItemStack.deserializeBytes(
                                java.util.Base64.getDecoder().decode(data.get("saddleItem").getAsString())));
                    }
                    if (data.has("armorItem")) {
                        h.getInventory().setArmor(org.bukkit.inventory.ItemStack.deserializeBytes(
                                java.util.Base64.getDecoder().decode(data.get("armorItem").getAsString())));
                    }
                }
            } else if (spawned instanceof org.bukkit.entity.IronGolem golem) {
                golem.setPlayerCreated(true);
            }

            for (com.aearost.aranarthcore.objects.Sentinel s : sentinels) {
                if (s.getUuid().equals(oldUuid)) {
                    s.setUuid(spawned.getUniqueId());
                    s.setServerName(thisServer);
                    s.setLocation(spawnLoc);
                    break;
                }
            }
        }

        ap.getSentinels().put(type, sentinels);
        AranarthUtils.setPlayer(playerUuid, ap);
        PersistenceUtils.syncSentinelsToDatabase();

        if (player != null) {
            int tpVol = AranarthUtils.getPlayer(player.getUniqueId()).getTeleportSoundVolume();
            if (tpVol > 0) {
                player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, tpVol / 100f, 0.9F);
            }
            player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, player.getEyeLocation(), 250, 3, 2, 3);
        }
    }

    /**
     * Received on the sentinel owner's server when the entity died on a remote server.
     * Removes the sentinel from the player's data.
     */
    private void handleSentinelDeath(JsonObject json) {
        UUID sentinelUuid = UUID.fromString(json.get("sentinelUuid").getAsString());
        org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(json.get("sentinelType").getAsString());

        for (Map.Entry<UUID, AranarthPlayer> entry : AranarthUtils.getAranarthPlayers().entrySet()) {
            AranarthPlayer ap = entry.getValue();
            if (ap.getSentinels() == null || ap.getSentinels().get(type) == null) {
                continue;
            }
            java.util.List<com.aearost.aranarthcore.objects.Sentinel> list = ap.getSentinels().get(type);
            com.aearost.aranarthcore.objects.Sentinel toRemove = null;
            for (com.aearost.aranarthcore.objects.Sentinel s : list) {
                if (s.getUuid().equals(sentinelUuid)) {
                    toRemove = s;
                    break;
                }
            }
            if (toRemove != null) {
                list.remove(toRemove);
                AranarthUtils.setPlayer(entry.getKey(), ap);
                break;
            }
        }
    }

    private void publish(String channel, JsonObject json) {
        final String payload = json.toString();
        publishExecutor.submit(() -> {
            try {
                db.publishMessage(channel, payload, thisServer);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                        + "DB publish failed on " + channel + ": " + e.getMessage());
            }
        });
    }
}
