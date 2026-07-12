package com.aearost.aranarthsmp.network;

import com.aearost.aranarthsmp.AranarthSMP;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles all cross-server communication for the AranarthSMP companion plugin.
 * Mirrors the NetworkManager in AranarthCore, adapted for the SMP server perspective.
 */
public class SMPNetworkManager {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static SMPNetworkManager instance;

    public static SMPNetworkManager getInstance() { return instance; }

    public static boolean isActive() { return instance != null; }

    public static void initialize(String host, int port, String password, String thisServer) {
        if (instance != null) instance.doShutdown();
        try {
            instance = new SMPNetworkManager(host, port, password, thisServer);
            Bukkit.getLogger().info("[AranarthSMP] Connected to Redis at " + host + ":" + port);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[AranarthSMP] Failed to connect to Redis: " + e.getMessage());
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
    // Channels (must match AranarthCore's NetworkManager)
    // -------------------------------------------------------------------------

    private static final String CH_CHAT       = "aranarth:chat";
    private static final String CH_JOIN       = "aranarth:join";
    private static final String CH_QUIT       = "aranarth:quit";
    private static final String CH_CLOCK      = "aranarth:clock";
    private static final String CH_TP_REQUEST = "aranarth:tp_request";
    private static final String CH_TP_ACCEPT  = "aranarth:tp_accept";
    private static final String CH_TP_DENY    = "aranarth:tp_deny";
    private static final String CH_TRANSFER   = "aranarth:transfer";

    private static final String KEY_ONLINE     = "aranarth:online:";
    private static final String KEY_PENDING_TP = "aranarth:pending_tp:";

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final String thisServer;
    private final JedisPool pool;
    private final JedisPubSub pubSub;
    private final Thread subscriberThread;
    private final Gson gson = new Gson();

    /** All players on the network: both local (SMP) and remote (survival). */
    private final Map<UUID, NetworkPlayer> roster = new ConcurrentHashMap<>();

    /** Synced date from survival server. */
    private int clockDay;
    private int clockWeekday;
    private String clockMonth = "Ignivor";
    private int clockYear;

    /** Cross-server TP requests received from survival, keyed by local player UUID. */
    private final Map<UUID, CrossServerTpContext> pendingRequests = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Constructor / lifecycle
    // -------------------------------------------------------------------------

    private SMPNetworkManager(String host, int port, String password, String thisServer) {
        this.thisServer = thisServer;

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        config.setTestOnBorrow(true);

        if (password != null && !password.isEmpty()) {
            pool = new JedisPool(config, host, port, 2000, password);
        } else {
            pool = new JedisPool(config, host, port, 2000);
        }

        try (Jedis jedis = pool.getResource()) { jedis.ping(); }

        pubSub = buildPubSub();
        subscriberThread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(pubSub, CH_CHAT, CH_JOIN, CH_QUIT, CH_CLOCK,
                        CH_TP_REQUEST, CH_TP_ACCEPT, CH_TP_DENY, CH_TRANSFER);
            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    Bukkit.getLogger().warning("[AranarthSMP] Redis subscriber disconnected: " + e.getMessage());
                }
            }
        }, "AranarthSMP-Redis-Subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    private void doShutdown() {
        try { pubSub.unsubscribe(); } catch (Exception ignored) {}
        subscriberThread.interrupt();
        pool.close();
        Bukkit.getLogger().info("[AranarthSMP] Disconnected from Redis");
    }

    // -------------------------------------------------------------------------
    // Subscriber
    // -------------------------------------------------------------------------

    private JedisPubSub buildPubSub() {
        return new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                    Bukkit.getScheduler().runTask(AranarthSMP.getInstance(), () -> dispatch(channel, json));
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[AranarthSMP] Bad Redis message on " + channel + ": " + e.getMessage());
                }
            }
        };
    }

    private void dispatch(String channel, JsonObject json) {
        switch (channel) {
            case CH_CHAT       -> handleChat(json);
            case CH_JOIN       -> handleJoin(json);
            case CH_QUIT       -> handleQuit(json);
            case CH_CLOCK      -> handleClock(json);
            case CH_TP_REQUEST -> handleTpRequest(json);
            case CH_TP_ACCEPT  -> handleTpAccept(json);
            case CH_TP_DENY    -> handleTpDeny(json);
            case CH_TRANSFER   -> handleTransfer(json);
        }
    }

    // -------------------------------------------------------------------------
    // Publishers
    // -------------------------------------------------------------------------

    /** Publishes a public chat message from an SMP player to the survival server. */
    public void publishChat(String prefix, String chatMessage) {
        JsonObject json = new JsonObject();
        json.addProperty("server", thisServer);
        json.addProperty("prefix", prefix);
        json.addProperty("message", chatMessage);
        publish(CH_CHAT, json);
    }

    /** Called from PlayerJoinListener after player data is ready. */
    public void publishPlayerJoin(UUID uuid, String nickname, int rank, int councilRank,
                                  int saintRank, int architectRank, boolean vanished) {
        // Update local roster
        roster.put(uuid, new NetworkPlayer(uuid, nickname, thisServer, rank, councilRank, saintRank, architectRank, vanished));

        // Update Redis hash
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> fields = new HashMap<>();
            fields.put("nickname", nickname);
            fields.put("server", thisServer);
            fields.put("rank", String.valueOf(rank));
            fields.put("councilRank", String.valueOf(councilRank));
            fields.put("saintRank", String.valueOf(saintRank));
            fields.put("architectRank", String.valueOf(architectRank));
            fields.put("vanished", String.valueOf(vanished));
            jedis.hset(KEY_ONLINE + uuid, fields);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AranarthSMP] Redis: failed to set roster for " + uuid);
        }

        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("nickname", nickname);
        json.addProperty("server", thisServer);
        json.addProperty("rank", rank);
        json.addProperty("councilRank", councilRank);
        json.addProperty("saintRank", saintRank);
        json.addProperty("architectRank", architectRank);
        json.addProperty("vanished", vanished);
        publish(CH_JOIN, json);
    }

    /** Called from PlayerQuitListener. */
    public void publishPlayerQuit(UUID uuid) {
        roster.remove(uuid);
        try (Jedis jedis = pool.getResource()) {
            jedis.del(KEY_ONLINE + uuid);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AranarthSMP] Redis: failed to del roster for " + uuid);
        }
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("server", thisServer);
        publish(CH_QUIT, json);
    }

    public void publishTpRequest(UUID fromUuid, String fromNickname, UUID toUuid, boolean isTpHere) {
        JsonObject json = new JsonObject();
        json.addProperty("fromUuid", fromUuid.toString());
        json.addProperty("fromNickname", fromNickname);
        json.addProperty("fromServer", thisServer);
        json.addProperty("toUuid", toUuid.toString());
        json.addProperty("isTpHere", isTpHere);
        publish(CH_TP_REQUEST, json);
    }

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

    public void publishTpDenied(UUID denierUuid, String denierNickname, UUID requesterUuid) {
        JsonObject json = new JsonObject();
        json.addProperty("denierUuid", denierUuid.toString());
        json.addProperty("denierNickname", denierNickname);
        json.addProperty("requesterUuid", requesterUuid.toString());
        publish(CH_TP_DENY, json);
    }

    // -------------------------------------------------------------------------
    // Pending teleport queue
    // -------------------------------------------------------------------------

    public void setPendingTeleport(UUID uuid, PendingTeleport pending) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(KEY_PENDING_TP + uuid, 300, gson.toJson(pending));
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AranarthSMP] Redis: failed to set pending TP for " + uuid);
        }
    }

    public PendingTeleport getPendingTeleport(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            String data = jedis.get(KEY_PENDING_TP + uuid);
            return data == null ? null : gson.fromJson(data, PendingTeleport.class);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AranarthSMP] Redis: failed to get pending TP for " + uuid);
            return null;
        }
    }

    public void clearPendingTeleport(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(KEY_PENDING_TP + uuid);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AranarthSMP] Redis: failed to clear pending TP for " + uuid);
        }
    }

    // -------------------------------------------------------------------------
    // Server transfer
    // -------------------------------------------------------------------------

    public void transferPlayer(Player player, String targetServer) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF("Connect");
            dos.writeUTF(targetServer);
            player.sendPluginMessage(AranarthSMP.getInstance(), "BungeeCord", bos.toByteArray());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AranarthSMP] Failed to transfer " + player.getName()
                    + " to " + targetServer + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Cross-server TP context
    // -------------------------------------------------------------------------

    public void storeCrossServerTpContext(UUID localUuid, CrossServerTpContext ctx) {
        pendingRequests.put(localUuid, ctx);
    }

    public CrossServerTpContext getCrossServerTpContext(UUID localUuid) {
        return pendingRequests.get(localUuid);
    }

    public void clearCrossServerTpContext(UUID localUuid) {
        pendingRequests.remove(localUuid);
    }

    // -------------------------------------------------------------------------
    // Roster access
    // -------------------------------------------------------------------------

    public Map<UUID, NetworkPlayer> getRoster() { return Collections.unmodifiableMap(roster); }

    public NetworkPlayer getPlayer(UUID uuid) { return roster.get(uuid); }

    public String getThisServer() { return thisServer; }

    /** Returns the current server date string received from survival. */
    public String getClockDateLine() {
        return "Day " + clockDay + " of " + clockMonth + ", Year " + clockYear;
    }

    /** Re-populates local roster from Redis on startup. */
    public void syncRosterFromRedis() {
        try (Jedis jedis = pool.getResource()) {
            Set<String> keys = jedis.keys(KEY_ONLINE + "*");
            for (String key : keys) {
                Map<String, String> fields = jedis.hgetAll(key);
                if (fields.isEmpty()) continue;
                UUID uuid = UUID.fromString(key.substring(KEY_ONLINE.length()));
                roster.put(uuid, new NetworkPlayer(
                        uuid,
                        fields.getOrDefault("nickname", ""),
                        fields.getOrDefault("server", ""),
                        Integer.parseInt(fields.getOrDefault("rank", "0")),
                        Integer.parseInt(fields.getOrDefault("councilRank", "0")),
                        Integer.parseInt(fields.getOrDefault("saintRank", "0")),
                        Integer.parseInt(fields.getOrDefault("architectRank", "0")),
                        Boolean.parseBoolean(fields.getOrDefault("vanished", "false"))
                ));
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AranarthSMP] Failed to sync roster from Redis: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private void handleChat(JsonObject json) {
        String originServer = json.get("server").getAsString();
        if (originServer.equals(thisServer)) return;

        String prefix  = json.get("prefix").getAsString();
        String message = json.get("message").getAsString();

        // Strip legacy colour for display (Paper accepts § codes)
        String formatted = (prefix + message).replace("&", "§").replace("§§", "&");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
        Bukkit.getConsoleSender().sendMessage("[SURVIVAL] " + prefix + message);
    }

    private void handleJoin(JsonObject json) {
        String server = json.get("server").getAsString();
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        roster.put(uuid, new NetworkPlayer(
                uuid,
                json.get("nickname").getAsString(),
                server,
                json.get("rank").getAsInt(),
                json.get("councilRank").getAsInt(),
                json.get("saintRank").getAsInt(),
                json.get("architectRank").getAsInt(),
                json.get("vanished").getAsBoolean()
        ));
        updateTab();
    }

    private void handleQuit(JsonObject json) {
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        // Only remove if NOT a local player (local quits handled by PlayerQuitListener)
        NetworkPlayer np = roster.get(uuid);
        if (np != null && !np.getServer().equals(thisServer)) {
            roster.remove(uuid);
            updateTab();
        }
    }

    private void handleClock(JsonObject json) {
        clockDay     = json.get("day").getAsInt();
        clockWeekday = json.get("weekday").getAsInt();
        clockMonth   = json.get("month").getAsString();
        clockYear    = json.get("year").getAsInt();
        // Tab header is refreshed on the next updateTab() cycle
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

        // Store context for /tpaccept
        storeCrossServerTpContext(toUuid, new CrossServerTpContext(fromUuid, fromNickname, fromServer, isTpHere));

        if (isTpHere) {
            target.sendMessage("§e" + fromNickname + " §7has requested you teleport to them");
        } else {
            target.sendMessage("§e" + fromNickname + " §7has requested to teleport to you");
        }
        target.sendMessage("§7Use §e/tpaccept §7or §e/tpdeny");
    }

    private void handleTpAccept(JsonObject json) {
        UUID accepterUuid     = UUID.fromString(json.get("accepterUuid").getAsString());
        String accepterNick   = json.get("accepterNickname").getAsString();
        String accepterServer = json.get("accepterServer").getAsString();
        UUID requesterUuid    = UUID.fromString(json.get("requesterUuid").getAsString());
        boolean isTpHere      = json.get("isTpHere").getAsBoolean();

        if (accepterServer.equals(thisServer)) return;

        if (isTpHere) {
            // Accepter (on survival) is coming HERE. Set their pending TP and transfer them.
            setPendingTeleport(accepterUuid,
                    new PendingTeleport(requesterUuid.toString(), accepterNick, "§7You have teleported to " + accepterNick));
            JsonObject transfer = new JsonObject();
            transfer.addProperty("uuid", accepterUuid.toString());
            transfer.addProperty("targetServer", thisServer);
            publish(CH_TRANSFER, transfer);

            Player requester = Bukkit.getPlayer(requesterUuid);
            if (requester != null) {
                requester.sendMessage("§e" + accepterNick + " §7has accepted your teleport request");
            }
        } else {
            // Requester (on this server) is going to accepter (on survival).
            setPendingTeleport(requesterUuid,
                    new PendingTeleport(accepterUuid.toString(), "§e§l" + accepterServer.toUpperCase(),
                            "§7You have teleported to " + accepterNick));
            Player requester = Bukkit.getPlayer(requesterUuid);
            if (requester != null) {
                requester.sendMessage("§e" + accepterNick + " §7has accepted your teleport request");
                String survivalServerName = AranarthSMP.getInstance().getConfig()
                        .getString("network.servers.survival", "survival");
                transferPlayer(requester, survivalServerName);
            }
        }
    }

    private void handleTpDeny(JsonObject json) {
        UUID denierUuid   = UUID.fromString(json.get("denierUuid").getAsString());
        String denierNick = json.get("denierNickname").getAsString();
        UUID requesterUuid = UUID.fromString(json.get("requesterUuid").getAsString());

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null) {
            requester.sendMessage("§e" + denierNick + " §7has denied your teleport request");
        }
        clearCrossServerTpContext(requesterUuid);
    }

    private void handleTransfer(JsonObject json) {
        UUID uuid       = UUID.fromString(json.get("uuid").getAsString());
        String toServer = json.get("targetServer").getAsString();

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        String velocityName = AranarthSMP.getInstance().getConfig()
                .getString("network.servers." + toServer, toServer);
        transferPlayer(player, velocityName);
    }

    // -------------------------------------------------------------------------
    // Tab list update
    // -------------------------------------------------------------------------

    /** Updates the tab list for all local players to include the full network roster. */
    public void updateTab() {
        List<NetworkPlayer> all = new ArrayList<>(roster.values());
        all.removeIf(NetworkPlayer::isVanished);
        all.sort((a, b) -> Integer.compare(rankPriority(b), rankPriority(a)));

        long total = roster.values().stream().filter(np -> !np.isVanished()).count();
        String playerOrPlayers = total == 1 ? "player" : "players";
        String tps = String.format("%.1f", Bukkit.getServer().getTPS()[0]);
        String header = "§8§l---------------------\n§6§lThe Realm of Aranarth\n§e" + getClockDateLine()
                + "\n§e" + total + " " + playerOrPlayers + " online §7§l| §eTPS " + tps;
        String footer = "§8§l---------------------";

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.setPlayerListHeader(header);
            viewer.setPlayerListFooter(footer);
        }

        int order = 0;
        for (NetworkPlayer np : all) {
            Player local = Bukkit.getPlayer(np.getUuid());
            String display = buildDisplayName(np);
            if (local != null) {
                local.setPlayerListName(display);
                local.setPlayerListOrder(order);
            }
            order++;
        }
    }

    private String buildDisplayName(NetworkPlayer np) {
        String rankPrefix = rankPrefix(np);
        String name = np.getNickname().isEmpty() ? np.getUuid().toString().substring(0, 8) : np.getNickname();
        String serverTag = np.getServer().equals(thisServer) ? "" : " §8[§7" + np.getServer().toUpperCase() + "§8]";
        return rankPrefix + name + serverTag;
    }

    private String rankPrefix(NetworkPlayer np) {
        if (np.getCouncilRank() == 3) return "§4§l[Admin] §r";
        if (np.getCouncilRank() == 2) return "§c§l[Mod] §r";
        if (np.getCouncilRank() == 1) return "§e§l[Helper] §r";
        if (np.getArchitectRank() >= 1) return "§6§l[Arch] §r";
        return switch (np.getRank()) {
            case 8 -> "§5§l[Emperor] §r";
            case 7 -> "§d§l[King] §r";
            case 6 -> "§b§l[Prince] §r";
            case 5 -> "§3§l[Duke] §r";
            case 4 -> "§2§l[Count] §r";
            case 3 -> "§a§l[Baron] §r";
            case 2 -> "§f§l[Knight] §r";
            case 1 -> "§7§l[Esquire] §r";
            default -> "§8[Peasant] §r";
        };
    }

    private int rankPriority(NetworkPlayer np) {
        if (np.getCouncilRank() >= 1) return 40 + np.getCouncilRank() * 2 + np.getSaintRank();
        if (np.getArchitectRank() >= 1) return 36 + np.getSaintRank();
        return np.getRank() * 4 + np.getSaintRank();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void publish(String channel, JsonObject json) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, json.toString());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AranarthSMP] Redis publish failed on " + channel + ": " + e.getMessage());
        }
    }
}
