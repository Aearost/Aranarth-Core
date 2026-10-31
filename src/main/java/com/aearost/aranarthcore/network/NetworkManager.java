package com.aearost.aranarthcore.network;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.abilities.airbending.spiritual.AstralProjection;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.PrefixStyle;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.enums.WorldEvent;
import com.aearost.aranarthcore.event.world.WorldEventManager;
import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.*;
import com.aearost.aranarthcore.utils.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
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
    public static final String CH_DOMINION_UPDATE = "aranarth:dominion_update";
    public static final String CH_DOMINION_LEVEL_OVERRIDE = "aranarth:dominion_level_override";
    public static final String CH_CHAT_GAME_START = "aranarth:chat_game_start";
    public static final String CH_CHAT_GAME_WIN = "aranarth:chat_game_win";
    public static final String CH_CHAT_GAME_EXPIRE = "aranarth:chat_game_expire";
    public static final String CH_CHAT_GAME_CLAIM = "aranarth:chat_game_claim";
    public static final String CH_DEATH = "aranarth:death";
    public static final String CH_BREW_UNLOCK = "aranarth:brew_unlock";
    public static final String CH_BALANCE_ADJUST = "aranarth:balance_adjust";
    public static final String CH_PAY_NOTIFY = "aranarth:pay_notify";
    public static final String CH_SUDO = "aranarth:sudo";
    public static final String CH_SENTINEL_SUMMON = "aranarth:sentinel_summon";
    public static final String CH_SENTINEL_SPAWN = "aranarth:sentinel_spawn";
    public static final String CH_SENTINEL_DEATH = "aranarth:sentinel_death";
    public static final String CH_OUTPOST_DISBAND = "aranarth:outpost_disband";
    public static final String CH_OUTPOST_CREATE = "aranarth:outpost_create";
    public static final String CH_OUTPOST_UPDATE = "aranarth:outpost_update";
    public static final String CH_CREATIVE_PLOT_UPDATE = "aranarth:creative_plot_update";
    public static final String CH_CREATIVE_PLOT_DELETE = "aranarth:creative_plot_delete";
    public static final String CH_CREATIVE_EXPAND = "aranarth:creative_expand";
    public static final String CH_JOB_UPDATE = "aranarth:job_update";
    public static final String CH_JOB_TOP_REQUEST = "aranarth:job_top_request";
    public static final String CH_JOB_TOP_RESPONSE = "aranarth:job_top_response";
    public static final String CH_MOUNT_UPDATE = "aranarth:mount_update";
    public static final String CH_VOTE_AWARD = "aranarth:vote_award";
    public static final String CH_VOTE_KEY = "aranarth:vote_key";
    public static final String CH_PURCHASE_KEY = "aranarth:purchase_key";
    public static final String CH_VP_TRANSFER = "aranarth:vp_transfer";
    public static final String CH_INVSEE_REQUEST = "aranarth:invsee_request";
    public static final String CH_INVSEE_RESPONSE = "aranarth:invsee_response";
    public static final String CH_INVSEE_UPDATE = "aranarth:invsee_update";
    public static final String CH_INVSEE_UNWATCH = "aranarth:invsee_unwatch";
    public static final String CH_RANK_UPDATE = "aranarth:rank_update";
    public static final String CH_PERK_UPDATE = "aranarth:perk_update";
    public static final String CH_MARKET_UPDATE = "aranarth:market_update";
    public static final String CH_PERM_RELOAD = "aranarth:perm_reload";
    public static final String CH_CONFIG_RELOAD = "aranarth:config_reload";
    public static final String CH_TRADE_INVITE = "aranarth:trade_invite";
    public static final String CH_TRADE_ACCEPT = "aranarth:trade_accept";
    public static final String CH_TRADE_CANCEL = "aranarth:trade_cancel";
    public static final String CH_TRADE_ITEMS = "aranarth:trade_items";
    public static final String CH_TRADE_MONEY = "aranarth:trade_money";
    public static final String CH_TRADE_CONFIRM = "aranarth:trade_confirm";
    public static final String CH_TRADE_EXECUTE = "aranarth:trade_execute";
    public static final String CH_TRADE_RESULT = "aranarth:trade_result";
    public static final String CH_PUNISHMENT = "aranarth:punishment";
    public static final String CH_PRONOUNS_UPDATE = "aranarth:pronouns_update";
    public static final String CH_PREFIX_UPDATE = "aranarth:prefix_update";
    public static final String CH_NICKNAME_UPDATE = "aranarth:nickname_update";
    public static final String CH_VANISH_UPDATE = "aranarth:vanish_update";
    public static final String CH_CLEAR_CHAT = "aranarth:clear_chat";
    public static final String CH_DATE_SET = "aranarth:date_set";
    public static final String CH_WORLD_EVENT_START = "aranarth:world_event_start";
    public static final String CH_PING_UPDATE = "aranarth:ping_update";
    public static final String CH_HEARTBEAT = "aranarth:heartbeat";
    public static final String CH_RESOURCE_WORLD_LOCK = "aranarth:resource_world_lock";
    public static final String CH_AVATAR_UPDATE = "aranarth:avatar_update";
    public static final String CH_ORIGINAL_PLAYER_UPDATE = "aranarth:original_player_update";
    public static final String CH_PLAYER_CREATED = "aranarth:player_created";
    public static final String CH_SHOP_STOCK = "aranarth:shop_stock";
    // Temp-data key prefixes
    private static final String KEY_PENDING_TP = "pending_tp:";
    private static final String KEY_RETURN_LOC = "return_loc:";
    private static final String KEY_LAST_MSG = "last_msg:";
    public static final String KEY_SMP_RESTART_INV = "smp_restart_inv:";
    public static final String KEY_SERVER_STARTED = "server_started:";
    public static final String KEY_HEARTBEAT = "heartbeat:";
    private static final int HEARTBEAT_TTL_SECONDS = 90;
    private static NetworkManager instance;
    private final String thisServer;
    private final DatabaseManager db;
    private final Gson gson = new Gson();
    /**
     * Players currently online on OTHER servers.
     */
    private final Map<UUID, NetworkPlayer> remoteRoster = new ConcurrentHashMap<>();
    /**
     * Players that landed on this server as a fallback (other server restarting) mapped to when it happened.
     */
    private final Map<UUID, Long> fallbackLandings = new ConcurrentHashMap<>();

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
    private final Set<UUID> lockedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> nonAuthoritativePlayers = ConcurrentHashMap.newKeySet();
    private static final long TRANSFER_TIMEOUT_TICKS = 400L;
    /**
     * Players whose cross-server join should suppress DiscordSRV's join announcement.
     * Populated in PlayerServerJoinListener, consumed in DiscordChatListener.
     */
    private final Set<UUID> crossServerJoinPlayers = ConcurrentHashMap.newKeySet();
    /**
     * Last time (epoch ms) a heartbeat was received from each remote server name.
     * Used to determine whether a peer server is currently online.
     */
    private final Map<String, Long> serverLastSeen = new ConcurrentHashMap<>();
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

    /**
     * Callbacks waiting on the in-flight /jobs top request for the other server's live job data.
     * Only touched on the main thread.
     */
    private final List<Consumer<Map<UUID, String>>> pendingJobTopCallbacks = new ArrayList<>();
    private String pendingJobTopRequestId = null;
    private Map<UUID, String> cachedRemoteJobData = null;
    private long cachedRemoteJobDataAt = 0L;
    private long lastProcessedMessageId;
    private BukkitTask pollingTask;
    private BukkitTask cleanupTask;
    private BukkitTask smpSnapshotTask;
    private BukkitTask heartbeatTask;
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
        if (AranarthCore.isSmpServer()) {
            // Lets Survival tell a fallback snapshot from SMP's previous run apart from one written
            // while SMP is down, so players are only kept on Survival while SMP is unavailable
            db.saveTempData(KEY_SERVER_STARTED + thisServer, String.valueOf(System.currentTimeMillis()), 30 * 24 * 60 * 60);
            startSmpFallbackSnapshots();
        }
        startHeartbeat();
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
     * Periodically snapshots every online SMP player's inventory + stats to MySQL so
     * that Survival can restore them if Velocity routes a player there as a fallback
     * during an SMP restart or crash. TTL is 10 minutes; refreshed every 30 seconds.
     */
    private void startSmpFallbackSnapshots() {
        // Collect inventory data synchronously on the main thread, then write async.
        smpSnapshotTask = Bukkit.getScheduler().runTaskTimer(
                AranarthCore.getInstance(),
                () -> {
                    if (!DatabaseManager.isActive()) {
                        return;
                    }
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        try {
                            String key = KEY_SMP_RESTART_INV + p.getUniqueId();
                            String json = buildFallbackSnapshot(p);
                            if (json == null) {
                                continue;
                            }
                            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                                    () -> db.saveTempData(key, json, 600)); // 10-min TTL
                        } catch (Exception e) {
                            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                                    + "[SmpSnapshot] Failed to snapshot " + p.getName() + ": " + e.getMessage());
                        }
                    }
                },
                600L,  // 30s initial delay
                600L   // every 30s
        );
    }

    /**
     * Sends a lightweight heartbeat to all other servers every 2 minutes so they can
     * determine whether this server is currently online via {@link #isServerOnline(String)}.
     */
    private void startHeartbeat() {
        heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                AranarthCore.getInstance(),
                this::publishHeartbeat,
                0L,    // fire immediately so peer servers see us as online within one polling cycle
                600L   // every 30 seconds
        );
    }

    /**
     * Publishes a heartbeat message so other servers know this server is online.
     */
    public void publishHeartbeat() {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        publish(CH_HEARTBEAT, json);
        // Also keep a heartbeat row in MySQL, since a server that has just started misses heartbeat
        // messages sent before its own NetworkManager came up
        db.saveTempData(KEY_HEARTBEAT + thisServer, String.valueOf(System.currentTimeMillis()), HEARTBEAT_TTL_SECONDS);
    }

    /**
     * Returns true if the given server is online according to its heartbeat row in MySQL. The row is
     * refreshed every 30 seconds, expires shortly after a crash and is deleted on shutdown, so a
     * restarting server is reported as offline right away. Blocks on a DB read, call off the main thread.
     */
    public boolean isServerOnlineInDatabase(String serverName) {
        return db.loadTempData(KEY_HEARTBEAT + serverName) != null;
    }

    /**
     * Returns true if the given server name has sent a heartbeat within the last 5 minutes.
     * Uses a 5-minute window against a 2-minute heartbeat interval, giving 2.5 cycles of
     * safety margin. Returns false if no heartbeat has ever been received (e.g. right after
     * Survival restarts while SMP is already running - the first heartbeat arrives within 2 min).
     */
    public boolean isServerOnline(String serverName) {
        Long lastSeen = serverLastSeen.get(serverName);
        return lastSeen != null && System.currentTimeMillis() - lastSeen < 5 * 60 * 1000L;
    }

    /**
     * Serializes the player's current inventory + stats into a JSON string suitable for
     * storing as an SMP fallback snapshot. Must be called from the main thread.
     *
     * @return The snapshot, or null if the player has no survival inventory to snapshot.
     */
    private String buildFallbackSnapshot(Player p) {
        // The inventory on this server is not the player's while they wait to be routed elsewhere
        if (isNonAuthoritative(p.getUniqueId())) {
            return null;
        }
        JsonObject snap = new JsonObject();
        snap.addProperty("ts", System.currentTimeMillis());
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(p.getUniqueId());
        if (AranarthUtils.isSurvivalWorld(p.getWorld().getName()) || aranarthPlayer == null) {
            snap.addProperty("inventory", ItemUtils.itemStackArrayToBase64(p.getInventory().getContents()));
            snap.addProperty("health", p.getHealth());
            snap.addProperty("food", p.getFoodLevel());
            snap.addProperty("saturation", p.getSaturation());
            snap.addProperty("expLevel", p.getLevel());
            snap.addProperty("expProgress", p.getExp());
            snap.addProperty("effects", AranarthUtils.serializePotionEffects(p));
        } else {
            // In a world with another inventory (i.e SMP Creative), so the survival inventory saved when they left is used
            if (aranarthPlayer.getSurvivalInventory().isEmpty()) {
                return null;
            }
            snap.addProperty("inventory", aranarthPlayer.getSurvivalInventory());
            snap.addProperty("health", aranarthPlayer.getSurvivalHealth());
            snap.addProperty("food", aranarthPlayer.getSurvivalFoodLevel());
            snap.addProperty("saturation", aranarthPlayer.getSurvivalSaturation());
            snap.addProperty("expLevel", aranarthPlayer.getSurvivalExpLevel());
            snap.addProperty("expProgress", aranarthPlayer.getSurvivalExpProgress());
            snap.addProperty("effects", aranarthPlayer.getSurvivalEffects());
        }
        // The ender chest is shared by every world
        snap.addProperty("enderChest", ItemUtils.itemStackArrayToBase64(p.getEnderChest().getContents()));
        // Carry the full player row (homes, balance, etc.) so Survival does not keep using its
        // stale copy - and later write it back to MySQL - when the player lands there as a fallback
        String rawRow = PersistenceUtils.buildPlayerRowForTransfer(p.getUniqueId());
        if (rawRow != null) {
            snap.addProperty("raw", rawRow);
        }
        return snap.toString();
    }

    /**
     * Writes a one-off SMP fallback snapshot for this player synchronously (no async
     * dispatch). Used during shutdown where async tasks may not complete, and on quit.
     * Safe to call from any thread.
     */
    public void writeSmpFallbackSnapshot(Player player) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        try {
            String json = buildFallbackSnapshot(player);
            if (json == null) {
                return;
            }
            db.saveTempData(KEY_SMP_RESTART_INV + player.getUniqueId(), json, 600);
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX
                    + "[SmpSnapshot] Snapshot written for " + player.getName());
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                    + "[SmpSnapshot] Failed to write shutdown snapshot for " + player.getName() + ": " + e.getMessage());
        }
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
        if (smpSnapshotTask != null) {
            smpSnapshotTask.cancel();
            smpSnapshotTask = null;
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        publishExecutor.shutdown();
        // Clear this server's roster entries from the DB so stale entries don't appear on other servers
        try {
            db.clearRosterForServer(thisServer);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to clear roster on shutdown: " + e.getMessage());
        }
        // Remove the heartbeat row so other servers see this one as offline immediately
        db.deleteTempData(KEY_HEARTBEAT + thisServer);
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
            case CH_DOMINION_UPDATE -> handleDominionUpdate(json);
            case CH_DOMINION_LEVEL_OVERRIDE -> handleDominionLevelOverride(json);
            case CH_CHAT_GAME_START -> handleChatGameStart(json);
            case CH_CHAT_GAME_WIN -> handleChatGameWin(json);
            case CH_CHAT_GAME_EXPIRE -> handleChatGameExpire(json);
            case CH_CHAT_GAME_CLAIM -> handleChatGameClaim(json);
            case CH_DEATH -> handleDeath(json);
            case CH_BREW_UNLOCK -> handleBrewUnlock(json);
            case CH_BALANCE_ADJUST -> handleBalanceAdjust(json);
            case CH_PAY_NOTIFY -> handlePayNotify(json);
            case CH_SUDO -> handleSudo(json);
            case CH_SENTINEL_SUMMON -> handleSentinelSummon(json);
            case CH_SENTINEL_SPAWN -> handleSentinelSpawn(json);
            case CH_SENTINEL_DEATH -> handleSentinelDeath(json);
            case CH_OUTPOST_DISBAND -> handleOutpostDisband(json);
            case CH_OUTPOST_CREATE -> handleOutpostCreate(json);
            case CH_OUTPOST_UPDATE -> handleOutpostUpdate(json);
            case CH_CREATIVE_PLOT_UPDATE -> handleCreativePlotUpdate(json);
            case CH_CREATIVE_PLOT_DELETE -> handleCreativePlotDelete(json);
            case CH_CREATIVE_EXPAND -> handleCreativeExpand(json);
            case CH_JOB_UPDATE -> handleJobUpdate(json);
            case CH_JOB_TOP_REQUEST -> handleJobTopRequest(json);
            case CH_JOB_TOP_RESPONSE -> handleJobTopResponse(json);
            case CH_MOUNT_UPDATE -> handleMountUpdate(json);
            case CH_VOTE_AWARD -> handleVoteAward(json);
            case CH_VOTE_KEY -> handleVoteKey(json);
            case CH_PURCHASE_KEY -> handlePurchaseKey(json);
            case CH_VP_TRANSFER -> handleVpTransfer(json);
            case CH_INVSEE_REQUEST -> handleInvseeRequest(json);
            case CH_INVSEE_RESPONSE -> handleInvseeResponse(json);
            case CH_INVSEE_UPDATE -> handleInvseeUpdate(json);
            case CH_INVSEE_UNWATCH -> handleInvseeUnwatch(json);
            case CH_RANK_UPDATE -> handleRankUpdate(json);
            case CH_PERK_UPDATE -> handlePerkUpdate(json);
            case CH_MARKET_UPDATE -> handleMarketUpdate(json);
            case CH_PERM_RELOAD -> handlePermReload(json);
            case CH_CONFIG_RELOAD -> handleConfigReload(json);
            case CH_TRADE_INVITE -> handleTradeInvite(json);
            case CH_TRADE_ACCEPT -> handleTradeAccept(json);
            case CH_TRADE_CANCEL -> handleTradeCancel(json);
            case CH_TRADE_ITEMS -> handleTradeItems(json);
            case CH_TRADE_MONEY -> handleTradeMoney(json);
            case CH_TRADE_CONFIRM -> handleTradeConfirm(json);
            case CH_TRADE_EXECUTE -> handleTradeExecute(json);
            case CH_TRADE_RESULT -> handleTradeResult(json);
            case CH_PUNISHMENT -> handlePunishment(json);
            case CH_PRONOUNS_UPDATE -> handlePronounsUpdate(json);
            case CH_PREFIX_UPDATE -> handlePrefixUpdate(json);
            case CH_NICKNAME_UPDATE -> handleNicknameUpdate(json);
            case CH_VANISH_UPDATE -> handleVanishUpdate(json);
            case CH_CLEAR_CHAT -> handleClearChat(json);
            case CH_DATE_SET -> handleDateSet(json);
            case CH_WORLD_EVENT_START -> handleWorldEventStart(json);
            case CH_PING_UPDATE -> handlePingUpdate(json);
            case CH_HEARTBEAT -> handleHeartbeat(json);
            case CH_RESOURCE_WORLD_LOCK -> handleResourceWorldLock(json);
            case CH_AVATAR_UPDATE -> handleAvatarUpdate(json);
            case CH_ORIGINAL_PLAYER_UPDATE -> handleOriginalPlayerUpdate(json);
            case CH_PLAYER_CREATED -> handlePlayerCreated(json);
            case CH_SHOP_STOCK -> handleShopStock(json);
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
     * Publishes a public chat message along with the exact message Component shown on this server, so the
     * other server renders it identically (gradients, clickable URLs) instead of re-parsing the legacy string.
     * The prefix is sent as data so the other server can show it to each player in their own prefix styles.
     *
     * @param chatPrefix       the prefix of the sender
     * @param chatMessage      the legacy version of the message (for console fallback)
     * @param messageComponent the message Component, without the prefix
     */
    public void publishChat(ChatPrefix chatPrefix, String chatMessage, Component messageComponent) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("prefix", chatPrefix.format(PrefixStyle.defaults()));
        json.addProperty("message", chatMessage);
        json.add("prefixData", chatPrefix.toJson());
        json.addProperty("messageJson", GsonComponentSerializer.gson().serialize(messageComponent));
        publish(CH_CHAT, json);
    }

    /**
     * Publishes an interactive chat message (one containing [item]/[inv]/[ec]/[coords]) so the
     * other server can display the same clickable component and serve /ichat view clicks locally.
     *
     * @param chatPrefix       the prefix of the sender
     * @param chatMessage      the plain-text version of the message (for console/discord fallback)
     * @param messageComponent the Adventure Component of the message including interactive parts, without the prefix
     * @param snapshots        every snapshot that was created while building the message
     */
    public void publishInteractiveChat(ChatPrefix chatPrefix, String chatMessage, Component messageComponent,
                                       List<InteractiveChatManager.Snapshot> snapshots) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("prefix", chatPrefix.format(PrefixStyle.defaults()));
        json.addProperty("message", chatMessage);
        json.add("prefixData", chatPrefix.toJson());
        json.addProperty("messageJson", GsonComponentSerializer.gson().serialize(messageComponent));

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

        // Rebuild the prefix for each player in their own prefix styles, followed by the interactive message
        relayChat(json, prefix, message);
        Bukkit.getConsoleSender().sendMessage(ChatUtils.translateToColor(
                "&8[" + originServer.toUpperCase() + "] " + prefix + message));
    }

    /**
     * Called from PlayerServerJoinListener after the player's AranarthPlayer is ready.
     */
    public void publishPlayerJoin(UUID uuid, AranarthPlayer ap) {
        String nickname = ap.getNickname().isEmpty() ? ap.getUsername() : ap.getNickname();

        // Update roster in DB
        String pronounsName = ap.getPronouns() != null ? ap.getPronouns().name() : "MALE";
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                db.upsertRosterEntry(uuid, ap.getUsername(), nickname, thisServer,
                        ap.getRank(), ap.getCouncilRank(), ap.getSaintRank(), ap.getArchitectRank(), ap.isVanished(),
                        pronounsName)
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
        json.addProperty("pronouns", ap.getPronouns() != null ? ap.getPronouns().name() : "MALE");
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
     * @param isSleepSkip Whether the time change came from players sleeping, which is the only case that ends a storm early.
     */
    public void publishSyncTime(long time, boolean isSleepSkip) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("time", time);
        json.addProperty("isSleepSkip", isSleepSkip);
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
     * @param total    Number of players sleeping across all servers.
     * @param sleeping Number of players currently sleeping.
     * @param required Number required to skip the night.
     */
    public void publishSleepMessage(String message, int total, int sleeping, int required) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("message", message);
        json.addProperty("total", total);
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
        publishBroadcast(rawMessage, false);
    }

    /**
     * Relays a pre-formatted broadcast, optionally playing the ping sound for each recipient.
     */
    public void publishBroadcast(String rawMessage, boolean isPing) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("message", rawMessage);
        if (isPing) {
            json.addProperty("ping", true);
        }
        publish(CH_BROADCAST, json);
    }

    /**
     * Relays a lang-keyed broadcast so each server renders it in its own players' locales.
     */
    public void publishLangBroadcast(String key, Object... args) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("key", key);
        JsonArray argArray = new JsonArray();
        for (Object arg : args) {
            argArray.add(String.valueOf(arg));
        }
        json.add("args", argArray);
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
     * Notifies all other servers of a player shop's recounted stock, so the shop search stays current on both servers.
     *
     * @param shop The shop whose stock was recounted.
     */
    public void publishShopStockUpdate(Shop shop) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", shop.getUuid().toString());
        json.addProperty("worldName", shop.getWorldName());
        json.addProperty("x", shop.getLocation().getBlockX());
        json.addProperty("y", shop.getLocation().getBlockY());
        json.addProperty("z", shop.getLocation().getBlockZ());
        json.addProperty("stock", shop.getStock());
        json.addProperty("freeSpace", shop.getFreeSpace());
        publish(CH_SHOP_STOCK, json);
    }

    private void handleShopStock(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        List<Shop> ownerShops = ShopUtils.getShops().get(UUID.fromString(json.get("uuid").getAsString()));
        if (ownerShops == null) {
            return;
        }
        String worldName = json.get("worldName").getAsString();
        int x = json.get("x").getAsInt();
        int y = json.get("y").getAsInt();
        int z = json.get("z").getAsInt();
        for (Shop shop : ownerShops) {
            Location loc = shop.getLocation();
            if (originServer.equals(shop.getServerName()) && worldName.equals(shop.getWorldName())
                    && loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z) {
                shop.setStock(json.get("stock").getAsInt());
                shop.setFreeSpace(json.get("freeSpace").getAsInt());
                return;
            }
        }
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
     * Tells the server where the target player is online to run a command on their behalf.
     *
     * @param targetUuid The UUID of the player the command will be run as.
     * @param command    The command to run, without the leading slash.
     */
    public void publishSudo(UUID targetUuid, String command) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("targetUuid", targetUuid.toString());
        json.addProperty("command", command);
        publish(CH_SUDO, json);
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
     * Notifies other servers that a player's perks have changed so their in-memory copy is never
     * stale, regardless of which server the player is on or logs into next.
     */
    public void publishPerkUpdate(UUID uuid, String perks) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("perks", perks);
        publish(CH_PERK_UPDATE, json);
    }

    /**
     * Notifies other servers that a player's pronouns have changed so they can update the
     * remote roster and refresh the TAB list immediately.
     */
    public void publishPronounsUpdate(UUID uuid, String pronouns) {
        // Update DB so the roster survives server restarts
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                db.updateRosterPronouns(uuid, pronouns)
        );
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("pronouns", pronouns);
        publish(CH_PRONOUNS_UPDATE, json);
    }

    /**
     * Notifies other servers that a player's nickname has changed so they can update the
     * remote roster, refresh the TAB list, and keep their copy of the player in sync.
     *
     * @param nickname The raw nickname, or an empty string if it was removed.
     */
    public void publishNicknameUpdate(UUID uuid, String username, String nickname) {
        String rosterNickname = nickname.isEmpty() ? username : nickname;
        // Update DB so the roster survives server restarts
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                db.updateRosterNickname(uuid, rosterNickname)
        );
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("username", username);
        json.addProperty("nickname", nickname);
        publish(CH_NICKNAME_UPDATE, json);
    }

    /**
     * Notifies other servers that a player has vanished or reappeared, so they can hide or show
     * them in the TAB list and online count.
     */
    public void publishVanishUpdate(UUID uuid, boolean isVanished) {
        // Update DB so the roster survives server restarts and the Discord online list stays accurate
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                db.updateRosterVanished(uuid, isVanished)
        );
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("vanished", isVanished);
        publish(CH_VANISH_UPDATE, json);
    }

    /**
     * Tells other servers to clear the chat of all their online players.
     */
    public void publishClearChat() {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        publish(CH_CLEAR_CHAT, json);
    }

    /**
     * Notifies other servers that the in-game date was manually changed, without triggering any new day logic.
     */
    public void publishDateSet(int day, int weekday, Month month, int year) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("day", day);
        json.addProperty("weekday", weekday);
        json.addProperty("month", month.name());
        json.addProperty("year", year);
        publish(CH_DATE_SET, json);
    }

    /**
     * Notifies other servers that a world event was manually started, ending any event active there first.
     */
    public void publishWorldEventStart(WorldEvent event, int intensity) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("event", event.name());
        json.addProperty("intensity", intensity);
        publish(CH_WORLD_EVENT_START, json);
    }

    /**
     * Notifies other servers that a player's prefix styles have changed, so their copy of the player stays in sync.
     */
    public void publishPrefixUpdate(UUID uuid, String prefixStyles) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("prefixStyles", prefixStyles);
        publish(CH_PREFIX_UPDATE, json);
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
     * Tells the other server to reload its config.yml and abilities.yml, and import its own coupons.txt.
     */
    public void publishConfigReload() {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        publish(CH_CONFIG_RELOAD, json);
    }

    /**
     * Notifies all other servers whether Survival's resource world is locked while it is being reset,
     * so that /resource is refused on the SMP server instead of transferring the player.
     */
    public void publishResourceWorldLock(boolean isLocked) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("locked", isLocked);
        publish(CH_RESOURCE_WORLD_LOCK, json);
    }

    /**
     * Notifies all other servers that an Avatar's reign has started or ended, carrying the full
     * avatar list so their in-memory copy (which they also save periodically) is never stale.
     */
    public void publishAvatarUpdate(UUID uuid, boolean isNewAvatar) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("isNewAvatar", isNewAvatar);
        json.add("avatars", PersistenceUtils.avatarsToJson());
        publish(CH_AVATAR_UPDATE, json);
    }

    /**
     * Notifies all other servers that a player was added to or removed from the OG list.
     */
    public void publishOriginalPlayerUpdate(UUID uuid, boolean isOriginal) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("isOriginal", isOriginal);
        publish(CH_ORIGINAL_PLAYER_UPDATE, json);
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
        // Update DB so the Discord bot's !online list can show who is AFK
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                db.updateRosterAfk(uuid, isAfk)
        );
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("nickname", nickname);
        json.addProperty("afk", isAfk);
        json.addProperty("afkStartTime", isAfk ? System.currentTimeMillis() : 0L);
        publish(CH_AFK, json);
    }

    /**
     * Publishes the current ping of every locally online player so other servers
     * can display accurate pings via /ping.
     */
    public void publishPingUpdates() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            JsonObject json = new JsonObject();
            json.addProperty("server", thisServer);
            json.addProperty("uuid", p.getUniqueId().toString());
            json.addProperty("ping", p.getPing());
            publish(CH_PING_UPDATE, json);
        }
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
     * Forwards a boost add from SMP to Survival so Survival applies it as the source of truth.
     *
     * @param boostName    Boost enum name (e.g. "MINER").
     * @param uuid         The UUID of the player applying the boost, or null if none.
     * @param fromVoteShop Whether the boost was purchased from the vote shop.
     */
    public void publishBoostAddRequest(String boostName, UUID uuid, boolean fromVoteShop) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("boost", boostName);
        json.addProperty("request", true);
        json.addProperty("uuid", uuid == null ? "" : uuid.toString());
        json.addProperty("fromVoteShop", fromVoteShop);
        json.addProperty("removing", false);
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
     * Freezes the player's inventory and interactions while a transfer or arrival is in progress,
     * so nothing can change between the inventory being serialized and the player leaving.
     */
    public void lockPlayer(UUID uuid) {
        lockedPlayers.add(uuid);
    }

    public void unlockPlayer(UUID uuid) {
        lockedPlayers.remove(uuid);
    }

    public boolean isLocked(UUID uuid) {
        return lockedPlayers.contains(uuid);
    }

    /**
     * Marks that this server's copy of the player's inventory is not theirs (they landed on the
     * wrong server and are waiting to be routed), so a quit here must not save it.
     */
    public void markNonAuthoritative(UUID uuid) {
        nonAuthoritativePlayers.add(uuid);
    }

    public void clearNonAuthoritative(UUID uuid) {
        nonAuthoritativePlayers.remove(uuid);
    }

    public boolean isNonAuthoritative(UUID uuid) {
        return nonAuthoritativePlayers.contains(uuid);
    }

    /**
     * Clears every transfer-related flag for the player. Called once their quit has been handled.
     */
    public void clearTransferState(UUID uuid) {
        lockedPlayers.remove(uuid);
        nonAuthoritativePlayers.remove(uuid);
    }

    /**
     * Velocity does not tell the backend when a Connect fails, so the player would otherwise stay
     * here flagged as transferring forever. Once the timeout passes with the player still online,
     * the transfer is treated as failed: the pending teleport is removed and the player is either
     * unlocked or, if this server's inventory is not theirs, kicked so they can reconnect.
     */
    private void scheduleTransferTimeout(Player player) {
        final UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (!player.isOnline() || !transferringPlayers.remove(uuid)) {
                return;
            }
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Net] Transfer of " + player.getName()
                    + " timed out; the player is still on this server.");
            clearPendingTeleport(uuid);
            if (isNonAuthoritative(uuid)) {
                player.kickPlayer(ChatUtils.translateToColor(Lang.getFor(player, "teleport.server_unreachable")));
            } else {
                unlockPlayer(uuid);
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.failed")));
            }
        }, TRANSFER_TIMEOUT_TICKS);
    }

    /**
     * Writes the pending teleport and only then tells the other server to send the player, so the
     * other server can never read the transfer before the pending teleport exists.
     */
    public void setPendingAndPublishTransfer(UUID uuid, PendingTeleport pending, String targetServer) {
        final String pendingJson = gson.toJson(pending);
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                db.saveTempData(KEY_PENDING_TP + uuid, pendingJson, 300);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "DB: failed to set pending TP for " + uuid);
                return;
            }
            publishTransfer(uuid, targetServer);
        });
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

        // The combat-log kill is skipped for transfers, so a tagged player must not be able to leave this way
        AranarthPlayer apCombat = AranarthUtils.getPlayer(uuid);
        if (apCombat != null && !apCombat.isInAdminMode() && !apCombat.getCombatLogTime().isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.combat_tagged")));
            return;
        }

        // An open trade is cancelled first so the offered items are back in the inventory
        TradeManager.handleQuit(player);
        // An astral projection keeps the armor on its mannequin, so end it to put the armor back on first
        if (AstralProjection.isProjecting(uuid)) {
            AstralProjection.getActiveProjection(uuid).endAbility();
        }
        // Closing first runs every GUI close handler (held shulker, quiver, potions, dominion food)
        // and returns the crafting grid and cursor, so the serialized inventory is the final one
        player.closeInventory();
        // Nothing may change between serialization and the player leaving this server
        lockPlayer(uuid);

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
                ap.setSurvivalEffects(AranarthUtils.serializePotionEffects(player));
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
                    // (flag was never set). Remove the pending teleport so their next login is not
                    // treated as an arrival from this transfer.
                    clearPendingTeleport(uuid);
                    return;
                }
                if (!transferAllowed) {
                    // The pending-TP write failed: transferring now would leave the player on the
                    // destination with no inventory state. Abort and notify the player instead.
                    unlockPlayer(uuid);
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.failed")));
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
                    scheduleTransferTimeout(player);
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                            + "Failed to transfer " + player.getName() + " to " + targetServer + ": " + e.getMessage());
                    transferringPlayers.remove(uuid);
                    clearPendingTeleport(uuid);
                    unlockPlayer(uuid);
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.failed")));
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
        TradeManager.handleQuit(player);
        player.closeInventory();
        lockPlayer(uuid);

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
                    // Otherwise their next login would be treated as an arrival from this transfer
                    clearPendingTeleport(uuid);
                    return;
                }
                if (!transferAllowed) {
                    if (isNonAuthoritative(uuid)) {
                        player.kickPlayer(ChatUtils.translateToColor(Lang.getFor(player, "teleport.server_unreachable")));
                        return;
                    }
                    unlockPlayer(uuid);
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.failed")));
                    return;
                }
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(bos);
                    dos.writeUTF("Connect");
                    dos.writeUTF(targetServer);
                    transferringPlayers.add(uuid);
                    player.sendPluginMessage(AranarthCore.getInstance(), "BungeeCord", bos.toByteArray());
                    scheduleTransferTimeout(player);
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                            + "Failed to transfer " + player.getName() + " to " + targetServer + ": " + e.getMessage());
                    transferringPlayers.remove(uuid);
                    clearPendingTeleport(uuid);
                    if (isNonAuthoritative(uuid)) {
                        player.kickPlayer(ChatUtils.translateToColor(Lang.getFor(player, "teleport.server_unreachable")));
                    } else {
                        unlockPlayer(uuid);
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.failed")));
                    }
                }
            });
        });
    }

    // -------------------------------------------------------------------------
    // Cross-server TP context (in-memory)
    // -------------------------------------------------------------------------

    public void storeCrossServerTpContext(UUID localPlayerUuid, CrossServerTpContext ctx) {
        long now = System.currentTimeMillis();
        pendingCrossServerRequests.values().removeIf(c -> now - c.createdAt() > AranarthPlayer.TELEPORT_REQUEST_TTL_MS);
        pendingCrossServerRequests.put(localPlayerUuid, ctx);
    }

    /**
     * Provides the cross-server request the local player received, or null if there is none or it expired.
     */
    public CrossServerTpContext getCrossServerTpContext(UUID localPlayerUuid) {
        CrossServerTpContext ctx = pendingCrossServerRequests.get(localPlayerUuid);
        if (ctx != null && System.currentTimeMillis() - ctx.createdAt() > AranarthPlayer.TELEPORT_REQUEST_TTL_MS) {
            pendingCrossServerRequests.remove(localPlayerUuid);
            return null;
        }
        return ctx;
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

    /**
     * Records that the player just landed on this server as a fallback because their server went down.
     */
    public void markFallbackLanding(UUID uuid) {
        fallbackLandings.put(uuid, System.currentTimeMillis());
    }

    /**
     * Returns true if the player landed on this server as a fallback within the last minute.
     * When both servers auto-restart together, SMP players get bounced here seconds before this
     * server also shuts down, and their last location should keep pointing at SMP.
     */
    public boolean isRecentFallbackLanding(UUID uuid) {
        Long landedAt = fallbackLandings.get(uuid);
        return landedAt != null && System.currentTimeMillis() - landedAt < 60 * 1000L;
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
                // Seed serverLastSeen so isServerOnline() works immediately at startup
                // when a peer server is already running and has players in the roster.
                long now = System.currentTimeMillis();
                for (NetworkPlayer np : loaded.values()) {
                    serverLastSeen.put(np.getServer(), now);
                }
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

        relayChat(json, prefix, message);
        Bukkit.getConsoleSender().sendMessage(ChatUtils.translateToColor(
                "&8[" + originServer.toUpperCase() + "] " + prefix + message));
    }

    /**
     * Sends a relayed chat message to every player on this server. When the sender's prefix data is included,
     * each player sees the prefix in their own prefix styles, otherwise the legacy prefix and message are used.
     */
    private void relayChat(JsonObject json, String prefix, String message) {
        if (json.has("prefixData") && json.has("messageJson")) {
            try {
                ChatPrefix chatPrefix = ChatPrefix.fromJson(json.getAsJsonObject("prefixData"));
                Component messageComponent = GsonComponentSerializer.gson().deserialize(json.get("messageJson").getAsString());
                ChatUtils.sendChatToViewers(Bukkit.getOnlinePlayers(), chatPrefix, messageComponent);
                return;
            } catch (Exception ignored) {
                // Falls back to the legacy string below
            }
        }
        Component component = LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor(prefix + message));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
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
        String pronouns = json.has("pronouns") ? json.get("pronouns").getAsString() : "MALE";
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
                textureSignature,
                pronouns
        );
        remoteRoster.put(uuid, np);
        if (!vanished) {
            NetworkTabManager.addToTab(np);
        }
        AranarthUtils.updateTab();

        // Loads the player if this server has never seen them, so commands targeting them work here too
        if (AranarthUtils.getLoadedPlayer(uuid) == null) {
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
                String rawRow = db.loadAranarthPlayerRaw(uuid);
                if (rawRow == null) {
                    return;
                }
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                    if (AranarthUtils.getLoadedPlayer(uuid) == null) {
                        PersistenceUtils.reloadPlayerFromRow(uuid, rawRow);
                    }
                });
            });
        }
    }

    /**
     * Sends the row of a player who just joined the network for the first time to the other server.
     */
    public void publishPlayerCreated(UUID uuid, String username, String rawRow) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("username", username);
        json.addProperty("raw", rawRow);
        publish(CH_PLAYER_CREATED, json);
    }

    private void handlePlayerCreated(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        // Never replaces a copy this server already has
        if (AranarthUtils.getLoadedPlayer(uuid) == null) {
            String username = json.has("username") ? json.get("username").getAsString() : null;
            PersistenceUtils.reloadPlayerFromRow(uuid, json.get("raw").getAsString(), username);
        }
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

        Bukkit.getConsoleSender().sendMessage(ChatUtils.translateToColor(Lang.plain(joinMessage)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatUtils.translateToColor(Lang.resolve(p, joinMessage)));
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
        pendingCrossServerRequests.values().removeIf(c -> c.remotePlayerUuid().equals(uuid));

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
            Bukkit.getConsoleSender().sendMessage(ChatUtils.translateToColor(Lang.plain(quitMessage)));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatUtils.translateToColor(Lang.resolve(p, quitMessage)));
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

        storeCrossServerTpContext(toUuid, new CrossServerTpContext(fromUuid, fromNickname, fromServer, isTpHere, System.currentTimeMillis()));

        // Do NOT set teleportToUuid/teleportFromUuid here - those fields resolve the UUID via
        // Bukkit.getPlayer(), which only works for locally-online players. The remote requester is
        // on a different server, so that lookup returns null and /tpaccept would falsely report
        // "player is no longer online". The CrossServerTpContext above is the sole routing
        // mechanism; /tpaccept falls through to the NetworkManager block when neither local field
        // is populated.

        if (isTpHere) {
            target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "tp.tphere_request", "player", fromNickname)));
        } else {
            target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "tp.request_received", "player", fromNickname)));
        }
        target.sendMessage(ChatUtils.buildTpAcceptDenyPrompt(target));
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
            setPendingAndPublishTransfer(accepterUuid,
                    new PendingTeleport(requesterUuid.toString(), accepterNick, Lang.getFor(accepterUuid, "teleport.teleported_to", "player", requesterNick)),
                    thisServer);
            if (requester != null) {
                requester.sendMessage(ChatUtils.chatMessage(Lang.getFor(requester, "tp.accepted", "player", accepterNick)));
            }
        } else {
            Player requester = Bukkit.getPlayer(requesterUuid);
            if (requester != null) {
                requester.sendMessage(ChatUtils.chatMessage(Lang.getFor(requester, "tp.accepted", "player", accepterNick)));
                String targetServer = AranarthCore.getInstance().getConfig()
                        .getString("network.servers." + accepterServer, accepterServer);
                PendingTeleport ptForRequester = new PendingTeleport(accepterUuid.toString(),
                        "&e&l" + accepterServer.toUpperCase(), Lang.getFor(requester, "teleport.teleported_to", "player", accepterNick));
                // Same countdown and combat check as a same-server teleport
                AranarthPlayer requesterAp = AranarthUtils.getPlayer(requesterUuid);
                AranarthUtils.teleportPlayer(requester, requester.getLocation(), requester.getLocation(),
                        requesterAp.isInAdminMode(), "&e&l" + accepterServer.toUpperCase(), Lang.getFor(requester, "teleport.transferring_named", "name", accepterNick), success -> {
                            if (success) {
                                saveInventoryAndTransfer(requester, targetServer, ptForRequester);
                            }
                        });
            }
        }
    }

    private void handleTpDeny(JsonObject json) {
        UUID denierUuid = UUID.fromString(json.get("denierUuid").getAsString());
        String denierNick = json.get("denierNickname").getAsString();
        UUID requesterUuid = UUID.fromString(json.get("requesterUuid").getAsString());

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null) {
            requester.sendMessage(ChatUtils.chatMessage(Lang.getFor(requester, "tp.denied_by", "player", denierNick)));
        }
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
        // The pending teleport is always written before the transfer is published. Without it the
        // destination would keep its own stale copy of the inventory, so never transfer bare.
        PendingTeleport existing = getPendingTeleport(uuid);
        if (existing != null) {
            saveInventoryAndTransfer(player, velocityName, existing);
        } else {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Net] Transfer of " + player.getName()
                    + " to " + toServer + " skipped: no pending teleport was found.");
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.failed")));
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
        // Only sleeping may end a storm before its planned duration is reached
        boolean isSleepSkip = json.has("isSleepSkip") && json.get("isSleepSkip").getAsBoolean();
        if (isSleepSkip && AranarthUtils.getWeather() != Weather.CLEAR) {
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
                    if (pWorld.equals("arena") || pWorld.startsWith("creative")) {
                        continue;
                    }
                    AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
                    if (ap.isWeatherMessageDisabled()) {
                        continue;
                    }
                    p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "weather.clear")));
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
                String broadcastKey = isThunder ? "weather.thunder" : "weather.rain";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pWorld = p.getWorld().getName();
                    if (pWorld.equals("arena") || pWorld.startsWith("creative")) {
                        continue;
                    }
                    AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
                    if (ap.isWeatherMessageDisabled()) {
                        continue;
                    }
                    p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, broadcastKey)));
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
                    if (pWorld.equals("arena") || pWorld.startsWith("creative")) {
                        continue;
                    }
                    AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
                    if (ap.isWeatherMessageDisabled()) {
                        continue;
                    }
                    p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "weather.snow")));
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
        Component targetPrefixComp = LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor(prefixStart + Lang.getFor(target, "message.from") + fromNickname + prefixEnd + " &7&o>> &e&o"));
        Component targetMsgComp = targetPrefixComp.append(LegacyComponentSerializer.legacySection().deserialize(ChatUtils.translateToColor("&e&o") + message));
        target.sendMessage(ChatUtils.clickableCommand(targetMsgComp, ChatUtils.translateToColor(Lang.getFor(target, "message.hover_reply", "player", fromNickname)), "/r ", true));
        AranarthUtils.playPingSound(target);

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

        // Broadcast the AFK message to locally online players who can see the player
        String langKey = isAfk ? "afk.player_now_afk" : "afk.player_no_longer_afk";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (AranarthUtils.isVanishedFrom(p, uuid)) {
                continue;
            }
            p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, langKey, "name", nickname)));
        }
    }

    private void handlePingUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        int ping = json.get("ping").getAsInt();

        NetworkPlayer np = remoteRoster.get(uuid);
        if (np != null) {
            np.setPing(ping);
            if (!np.isVanished()) {
                NetworkTabManager.updatePingInTab(np);
            }
        }
    }

    private void handleHeartbeat(JsonObject json) {
        if (!json.has("server")) return;
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;
        serverLastSeen.put(originServer, System.currentTimeMillis());
    }

    private void handlePerkUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        HashMap<Perk, Integer> perks = PersistenceUtils.deserializePerks(json.get("perks").getAsString());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
            if (ap == null) {
                return;
            }
            boolean hadCompressor = ap.getPerks() != null && ap.getPerks().getOrDefault(Perk.COMPRESSOR, 0) == 1;
            ap.setPerks(perks);
            AranarthUtils.setPlayer(uuid, ap);

            // Enables all materials to be compressed by default
            if (!hadCompressor && perks.getOrDefault(Perk.COMPRESSOR, 0) == 1) {
                AranarthUtils.compressAllMaterials(uuid);
            }

            // This server is authoritative for the player, so it saves their row
            Player localPlayer = Bukkit.getPlayer(uuid);
            if (localPlayer != null) {
                PermissionUtils.evaluatePlayerPermissions(localPlayer);
                PersistenceUtils.saveAranarthPlayerImmediately(uuid);
            }
        });
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

    private void handlePronounsUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String pronounsStr = json.get("pronouns").getAsString();

        NetworkPlayer np = remoteRoster.get(uuid);
        if (np != null) {
            np.setPronouns(pronounsStr);
            if (!np.isVanished()) {
                NetworkTabManager.addToTab(np);
            }
            AranarthUtils.updateTab();
        }

        // Keeps the in-memory copy accurate so /info and transfers to this server show the new pronouns
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap != null) {
            ap.setPronouns(Pronouns.fromName(pronounsStr));
            AranarthUtils.setPlayer(uuid, ap);
        }
    }

    private void handleNicknameUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String username = json.get("username").getAsString();
        String nickname = json.get("nickname").getAsString();

        NetworkPlayer np = remoteRoster.get(uuid);
        if (np != null) {
            np.setNickname(nickname.isEmpty() ? username : nickname);
            if (!np.isVanished()) {
                NetworkTabManager.addToTab(np);
            }
            AranarthUtils.updateTab();
        }

        // Keeps the in-memory copy accurate so the player keeps their nickname when transferring to this server
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap != null) {
            ap.setNickname(nickname);
            AranarthUtils.setPlayer(uuid, ap);
        }
    }

    private void handleVanishUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        boolean isVanished = json.get("vanished").getAsBoolean();

        NetworkPlayer np = remoteRoster.get(uuid);
        if (np != null) {
            np.setVanished(isVanished);
            if (isVanished) {
                NetworkTabManager.removeFromTab(uuid);
            } else {
                NetworkTabManager.addToTab(np);
            }
            AranarthUtils.updateTab();
        }

        // Keeps the in-memory copy accurate so the player stays vanished when transferring to this server
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap != null) {
            ap.setVanished(isVanished);
            AranarthUtils.setPlayer(uuid, ap);
        }
    }

    private void handleClearChat(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        for (int i = 0; i < 128; i++) {
            Bukkit.broadcastMessage("");
        }
    }

    private void handleDateSet(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        AranarthUtils.setDay(json.get("day").getAsInt());
        AranarthUtils.setWeekday(json.get("weekday").getAsInt());
        AranarthUtils.setMonth(Month.valueOf(json.get("month").getAsString()));
        AranarthUtils.setYear(json.get("year").getAsInt());
    }

    private void handleWorldEventStart(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        WorldEvent event;
        try {
            event = WorldEvent.valueOf(json.get("event").getAsString());
        } catch (IllegalArgumentException e) {
            return;
        }
        int intensity = json.get("intensity").getAsInt();

        // Only the Survival server announces the event and posts it to Discord, so it is never shown twice
        if (AranarthUtils.getActiveWorldEvent() != null) {
            WorldEventManager.getInstance().endEvent();
        }
        WorldEventManager.getInstance().startEvent(event, intensity);
    }

    private void handlePrefixUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String prefixStyles = json.get("prefixStyles").getAsString();

        // Prefix styles only change how the player sees others, so no other player's display needs to be refreshed.
        // Keeps the in-memory copy accurate so the player keeps their styles when transferring to this server
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap != null) {
            ap.setEncodedPrefixStyles(prefixStyles);
            AranarthUtils.setPlayer(uuid, ap);
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

    private void handleConfigReload(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            AranarthCore.getInstance().reloadConfig();
            AbilityConfig.load(AranarthCore.getInstance());
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), CouponUtils::importCoupons);
        });
    }

    private void handleResourceWorldLock(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        boolean isLocked = json.get("locked").getAsBoolean();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> ResourceWorldUtils.setRemoteResourceLocked(isLocked));
    }

    private void handleAvatarUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        boolean isNewAvatar = json.get("isNewAvatar").getAsBoolean();
        List<Avatar> avatars = PersistenceUtils.avatarsFromJson(json.getAsJsonArray("avatars"));
        AvatarUtils.applyRemoteUpdate(avatars, uuid, isNewAvatar);
    }

    private void handleOriginalPlayerUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        boolean isOriginal = json.get("isOriginal").getAsBoolean();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            if (isOriginal) {
                AranarthUtils.addOriginalPlayer(uuid);
            } else {
                AranarthUtils.removeOriginalPlayer(uuid);
            }
        });
    }

    private void handleMarketUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        // Reload market dynamics and trade market data from DB on main thread;
        // only refresh signs on Survival (shops world does not exist on SMP)
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            PersistenceUtils.loadMarketDynamicsFromDatabase();
            PersistenceUtils.loadTradeMarketDataFromDatabase();
            if (!AranarthCore.isSmpServer()) {
                List<Shop> serverShops = ShopUtils.getShops().get(null);
                if (serverShops != null) {
                    for (Shop shop : serverShops) {
                        MarketUtils.refreshServerShopSign(shop);
                    }
                }
            }
        });
    }

    private void handleBroadcast(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        if (json.has("key")) {
            JsonArray argArray = json.getAsJsonArray("args");
            Object[] args = new Object[argArray.size()];
            for (int i = 0; i < args.length; i++) {
                args[i] = argArray.get(i).getAsString();
            }
            Lang.broadcastLocal(json.get("key").getAsString(), args);
            return;
        }
        String message = json.get("message").getAsString();
        boolean isServerTip = json.has("type") && json.get("type").getAsString().equals("server_tip");
        boolean isPing = json.has("ping") && json.get("ping").getAsBoolean();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isServerTip) {
                AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
                if (ap != null && ap.isServerTipsDisabled()) {
                    continue;
                }
            }
            player.sendMessage(message);
            if (isPing) {
                AranarthUtils.playPingSound(player);
            }
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
        // A player loaded from the DB here may already include the delta, so only an existing copy is adjusted
        AranarthPlayer ap = AranarthUtils.getLoadedPlayer(uuid);
        if (ap != null) {
            ap.setBalance(ap.getBalance() + delta);
            if (Bukkit.getPlayer(uuid) != null) {
                // This server is authoritative for the player, persist so the next periodic
                // save cannot overwrite the updated balance with a stale value
                PersistenceUtils.saveAranarthPlayerImmediately(uuid);
            } else if (ap.getBalanceSnapshot() >= 0.0) {
                // The origin server persists the delta for players not online here, so shift the
                // snapshot too, otherwise this delta would be re-applied as a local change later
                ap.setBalanceSnapshot(ap.getBalanceSnapshot() + delta);
            }
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
        target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "economy.received_payment", "amount", formattedAmount, "player", fromNickname)));
    }

    private void handleSudo(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID targetUuid = UUID.fromString(json.get("targetUuid").getAsString());
        String command = json.get("command").getAsString();

        // Commands must be dispatched on the main thread
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            Player target = Bukkit.getPlayer(targetUuid);
            if (target == null || !target.isOnline()) {
                return;
            }
            // Re-check the council rank in case it changed since the origin server's roster was updated
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(targetUuid);
            if (aranarthPlayer != null && aranarthPlayer.getCouncilRank() >= 2) {
                return;
            }
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[SUDO] Running cross-server command as " + target.getName() + ": /" + command);
            target.performCommand(command);
        });
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
                    String text = json.has("total") && json.has("required")
                            ? Lang.getFor(player, "sleep.players_sleeping", "sleeping", json.get("total").getAsInt(), "required", json.get("required").getAsInt())
                            : message;
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
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
        // SMP forwarded a boost add - only Survival applies it, then syncs the end time back
        if (json.has("request") && json.get("request").getAsBoolean()) {
            if (!AranarthCore.isSmpServer()) {
                String uuidStr = json.get("uuid").getAsString();
                UUID uuid = uuidStr.isEmpty() ? null : UUID.fromString(uuidStr);
                AranarthUtils.addServerBoost(boost, null, uuid, json.get("fromVoteShop").getAsBoolean());
            }
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
        target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "mail.received", "sender", fromNickname)));
        target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "mail.received_view")));
        AranarthUtils.playPingSound(target);
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
                                   java.util.UUID newHolderUUID, double newGlobalBestTime, int streakCount) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("winner", winnerNickname);
        json.addProperty("answer", answer);
        json.addProperty("winnerUUID", winnerUUID.toString());
        json.addProperty("elapsedSeconds", elapsedSeconds);
        json.addProperty("newGlobalRecord", newGlobalRecord);
        json.addProperty("streakCount", streakCount);
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
     * Publishes that a dominion was saved, so the other server refreshes its copy from MySQL.
     */
    public void publishDominionUpdate(UUID dominionId) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("dominionId", dominionId.toString());
        publish(CH_DOMINION_UPDATE, json);
    }

    private void handleDominionUpdate(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID dominionId = UUID.fromString(json.get("dominionId").getAsString());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> PersistenceUtils.reloadSyncedDominion(dominionId));
    }

    /**
     * Asks the server hosting a dominion to temporarily set its level until its next re-evaluation.
     *
     * @param dominionId The UUID of the dominion.
     * @param level      The temporary level.
     */
    public void publishDominionLevelOverride(UUID dominionId, int level) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("dominionId", dominionId.toString());
        json.addProperty("level", level);
        publish(CH_DOMINION_LEVEL_OVERRIDE, json);
    }

    private void handleDominionLevelOverride(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID dominionId = UUID.fromString(json.get("dominionId").getAsString());
        Dominion dominion = DominionUtils.getDominionById(dominionId);
        // Only the hosting server re-evaluates levels, so it is the one that holds the override
        if (dominion != null && DominionUtils.isHostedHere(dominion)) {
            DominionLevelUtils.overrideLevel(dominion, json.get("level").getAsInt());
        }
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
     * Publishes a claimed or changed Creative plot so the other server's copy stays current.
     */
    public void publishCreativePlotUpdate(CreativePlot plot) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.add("data", PlotUtils.toJson(plot));
        publish(CH_CREATIVE_PLOT_UPDATE, json);
    }

    private void handleCreativePlotUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        JsonObject data = json.getAsJsonObject("data");
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                PlotUtils.loadPlot(PlotUtils.fromJson(data)));
    }

    /**
     * Publishes an unclaimed Creative plot so the other server removes it from memory.
     */
    public void publishCreativePlotDelete(String plotKey) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("plotKey", plotKey);
        publish(CH_CREATIVE_PLOT_DELETE, json);
    }

    /**
     * Asks the Survival server to expand the Creative plots world, as the world only exists there.
     */
    public void publishCreativeExpand(UUID requester, int rings) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("requester", requester.toString());
        json.addProperty("rings", rings);
        publish(CH_CREATIVE_EXPAND, json);
    }

    /**
     * Tells the requester's server how large the Creative plots world now is.
     */
    private void publishCreativeExpanded(UUID requester, int plotsAcross) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("requester", requester.toString());
        json.addProperty("plotsAcross", plotsAcross);
        publish(CH_CREATIVE_EXPAND, json);
    }

    private void handleCreativeExpand(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID requester = UUID.fromString(json.get("requester").getAsString());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            if (json.has("rings")) {
                // The request - only the server with the plots world expands it
                if (!PlotUtils.isPlotServer()) {
                    return;
                }
                int plotsAcross = PlotUtils.expand(json.get("rings").getAsInt());
                publishCreativeExpanded(requester, plotsAcross);
            } else {
                // The result - tell the requester if they are on this server
                Player player = Bukkit.getPlayer(requester);
                if (player == null) {
                    return;
                }
                int plotsAcross = json.get("plotsAcross").getAsInt();
                if (plotsAcross < 0) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.could_not_creative")));
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "creative.expanded", "size", plotsAcross)));
                }
            }
        });
    }

    private void handleCreativePlotDelete(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        String plotKey = json.get("plotKey").getAsString();
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> PlotUtils.unloadPlot(plotKey));
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
            if (data.has("mobSpawningEnabled")) {
                existing.setMobSpawningEnabled(data.get("mobSpawningEnabled").getAsBoolean());
            }
            if (data.has("memberPvpEnabled")) {
                existing.setMemberPvpEnabled(data.get("memberPvpEnabled").getAsBoolean());
            }
            if (data.has("bendingEnabled")) {
                existing.setBendingEnabled(data.get("bendingEnabled").getAsBoolean());
            }
            if (data.has("explosionEnabled")) {
                existing.setExplosionEnabled(data.get("explosionEnabled").getAsBoolean());
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
     * Provides the live job data of every player online on the other server, as their XP is only
     * written to MySQL periodically. Responds with an empty map if the other server is empty or
     * does not answer in time. Results are reused for a few seconds so /jobs top cannot be spammed
     * to flood the network. Must be called from the main thread.
     *
     * @param callback Receives the job data JSON keyed by UUID, on the main thread.
     */
    public void requestRemoteJobData(Consumer<Map<UUID, String>> callback) {
        if (remoteRoster.isEmpty()) {
            callback.accept(Collections.emptyMap());
            return;
        }
        if (cachedRemoteJobData != null && System.currentTimeMillis() - cachedRemoteJobDataAt < 5000L) {
            callback.accept(cachedRemoteJobData);
            return;
        }
        pendingJobTopCallbacks.add(callback);
        if (pendingJobTopRequestId != null) {
            return;
        }

        String requestId = UUID.randomUUID().toString();
        pendingJobTopRequestId = requestId;
        JsonObject json = new JsonObject();
        json.addProperty("fromServer", thisServer);
        json.addProperty("requestId", requestId);
        publish(CH_JOB_TOP_REQUEST, json);

        // Falls back to the database values if the other server does not respond
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (requestId.equals(pendingJobTopRequestId)) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Jobs] No /jobs top response from the other server - using database values");
                completeJobTopRequest(Collections.emptyMap(), false);
            }
        }, 60L);
    }

    private void completeJobTopRequest(Map<UUID, String> jobData, boolean cache) {
        pendingJobTopRequestId = null;
        if (cache) {
            cachedRemoteJobData = jobData;
            cachedRemoteJobDataAt = System.currentTimeMillis();
        }
        List<Consumer<Map<UUID, String>>> callbacks = new ArrayList<>(pendingJobTopCallbacks);
        pendingJobTopCallbacks.clear();
        for (Consumer<Map<UUID, String>> callback : callbacks) {
            callback.accept(jobData);
        }
    }

    /**
     * Received on the other server. Replies with the live job data of every player online here.
     */
    private void handleJobTopRequest(JsonObject json) {
        String fromServer = json.get("fromServer").getAsString();
        if (fromServer.equals(thisServer)) {
            return;
        }

        JsonObject players = new JsonObject();
        for (Player player : Bukkit.getOnlinePlayers()) {
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            if (ap == null || !ap.isJobDataLoaded()) {
                continue;
            }
            players.addProperty(player.getUniqueId().toString(), PersistenceUtils.serializeJobData(ap.getJobData()));
        }

        JsonObject response = new JsonObject();
        response.addProperty("targetServer", fromServer);
        response.addProperty("requestId", json.get("requestId").getAsString());
        response.add("players", players);
        publish(CH_JOB_TOP_RESPONSE, response);
    }

    /**
     * Received on the server that ran /jobs top.
     */
    private void handleJobTopResponse(JsonObject json) {
        String targetServer = json.get("targetServer").getAsString();
        if (!targetServer.equals(thisServer)) {
            return;
        }
        if (!json.get("requestId").getAsString().equals(pendingJobTopRequestId)) {
            return;
        }

        Map<UUID, String> jobData = new HashMap<>();
        JsonObject players = json.getAsJsonObject("players");
        for (String key : players.keySet()) {
            try {
                jobData.put(UUID.fromString(key), players.get(key).getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        completeJobTopRequest(jobData, true);
    }

    /**
     * Notifies other servers that a player's mount progress was saved to the database.
     *
     * @param uuid The UUID of the player whose mounts were saved.
     */
    public void publishMountUpdate(UUID uuid) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        publish(CH_MOUNT_UPDATE, json);
    }

    private void handleMountUpdate(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        if (Bukkit.getPlayer(uuid) != null) {
            PersistenceUtils.loadMountsForPlayer(uuid);
        }
    }

    /**
     * Notifies other servers that a vote was awarded so they can update their in-memory vote list.
     * Called after addVote() and the DB sync on the server that received the VotifierEvent.
     */
    public void publishVoteAward(UUID uuid, int amount, long timestamp) {
        publishVoteAward(uuid, amount, timestamp, null);
    }

    /**
     * Same as publishVoteAward, but also notifies the player with the given lang key if they are online on the other server.
     */
    public void publishVoteAward(UUID uuid, int amount, long timestamp, String notifyLangKey) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("amount", amount);
        json.addProperty("timestamp", timestamp);
        if (notifyLangKey != null) {
            json.addProperty("langKey", notifyLangKey);
        }
        publish(CH_VOTE_AWARD, json);
    }

    private void handleVoteAward(JsonObject json) {
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        int amount = json.get("amount").getAsInt();
        long timestamp = json.get("timestamp").getAsLong();
        AranarthUtils.addVote(new AranarthVote(uuid, amount, timestamp));
        if (json.has("langKey")) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, json.get("langKey").getAsString(), "amount", amount)));
            }
        }
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
                    onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "crate.inventory_full_keyclaim")));
                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[VOTE] Remote key delivery: inventory full for " + onlinePlayer.getName() + " - stored as pending");
                } else {
                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[VOTE] Remote key delivery: key given directly to " + onlinePlayer.getName() + " in " + worldName);
                }
            } else {
                AranarthUtils.addPendingVoteKeys(uuid, 1);
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "crate.cannot_receive_here")));
                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[VOTE] Remote key delivery: " + onlinePlayer.getName() + " is in invalid world (" + worldName + ") - stored as pending");
            }
            if (DatabaseManager.isActive()) {
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                        () -> PersistenceUtils.syncVoteKeysForPlayerToDatabase(uuid));
            }
        });
    }

    /**
     * Tells the remote server to deliver purchased crate keys to the player.
     * Used when Survival processes a store command but the player is currently on SMP.
     *
     * @param uuid     The UUID of the player to receive keys.
     * @param keyType  The key type string ("key_rare", "key_epic", "key_godly").
     * @param quantity The number of keys to deliver.
     */
    public void publishPurchaseKey(UUID uuid, String keyType, int quantity) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("keyType", keyType);
        json.addProperty("quantity", quantity);
        publish(CH_PURCHASE_KEY, json);
    }

    private void handlePurchaseKey(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String keyType = json.get("keyType").getAsString();
        int quantity = json.get("quantity").getAsInt();

        ItemStack key = switch (keyType) {
            case "key_rare" -> new KeyRare().getItem();
            case "key_epic" -> new KeyEpic().getItem();
            case "key_godly" -> new KeyGodly().getItem();
            default -> null;
        };
        if (key == null) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[STORE] Unknown purchase key type: " + keyType);
            return;
        }
        final ItemStack finalKey = key;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            AranarthUtils.addPendingKey(uuid, finalKey, quantity);
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[STORE] Cross-server purchase key for " + uuid + " but player is not online - stored as pending");
            if (DatabaseManager.isActive()) {
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                        () -> PersistenceUtils.syncVoteKeysForPlayerToDatabase(uuid));
            }
            return;
        }

        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                AranarthUtils.addPendingKey(uuid, finalKey, quantity);
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[STORE] Cross-server purchase key: " + uuid + " logged off before delivery - stored as pending");
                if (DatabaseManager.isActive()) {
                    Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                            () -> PersistenceUtils.syncVoteKeysForPlayerToDatabase(uuid));
                }
                return;
            }
            finalKey.setAmount(quantity);
            String worldName = onlinePlayer.getWorld().getName();
            if (AranarthUtils.isSurvivalWorld(worldName)) {
                HashMap<Integer, ItemStack> remainder = onlinePlayer.getInventory().addItem(finalKey);
                if (!remainder.isEmpty()) {
                    AranarthUtils.addPendingKey(uuid, finalKey, quantity);
                    onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "crate.inventory_full_keyclaim")));
                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[STORE] Cross-server purchase key: inventory full for " + onlinePlayer.getName() + " - stored as pending");
                } else {
                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[STORE] Cross-server purchase key: delivered " + keyType + " x" + quantity + " to " + onlinePlayer.getName());
                }
            } else {
                AranarthUtils.addPendingKey(uuid, finalKey, quantity);
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "give.keyclaim_hint")));
                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[STORE] Cross-server purchase key: " + onlinePlayer.getName() + " in invalid world (" + worldName + ") - stored as pending");
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
            receiver.sendMessage(ChatUtils.chatMessage(Lang.getFor(receiver, "economy.received_vote_points", "amount", amount, "player", senderNickname)));
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
                        Lang.getFor(viewer, "admin.invsee_load_failed")));
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

            String targetKey = switch (type) {
                case "ally" -> "dominion.ally_request_received_notify";
                case "truce" -> "dominion.truce_request_received_notify";
                default -> "dominion.neutral_request_received_notify";
            };
            String requesterKey = switch (type) {
                case "ally" -> "dominion.ally_request_sent_notify";
                case "truce" -> "dominion.truce_request_sent_notify";
                default -> "dominion.neutral_request_sent_notify";
            };

            for (Player p : Bukkit.getOnlinePlayers()) {
                int domVol = AranarthUtils.getPlayer(p.getUniqueId()).getDominionSoundVolume();
                if (targetDominion.getMembers().contains(p.getUniqueId())) {
                    if (domVol > 0) {
                        p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                    }
                    p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, targetKey, "name", requesterDominion.getName())));
                } else if (requesterDominion.getMembers().contains(p.getUniqueId())) {
                    if (domVol > 0) {
                        p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                    }
                    p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, requesterKey, "name", targetDominion.getName())));
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
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.notify_neutral", "dominion", b.getName())));
                    } else if (b.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                        }
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.notify_neutral", "dominion", a.getName())));
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
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.notify_conquering_own", "dominion", b.getName())));
                    } else if (b.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_4, 2F * (domVol / 100f), 1F);
                        }
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.notify_conquering", "dominion", a.getName())));
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.conquest_auto_warning")));
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
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.notify_rebellion_own", "dominion", b.getName())));
                    } else if (b.getMembers().contains(p.getUniqueId())) {
                        if (domVol > 0) {
                            p.playSound(p, Sound.ITEM_GOAT_HORN_SOUND_1, 2F * (domVol / 100f), 1F);
                        }
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.notify_rebellion", "dominion", a.getName())));
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.notify_retreat", "dominion", a.getName())));
                        p.sendMessage(ChatUtils.chatMessage(Lang.getFor(p, "dominion.freed_note")));
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
        int streakCount = json.has("streakCount") ? json.get("streakCount").getAsInt() : 0;
        ChatGameUtils.applyNetworkGameWin(AranarthCore.getInstance(), winner, answer, winnerUUID,
                elapsedSeconds, newGlobalRecord, newHolderNickname, newHolderUUID, newGlobalBestTime, streakCount);
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
        // Search all tracked sentinel types by UUID
        org.bukkit.entity.EntityType[] trackedTypes = {
                org.bukkit.entity.EntityType.HORSE,
                org.bukkit.entity.EntityType.IRON_GOLEM,
                org.bukkit.entity.EntityType.WOLF
        };

        for (Map.Entry<UUID, AranarthPlayer> entry : AranarthUtils.getAranarthPlayers().entrySet()) {
            AranarthPlayer ap = entry.getValue();
            if (ap.getSentinels() == null) {
                continue;
            }
            boolean removed = false;
            for (org.bukkit.entity.EntityType trackedType : trackedTypes) {
                java.util.List<com.aearost.aranarthcore.objects.Sentinel> list = ap.getSentinels().get(trackedType);
                if (list == null) {
                    continue;
                }
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
                    PersistenceUtils.syncPlayerSentinelsToDatabase(entry.getKey());
                    removed = true;
                    break;
                }
            }
            if (removed) {
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

    // -------------------------------------------------------------------------
    // Trade - publishers
    // -------------------------------------------------------------------------

    /**
     * Sent by A's server when A invites B who is on another server.
     */
    public void publishTradeInvite(UUID inviterUuid, String inviterName, String inviterUsername, UUID inviteeUuid) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("inviterUuid", inviterUuid.toString());
        json.addProperty("inviterName", inviterName);
        json.addProperty("inviterUsername", inviterUsername);
        json.addProperty("inviteeUuid", inviteeUuid.toString());
        publish(CH_TRADE_INVITE, json);
    }

    /**
     * Sent by B's server when B accepts a cross-server invite from A.
     */
    public void publishTradeAccept(UUID initiatorUuid, UUID targetUuid) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("initiatorUuid", initiatorUuid.toString());
        json.addProperty("targetUuid", targetUuid.toString());
        publish(CH_TRADE_ACCEPT, json);
    }

    /**
     * Sent by either server when a player cancels the trade.
     */
    public void publishTradeCancel(UUID initiatorUuid, UUID cancellerUuid, String cancellerNick) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("initiatorUuid", initiatorUuid.toString());
        json.addProperty("cancellerUuid", cancellerUuid.toString());
        json.addProperty("cancellerNick", cancellerNick);
        publish(CH_TRADE_CANCEL, json);
    }

    /**
     * Sent after a player's offered items change.
     */
    public void publishTradeItems(UUID changerUuid, UUID initiatorUuid, ItemStack[] items) {
        try {
            String base64 = com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayToBase64(items);
            JsonObject json = new JsonObject();
            json.addProperty("server", thisServer);
            json.addProperty("changerUuid", changerUuid.toString());
            json.addProperty("initiatorUuid", initiatorUuid.toString());
            json.addProperty("items", base64);
            publish(CH_TRADE_ITEMS, json);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                    + "Failed to serialize trade items for " + changerUuid + ": " + e.getMessage());
        }
    }

    /**
     * Sent after a player's money offer changes.
     */
    public void publishTradeMoney(UUID changerUuid, UUID initiatorUuid, double amount) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("changerUuid", changerUuid.toString());
        json.addProperty("initiatorUuid", initiatorUuid.toString());
        json.addProperty("amount", amount);
        publish(CH_TRADE_MONEY, json);
    }

    /**
     * Sent when a player toggles their confirm state.
     */
    public void publishTradeConfirm(UUID confirmerUuid, UUID initiatorUuid, boolean confirmed) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("confirmerUuid", confirmerUuid.toString());
        json.addProperty("initiatorUuid", initiatorUuid.toString());
        json.addProperty("confirmed", confirmed);
        publish(CH_TRADE_CONFIRM, json);
    }

    /**
     * Sent by the initiator's server once the initiator's side is in escrow. Carries both offers as
     * the initiator saw them, so the target's server can refuse the trade if anything changed.
     */
    public void publishTradeExecute(UUID commitId, UUID initiatorUuid, ItemStack[] itemsForTarget, double initiatorMoney,
                                    ItemStack[] expectedTargetItems, double targetMoney) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("server", thisServer);
            json.addProperty("commitId", commitId.toString());
            json.addProperty("initiatorUuid", initiatorUuid.toString());
            json.addProperty("itemsForTarget", com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayToBase64(itemsForTarget));
            json.addProperty("initiatorMoney", initiatorMoney);
            json.addProperty("expectedTargetItems", com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayToBase64(expectedTargetItems));
            json.addProperty("targetMoney", targetMoney);
            publish(CH_TRADE_EXECUTE, json);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                    + "Failed to serialize trade execute for " + initiatorUuid + ": " + e.getMessage());
        }
    }

    /**
     * Sent by the target's server after it committed or refused a cross-server trade.
     */
    public void publishTradeResult(UUID commitId, UUID initiatorUuid, boolean success, ItemStack[] itemsForInitiator,
                                   double targetMoney, String targetNick) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("server", thisServer);
            json.addProperty("commitId", commitId.toString());
            json.addProperty("initiatorUuid", initiatorUuid.toString());
            json.addProperty("success", success);
            json.addProperty("itemsForInitiator", com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayToBase64(itemsForInitiator));
            json.addProperty("targetMoney", targetMoney);
            json.addProperty("targetNick", targetNick);
            publish(CH_TRADE_RESULT, json);
        } catch (Exception e) {
            Bukkit.getLogger().severe(AranarthCore.LOG_PREFIX
                    + "Failed to serialize trade result " + commitId + " for " + initiatorUuid + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Trade - handlers
    // -------------------------------------------------------------------------

    private void handleTradeInvite(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID inviterUuid = UUID.fromString(json.get("inviterUuid").getAsString());
        String inviterName = json.get("inviterName").getAsString();
        String inviterUsername = json.has("inviterUsername") ? json.get("inviterUsername").getAsString()
                : com.aearost.aranarthcore.utils.ChatUtils.stripColorFormatting(inviterName);
        UUID inviteeUuid = UUID.fromString(json.get("inviteeUuid").getAsString());
        com.aearost.aranarthcore.utils.TradeManager.onCrossServerInvite(inviterUuid, inviterName, inviterUsername, inviteeUuid);
    }

    private void handleTradeAccept(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID initiatorUuid = UUID.fromString(json.get("initiatorUuid").getAsString());
        UUID targetUuid = UUID.fromString(json.get("targetUuid").getAsString());
        com.aearost.aranarthcore.utils.TradeManager.onCrossServerAccept(initiatorUuid, targetUuid);
    }

    private void handleTradeCancel(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID initiatorUuid = UUID.fromString(json.get("initiatorUuid").getAsString());
        UUID cancellerUuid = UUID.fromString(json.get("cancellerUuid").getAsString());
        String cancellerNick = json.get("cancellerNick").getAsString();
        com.aearost.aranarthcore.utils.TradeManager.onRemoteCancel(initiatorUuid, cancellerUuid, cancellerNick);
    }

    private void handleTradeItems(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID changerUuid = UUID.fromString(json.get("changerUuid").getAsString());
        UUID initiatorUuid = UUID.fromString(json.get("initiatorUuid").getAsString());
        String base64 = json.get("items").getAsString();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                ItemStack[] items = com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayFromBase64(base64);
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                        () -> com.aearost.aranarthcore.utils.TradeManager.onRemoteItemUpdate(
                                changerUuid, initiatorUuid, items));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                        + "Failed to deserialize trade items from " + changerUuid + ": " + e.getMessage());
            }
        });
    }

    private void handleTradeMoney(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID changerUuid = UUID.fromString(json.get("changerUuid").getAsString());
        UUID initiatorUuid = UUID.fromString(json.get("initiatorUuid").getAsString());
        double amount = json.get("amount").getAsDouble();
        com.aearost.aranarthcore.utils.TradeManager.onRemoteMoneyUpdate(changerUuid, initiatorUuid, amount);
    }

    private void handleTradeConfirm(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID confirmerUuid = UUID.fromString(json.get("confirmerUuid").getAsString());
        UUID initiatorUuid = UUID.fromString(json.get("initiatorUuid").getAsString());
        boolean confirmed = json.get("confirmed").getAsBoolean();
        com.aearost.aranarthcore.utils.TradeManager.onRemoteConfirm(confirmerUuid, initiatorUuid, confirmed);
    }

    private void handleTradeExecute(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID commitId = UUID.fromString(json.get("commitId").getAsString());
        UUID initiatorUuid = UUID.fromString(json.get("initiatorUuid").getAsString());
        String itemsBase64 = json.get("itemsForTarget").getAsString();
        String expectedBase64 = json.get("expectedTargetItems").getAsString();
        double initiatorMoney = json.get("initiatorMoney").getAsDouble();
        double targetMoney = json.get("targetMoney").getAsDouble();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                ItemStack[] items = com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayFromBase64(itemsBase64);
                ItemStack[] expected = com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayFromBase64(expectedBase64);
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                        () -> com.aearost.aranarthcore.utils.TradeManager.onRemoteExecute(
                                commitId, initiatorUuid, items, initiatorMoney, expected, targetMoney));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                        + "Failed to deserialize trade execute items for " + initiatorUuid + ": " + e.getMessage());
                // Refuse the trade so the initiator is refunded instead of waiting for the timeout
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () ->
                        publishTradeResult(commitId, initiatorUuid, false, new ItemStack[12], 0, "?"));
            }
        });
    }

    private void handleTradeResult(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID commitId = UUID.fromString(json.get("commitId").getAsString());
        boolean success = json.get("success").getAsBoolean();
        String itemsBase64 = json.get("itemsForInitiator").getAsString();
        double targetMoney = json.get("targetMoney").getAsDouble();
        String targetNick = json.get("targetNick").getAsString();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                ItemStack[] items = com.aearost.aranarthcore.utils.ItemUtils.itemStackArrayFromBase64(itemsBase64);
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                        () -> com.aearost.aranarthcore.utils.TradeManager.onRemoteTradeResult(
                                commitId, success, items, targetMoney, targetNick));
            } catch (Exception e) {
                Bukkit.getLogger().severe(AranarthCore.LOG_PREFIX
                        + "Failed to deserialize trade result " + commitId + ": " + e.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Punishments
    // -------------------------------------------------------------------------

    /**
     * Sends a ban, unban, mute, unmute or warning to the other server so it applies it immediately.
     */
    public void publishPunishment(Punishment punishment, String nickname, String duration, long banExpiresAt, String muteEndDate) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("uuid", punishment.getUuid().toString());
        json.addProperty("type", punishment.getType());
        json.addProperty("reason", punishment.getReason());
        json.addProperty("date", punishment.getDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        json.addProperty("appliedBy", punishment.getAppliedBy() != null ? punishment.getAppliedBy().toString() : "");
        json.addProperty("nickname", nickname);
        json.addProperty("duration", duration);
        json.addProperty("banExpiresAt", banExpiresAt);
        json.addProperty("muteEndDate", muteEndDate != null ? muteEndDate : "");
        publish(CH_PUNISHMENT, json);
    }

    private void handlePunishment(JsonObject json) {
        if (json.get("server").getAsString().equals(thisServer)) {
            return;
        }
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String appliedBy = json.get("appliedBy").getAsString();
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(json.get("date").getAsLong()), ZoneId.systemDefault());
        Punishment punishment = new Punishment(uuid, date, json.get("type").getAsString(), json.get("reason").getAsString(),
                appliedBy.isEmpty() ? null : UUID.fromString(appliedBy));
        PunishmentUtils.onRemotePunishment(punishment, json.get("nickname").getAsString(), json.get("duration").getAsString(),
                json.get("banExpiresAt").getAsLong(), json.get("muteEndDate").getAsString());
    }
}
