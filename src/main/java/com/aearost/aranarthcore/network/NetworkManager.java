package com.aearost.aranarthcore.network;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ItemUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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

    public static final String CH_CHAT       = "aranarth:chat";
    public static final String CH_JOIN       = "aranarth:join";
    public static final String CH_QUIT       = "aranarth:quit";
    public static final String CH_TP_REQUEST = "aranarth:tp_request";
    public static final String CH_TP_ACCEPT  = "aranarth:tp_accept";
    public static final String CH_TP_DENY    = "aranarth:tp_deny";
    public static final String CH_TRANSFER   = "aranarth:transfer";

    // Temp-data key prefixes
    private static final String KEY_PENDING_TP = "pending_tp:";
    private static final String KEY_RETURN_LOC = "return_loc:";

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

    /** Pending cross-server TP requests received from another server. */
    private final Map<UUID, CrossServerTpContext> pendingCrossServerRequests = new ConcurrentHashMap<>();

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
        // Cleanup old messages every 5 minutes (6000 ticks)
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                AranarthCore.getInstance(),
                () -> {
                    db.cleanupMessages();
                    db.cleanupTempData();
                },
                6000L,
                6000L
        );
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
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "NetworkManager shut down");
    }

    // -------------------------------------------------------------------------
    // Subscriber dispatch
    // -------------------------------------------------------------------------

    private void dispatch(String channel, JsonObject json) {
        switch (channel) {
            case CH_CHAT       -> handleChat(json);
            case CH_JOIN       -> handleJoin(json);
            case CH_QUIT       -> handleQuit(json);
            case CH_TP_REQUEST -> handleTpRequest(json);
            case CH_TP_ACCEPT  -> handleTpAccept(json);
            case CH_TP_DENY    -> handleTpDeny(json);
            case CH_TRANSFER   -> handleTransfer(json);
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
        publish(CH_JOIN, json);
    }

    /** Called from PlayerServerQuitListener. */
    public void publishPlayerQuit(UUID uuid) {
        // Remove from roster in DB
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
            db.removeRosterEntry(uuid)
        );

        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("server", thisServer);
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
        if (AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
            if (ap != null) {
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
                AranarthUtils.setPlayer(uuid, ap);
            }
        }

        pending.setApplyInventory(true);

        // Build the serialized forms now, on the main thread, so the async task only does I/O.
        final String rawRow = PersistenceUtils.buildPlayerRowForTransfer(uuid);
        final String toggleJson = PersistenceUtils.buildPlayerToggleJson(uuid);
        final String pendingJson = gson.toJson(pending);

        transferringPlayers.add(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                if (rawRow != null && DatabaseManager.isActive()) {
                    DatabaseManager.getInstance().saveAranarthPlayerRaw(uuid, rawRow);
                }
                if (toggleJson != null && DatabaseManager.isActive()) {
                    DatabaseManager.getInstance().savePlayerToggles(uuid, toggleJson);
                }
                db.saveTempData(KEY_PENDING_TP + uuid, pendingJson, 300);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                        + "DB write before transfer failed for " + player.getName() + ": " + e.getMessage());
            }
            // Send the BungeeCord Connect message on the main thread after both writes complete.
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (!player.isOnline()) {
                    transferringPlayers.remove(uuid);
                    return;
                }
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(bos);
                    dos.writeUTF("Connect");
                    dos.writeUTF(targetServer);
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
        NetworkPlayer np = new NetworkPlayer(
                uuid,
                username,
                json.get("nickname").getAsString(),
                server,
                json.get("rank").getAsInt(),
                json.get("councilRank").getAsInt(),
                json.get("saintRank").getAsInt(),
                json.get("architectRank").getAsInt(),
                vanished
        );
        remoteRoster.put(uuid, np);
        if (!vanished) {
            NetworkTabManager.addToTab(np);
        }
        AranarthUtils.updateTab();
    }

    private void handleQuit(JsonObject json) {
        String server = json.get("server").getAsString();
        if (server.equals(thisServer)) return;

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        remoteRoster.remove(uuid);
        NetworkTabManager.removeFromTab(uuid);
        AranarthUtils.updateTab();
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

        if (isTpHere) {
            targetAp.setTeleportToUuid(fromUuid);
        } else {
            targetAp.setTeleportFromUuid(fromUuid);
        }
        AranarthUtils.setPlayer(toUuid, targetAp);

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
            setPendingTeleport(accepterUuid,
                    new PendingTeleport(requesterUuid.toString(), accepterNick, "&7You have teleported to " + accepterNick));
            JsonObject transfer = new JsonObject();
            transfer.addProperty("uuid", accepterUuid.toString());
            transfer.addProperty("targetServer", thisServer);
            publish(CH_TRANSFER, transfer);
            Player requester = Bukkit.getPlayer(requesterUuid);
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
