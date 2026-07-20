package com.aearost.aranarthcore.database;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Central MySQL persistence layer for AranarthCore.
 * Uses HikariCP for connection pooling.
 * Only active when the public server config is enabled and a MySQL connection succeeds.
 */
public class DatabaseManager {

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static DatabaseManager instance;

    public static DatabaseManager getInstance() {
        return instance;
    }

    public static boolean isActive() {
        return instance != null && instance.dataSource != null && !instance.dataSource.isClosed();
    }

    public static void initialize(String host, int port, String database, String username, String password) {
        if (instance != null) {
            instance.doShutdown();
        }
        try {
            instance = new DatabaseManager(host, port, database, username, password);
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "DatabaseManager connected to MySQL at " + host + ":" + port + "/" + database);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, AranarthCore.LOG_PREFIX + "Failed to connect to MySQL: " + e.getMessage(), e);
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
    // Message row record
    // -------------------------------------------------------------------------

    public static class MessageRow {
        public final long id;
        public final String channel;
        public final String payload;

        public MessageRow(long id, String channel, String payload) {
            this.id = id;
            this.channel = channel;
            this.payload = payload;
        }
    }

    // -------------------------------------------------------------------------
    // Last location record
    // -------------------------------------------------------------------------

    public static class LastLocation {
        public final String server;
        public final String world;
        public final double x, y, z;
        public final float yaw, pitch;

        public LastLocation(String server, String world, double x, double y, double z, float yaw, float pitch) {
            this.server = server;
            this.world = world;
            this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch;
        }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final HikariDataSource dataSource;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private DatabaseManager(String host, int port, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
                + "&serverTimezone=UTC&rewriteBatchedStatements=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(600_000);
        config.setPoolName("AranarthCore-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        // Test connectivity
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
        } catch (SQLException e) {
            dataSource.close();
            throw new RuntimeException("MySQL connectivity test failed: " + e.getMessage(), e);
        }

        createTables();
        migrateSchema();
    }

    private void doShutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "DatabaseManager disconnected from MySQL");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // -------------------------------------------------------------------------
    // Schema creation
    // -------------------------------------------------------------------------

    private void createTables() {
        String[] ddl = {
            """
            CREATE TABLE IF NOT EXISTS network_messages (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                channel VARCHAR(64) NOT NULL,
                payload MEDIUMTEXT NOT NULL,
                origin_server VARCHAR(64) NOT NULL,
                created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
                INDEX idx_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS network_roster (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(64) NOT NULL,
                nickname TEXT DEFAULT '',
                server VARCHAR(64) NOT NULL,
                rank INT DEFAULT 0,
                council_rank INT DEFAULT 0,
                saint_rank INT DEFAULT 0,
                architect_rank INT DEFAULT 0,
                vanished TINYINT(1) DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS aranarth_players (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(64) NOT NULL,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_kill_death (
                uuid VARCHAR(36) NOT NULL,
                target_uuid VARCHAR(36) NOT NULL,
                kill_count INT DEFAULT 0,
                death_count INT DEFAULT 0,
                PRIMARY KEY (uuid, target_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_chat_game_guesses (
                uuid VARCHAR(36) PRIMARY KEY,
                guess_count INT NOT NULL DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_votes (
                uuid VARCHAR(36) PRIMARY KEY,
                vote_count INT DEFAULT 0,
                pending_vote_keys INT DEFAULT 0,
                pending_rare_keys INT DEFAULT 0,
                pending_epic_keys INT DEFAULT 0,
                pending_godly_keys INT DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_quest_data (
                uuid VARCHAR(36) PRIMARY KEY,
                quest_state_json MEDIUMTEXT,
                quest_progress_json MEDIUMTEXT,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_login_streaks (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_mail (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                recipient_uuid VARCHAR(36) NOT NULL,
                data_json MEDIUMTEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_recipient (recipient_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_mounts (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_punishments (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_boosts (
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                id INT DEFAULT 1 PRIMARY KEY
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS network_temp_data (
                key_name VARCHAR(128) PRIMARY KEY,
                value_json TEXT NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                INDEX idx_expires (expires_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_date (
                id INT DEFAULT 1 PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_homepads (
                id INT DEFAULT 1 PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_warps (
                id INT DEFAULT 1 PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_locked_containers (
                id INT DEFAULT 1 PRIMARY KEY,
                data_json LONGTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_gates (
                id INT DEFAULT 1 PRIMARY KEY,
                data_json LONGTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_avatars (
                id INT DEFAULT 1 PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_dominions (
                id VARCHAR(36) PRIMARY KEY,
                raw_data LONGTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_dominion_permissions (
                dominion_id VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_dominion_player_perms (
                dominion_id VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_outposts (
                id VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_defenders (
                dominion_id VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_toggles (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_compressible (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_sentinels (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS server_shops (
                id INT DEFAULT 1 PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_shops (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_shop_locations (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_shop_collaborators (
                uuid VARCHAR(36) PRIMARY KEY,
                data_json MEDIUMTEXT NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,
            """
            CREATE TABLE IF NOT EXISTS player_last_location (
                uuid      VARCHAR(36) PRIMARY KEY,
                server    VARCHAR(64) NOT NULL,
                world     VARCHAR(64) NOT NULL,
                x         DOUBLE      NOT NULL,
                y         DOUBLE      NOT NULL,
                z         DOUBLE      NOT NULL,
                yaw       FLOAT       NOT NULL,
                pitch     FLOAT       NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """
        };

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : ddl) {
                stmt.execute(sql.trim());
            }
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "MySQL schema verified/created");
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, AranarthCore.LOG_PREFIX + "Failed to create MySQL schema: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Schema migrations
    // -------------------------------------------------------------------------

    private void migrateSchema() {
        String[] migrations = {
            "ALTER TABLE aranarth_players ADD COLUMN IF NOT EXISTS raw_data LONGTEXT",
            "ALTER TABLE player_votes ADD COLUMN IF NOT EXISTS history_json MEDIUMTEXT",
            "ALTER TABLE network_roster MODIFY COLUMN nickname TEXT DEFAULT ''"
        };
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : migrations) {
                try { stmt.execute(sql); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Schema migration error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Bulk-load methods (DB-primary loading on startup)
    // -------------------------------------------------------------------------

    /** Returns uuid -> raw pipe-delimited player row. */
    public Map<UUID, String> loadAllAranarthPlayersRaw() {
        String sql = "SELECT uuid, raw_data FROM aranarth_players WHERE raw_data IS NOT NULL AND raw_data != ''";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("raw_data"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load raw player data: " + e.getMessage());
        }
        return result;
    }

    /** Row returned by {@link #loadAllPlayerBalances()}. */
    public record BalanceEntry(double balance, String username, String nickname) {}

    /**
     * Returns a map of UUID -> BalanceEntry for all players by parsing raw_data.
     * Includes username and nickname so /baltop can display players not loaded on this server.
     */
    public Map<UUID, BalanceEntry> loadAllPlayerBalances() {
        String sql = "SELECT uuid, username, raw_data FROM aranarth_players WHERE raw_data IS NOT NULL AND raw_data != ''";
        Map<UUID, BalanceEntry> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String[] fields = rs.getString("raw_data").split("\\|");
                if (fields.length > 9) {
                    try {
                        double balance = Double.parseDouble(fields[9]);
                        String username = rs.getString("username");
                        String nickname = fields[1]; // raw_data[1] = nickname
                        result.put(UUID.fromString(rs.getString("uuid")), new BalanceEntry(balance, username, nickname));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load player balances: " + e.getMessage());
        }
        return result;
    }

    /** Saves the raw pipe-delimited player row to raw_data column. */
    public void saveAranarthPlayerRaw(UUID uuid, String rawData) {
        String sql = "INSERT INTO aranarth_players (uuid, username, data_json, raw_data) VALUES (?, '', '', ?) " +
                     "ON DUPLICATE KEY UPDATE raw_data=VALUES(raw_data)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, rawData);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save raw player data for " + uuid + ": " + e.getMessage());
        }
    }

    /** Returns the raw pipe-delimited row for the given uuid, or null if not found. */
    public String loadAranarthPlayerRaw(UUID uuid) {
        String sql = "SELECT raw_data FROM aranarth_players WHERE uuid = ? AND raw_data IS NOT NULL AND raw_data != ''";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("raw_data");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load raw player data for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    /** Returns uuid -> {worldPrefix -> int[]{kills, deaths}} for all players. */
    public Map<UUID, Map<String, int[]>> loadAllKillDeathData() {
        String sql = "SELECT uuid, target_uuid, kill_count, death_count FROM player_kill_death";
        Map<UUID, Map<String, int[]>> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                result.computeIfAbsent(uuid, k -> new HashMap<>())
                      .put(rs.getString("target_uuid"), new int[]{rs.getInt("kill_count"), rs.getInt("death_count")});
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all kill/death data: " + e.getMessage());
        }
        return result;
    }

    /**
     * Upserts the guess count for a single player.
     */
    public void saveChatGameGuessCount(UUID uuid, int guessCount) {
        String sql = """
            INSERT INTO player_chat_game_guesses (uuid, guess_count)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE guess_count = VALUES(guess_count)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, guessCount);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save chat game guess count for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Loads all chat game guess counts.
     * @return Map of UUID to guess count.
     */
    public Map<UUID, Integer> loadAllChatGameGuesses() {
        String sql = "SELECT uuid, guess_count FROM player_chat_game_guesses";
        Map<UUID, Integer> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getInt("guess_count"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load chat game guesses: " + e.getMessage());
        }
        return result;
    }

    /** Saves the vote history JSON blob for a player. */
    public void saveVoteHistory(UUID uuid, String historyJson) {
        String sql = "INSERT INTO player_votes (uuid, vote_count, pending_vote_keys, pending_rare_keys, pending_epic_keys, pending_godly_keys, history_json) " +
                     "VALUES (?, 0, 0, 0, 0, 0, ?) ON DUPLICATE KEY UPDATE history_json=VALUES(history_json)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, historyJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save vote history for " + uuid + ": " + e.getMessage());
        }
    }

    /** Returns all vote data rows: uuid -> int[]{voteCount, voteKeys, rareKeys, epicKeys, godlyKeys} */
    public Map<UUID, int[]> loadAllVoteCounts() {
        String sql = "SELECT uuid, vote_count, pending_vote_keys, pending_rare_keys, pending_epic_keys, pending_godly_keys FROM player_votes";
        Map<UUID, int[]> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), new int[]{
                    rs.getInt("vote_count"),
                    rs.getInt("pending_vote_keys"),
                    rs.getInt("pending_rare_keys"),
                    rs.getInt("pending_epic_keys"),
                    rs.getInt("pending_godly_keys")
                });
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load vote counts: " + e.getMessage());
        }
        return result;
    }

    /** Returns uuid -> history_json for players that have vote history stored. */
    public Map<UUID, String> loadAllVoteHistories() {
        String sql = "SELECT uuid, history_json FROM player_votes WHERE history_json IS NOT NULL AND history_json != ''";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("history_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load vote histories: " + e.getMessage());
        }
        return result;
    }

    /** Returns uuid -> [quest_state_json, quest_progress_json] for all rows in player_quest_data. */
    public Map<UUID, String[]> loadAllQuestData() {
        String sql = "SELECT uuid, quest_state_json, quest_progress_json FROM player_quest_data";
        Map<UUID, String[]> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")),
                    new String[]{rs.getString("quest_state_json"), rs.getString("quest_progress_json")});
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all quest data: " + e.getMessage());
        }
        return result;
    }

    /** Returns uuid -> data_json for all login streak rows. */
    public Map<UUID, String> loadAllLoginStreaks() {
        String sql = "SELECT uuid, data_json FROM player_login_streaks";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all login streaks: " + e.getMessage());
        }
        return result;
    }

    /** Returns recipient_uuid -> data_json for all mail rows (one row per recipient). */
    public Map<UUID, String> loadAllMailData() {
        String sql = "SELECT recipient_uuid, data_json FROM player_mail";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("recipient_uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all mail: " + e.getMessage());
        }
        return result;
    }

    /** Returns uuid -> data_json for all mount rows. */
    public Map<UUID, String> loadAllMountsData() {
        String sql = "SELECT uuid, data_json FROM player_mounts";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all mounts: " + e.getMessage());
        }
        return result;
    }

    /** Returns uuid -> data_json for all punishment rows. */
    public Map<UUID, String> loadAllPunishmentsData() {
        String sql = "SELECT uuid, data_json FROM server_punishments";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all punishments: " + e.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Cross-server messaging
    // -------------------------------------------------------------------------

    public void publishMessage(String channel, String payloadJson, String originServer) {
        String sql = "INSERT INTO network_messages (channel, payload, origin_server) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channel);
            ps.setString(2, payloadJson);
            ps.setString(3, originServer);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to publish message on " + channel + ": " + e.getMessage());
        }
    }

    /**
     * Polls for new messages since lastMessageId, excluding messages from thisServer.
     */
    public List<MessageRow> pollMessages(long lastMessageId, String thisServer) {
        String sql = "SELECT id, channel, payload FROM network_messages WHERE id > ? AND origin_server != ? ORDER BY id ASC";
        List<MessageRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lastMessageId);
            ps.setString(2, thisServer);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new MessageRow(rs.getLong("id"), rs.getString("channel"), rs.getString("payload")));
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to poll messages: " + e.getMessage());
        }
        return rows;
    }

    /** Returns the current max message id (0 if table is empty). Used on startup to avoid replaying old messages. */
    public long getMaxMessageId() {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM network_messages";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to get max message id: " + e.getMessage());
        }
        return 0L;
    }

    /** Deletes messages older than 5 minutes to keep the table small. */
    public void cleanupMessages() {
        String sql = "DELETE FROM network_messages WHERE created_at < DATE_SUB(NOW(3), INTERVAL 5 MINUTE)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to cleanup messages: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Roster methods
    // -------------------------------------------------------------------------

    public void upsertRosterEntry(UUID uuid, String username, String nickname, String server,
                                  int rank, int councilRank, int saintRank, int architectRank, boolean vanished) {
        String sql = """
            INSERT INTO network_roster (uuid, username, nickname, server, rank, council_rank, saint_rank, architect_rank, vanished)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE username=VALUES(username), nickname=VALUES(nickname), server=VALUES(server),
            rank=VALUES(rank), council_rank=VALUES(council_rank), saint_rank=VALUES(saint_rank),
            architect_rank=VALUES(architect_rank), vanished=VALUES(vanished)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            String safeNick = nickname != null ? nickname : "";
            if (safeNick.length() > 750) safeNick = safeNick.substring(0, 750);
            ps.setString(3, safeNick);
            ps.setString(4, server);
            ps.setInt(5, rank);
            ps.setInt(6, councilRank);
            ps.setInt(7, saintRank);
            ps.setInt(8, architectRank);
            ps.setBoolean(9, vanished);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to upsert roster entry for " + uuid + ": " + e.getMessage());
        }
    }

    public void removeRosterEntry(UUID uuid) {
        String sql = "DELETE FROM network_roster WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to remove roster entry for " + uuid + ": " + e.getMessage());
        }
    }

    /** Removes all roster entries for the given server. Used on startup/shutdown to prevent stale entries. */
    public void clearRosterForServer(String server) {
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM network_roster WHERE server = ?")) {
            stmt.setString(1, server);
            stmt.executeUpdate();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to clear roster for server " + server + ": " + e.getMessage());
        }
    }

    /** Loads all roster entries NOT from thisServer. */
    public Map<UUID, NetworkPlayer> loadRemoteRoster(String thisServer) {
        String sql = "SELECT uuid, username, nickname, server, rank, council_rank, saint_rank, architect_rank, vanished FROM network_roster WHERE server != ?";
        Map<UUID, NetworkPlayer> roster = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, thisServer);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    roster.put(uuid, new NetworkPlayer(
                            uuid,
                            rs.getString("username"),
                            rs.getString("nickname"),
                            rs.getString("server"),
                            rs.getInt("rank"),
                            rs.getInt("council_rank"),
                            rs.getInt("saint_rank"),
                            rs.getInt("architect_rank"),
                            rs.getBoolean("vanished"),
                            "", "" // textures not stored in DB roster; populated via CH_JOIN
                    ));
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load remote roster: " + e.getMessage());
        }
        return roster;
    }

    // -------------------------------------------------------------------------
    // aranarth_players
    // -------------------------------------------------------------------------

    public void saveAranarthPlayer(UUID uuid, String username, String dataJson) {
        String sql = """
            INSERT INTO aranarth_players (uuid, username, data_json)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE username=VALUES(username), data_json=VALUES(data_json)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setString(3, dataJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save aranarth player " + uuid + ": " + e.getMessage());
        }
    }

    /** Returns the data_json for the given uuid, or null if not found. */
    public String loadAranarthPlayerJson(UUID uuid) {
        String sql = "SELECT data_json FROM aranarth_players WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load aranarth player " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // player_kill_death
    // -------------------------------------------------------------------------

    /**
     * Saves kill/death data for a player. The data map uses worldPrefix strings as keys
     * (the same strings stored in kills_and_deaths.txt) mapped to int[]{kills, deaths}.
     */
    public void saveKillDeathData(UUID uuid, Map<String, int[]> data) {
        String deleteSql = "DELETE FROM player_kill_death WHERE uuid = ?";
        String insertSql = "INSERT INTO player_kill_death (uuid, target_uuid, kill_count, death_count) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                for (Map.Entry<String, int[]> entry : data.entrySet()) {
                    ins.setString(1, uuid.toString());
                    ins.setString(2, entry.getKey()); // worldPrefix stored as target_uuid column
                    ins.setInt(3, entry.getValue()[0]);
                    ins.setInt(4, entry.getValue()[1]);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save kill/death data for " + uuid + ": " + e.getMessage());
        }
    }

    /** Returns worldPrefix -> int[]{kills, deaths} */
    public Map<String, int[]> loadKillDeathData(UUID uuid) {
        String sql = "SELECT target_uuid, kill_count, death_count FROM player_kill_death WHERE uuid = ?";
        Map<String, int[]> data = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.put(rs.getString("target_uuid"), new int[]{rs.getInt("kill_count"), rs.getInt("death_count")});
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load kill/death data for " + uuid + ": " + e.getMessage());
        }
        return data;
    }

    // -------------------------------------------------------------------------
    // player_votes
    // -------------------------------------------------------------------------

    public void saveVoteData(UUID uuid, int voteCount, int voteKeys, int rareKeys, int epicKeys, int godlyKeys) {
        String sql = """
            INSERT INTO player_votes (uuid, vote_count, pending_vote_keys, pending_rare_keys, pending_epic_keys, pending_godly_keys)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE vote_count=VALUES(vote_count), pending_vote_keys=VALUES(pending_vote_keys),
            pending_rare_keys=VALUES(pending_rare_keys), pending_epic_keys=VALUES(pending_epic_keys),
            pending_godly_keys=VALUES(pending_godly_keys)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, voteCount);
            ps.setInt(3, voteKeys);
            ps.setInt(4, rareKeys);
            ps.setInt(5, epicKeys);
            ps.setInt(6, godlyKeys);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save vote data for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Updates only the vote_count for a player. Used by the periodic sync so that
     * pending key counts written by explicit claim/award events are never overwritten
     * by potentially stale in-memory values from the other server.
     */
    public void saveVoteCountOnly(UUID uuid, int voteCount) {
        String sql = """
            INSERT INTO player_votes (uuid, vote_count, pending_vote_keys, pending_rare_keys, pending_epic_keys, pending_godly_keys)
            VALUES (?, ?, 0, 0, 0, 0)
            ON DUPLICATE KEY UPDATE vote_count=VALUES(vote_count)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, voteCount);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save vote count for " + uuid + ": " + e.getMessage());
        }
    }

    /** Returns [voteCount, voteKeys, rareKeys, epicKeys, godlyKeys], or null if not found. */
    public int[] loadVoteData(UUID uuid) {
        String sql = "SELECT vote_count, pending_vote_keys, pending_rare_keys, pending_epic_keys, pending_godly_keys FROM player_votes WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{
                        rs.getInt("vote_count"),
                        rs.getInt("pending_vote_keys"),
                        rs.getInt("pending_rare_keys"),
                        rs.getInt("pending_epic_keys"),
                        rs.getInt("pending_godly_keys")
                    };
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load vote data for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // player_quest_data
    // -------------------------------------------------------------------------

    public void saveQuestData(UUID uuid, String questStateJson, String questProgressJson) {
        String sql = """
            INSERT INTO player_quest_data (uuid, quest_state_json, quest_progress_json)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE quest_state_json=VALUES(quest_state_json), quest_progress_json=VALUES(quest_progress_json)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, questStateJson);
            ps.setString(3, questProgressJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save quest data for " + uuid + ": " + e.getMessage());
        }
    }

    /** Returns [questStateJson, questProgressJson], or null if not found. */
    public String[] loadQuestData(UUID uuid) {
        String sql = "SELECT quest_state_json, quest_progress_json FROM player_quest_data WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString("quest_state_json"), rs.getString("quest_progress_json")};
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load quest data for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // player_login_streaks
    // -------------------------------------------------------------------------

    public void saveLoginStreak(UUID uuid, String dataJson) {
        String sql = """
            INSERT INTO player_login_streaks (uuid, data_json)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, dataJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save login streak for " + uuid + ": " + e.getMessage());
        }
    }

    public String loadLoginStreak(UUID uuid) {
        String sql = "SELECT data_json FROM player_login_streaks WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load login streak for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // player_mail  (stored as one JSON blob per recipient)
    // -------------------------------------------------------------------------

    public void saveAllMail(UUID recipientUuid, String dataJson) {
        // Delete existing rows for this recipient then insert a single blob row
        String deleteSql = "DELETE FROM player_mail WHERE recipient_uuid = ?";
        String insertSql = "INSERT INTO player_mail (recipient_uuid, data_json) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                del.setString(1, recipientUuid.toString());
                del.executeUpdate();
            }
            if (dataJson != null && !dataJson.isEmpty()) {
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    ins.setString(1, recipientUuid.toString());
                    ins.setString(2, dataJson);
                    ins.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save mail for " + recipientUuid + ": " + e.getMessage());
        }
    }

    public String loadAllMail(UUID recipientUuid) {
        String sql = "SELECT data_json FROM player_mail WHERE recipient_uuid = ? ORDER BY created_at ASC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipientUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load mail for " + recipientUuid + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // player_mounts
    // -------------------------------------------------------------------------

    public void saveMountData(UUID uuid, String dataJson) {
        String sql = """
            INSERT INTO player_mounts (uuid, data_json)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, dataJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save mount data for " + uuid + ": " + e.getMessage());
        }
    }

    public String loadMountData(UUID uuid) {
        String sql = "SELECT data_json FROM player_mounts WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load mount data for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // server_punishments  (single row per player, JSON blob)
    // -------------------------------------------------------------------------

    public void savePunishments(UUID uuid, String dataJson) {
        String sql = """
            INSERT INTO server_punishments (uuid, data_json)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, dataJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save punishments for " + uuid + ": " + e.getMessage());
        }
    }

    public String loadPunishments(UUID uuid) {
        String sql = "SELECT data_json FROM server_punishments WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load punishments for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // server_boosts  (single row, id=1)
    // -------------------------------------------------------------------------

    public void saveBoosts(String dataJson) {
        String sql = """
            INSERT INTO server_boosts (id, data_json)
            VALUES (1, ?)
            ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save boosts: " + e.getMessage());
        }
    }

    public String loadBoosts() {
        String sql = "SELECT data_json FROM server_boosts WHERE id = 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("data_json");
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load boosts: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // network_temp_data  (pending teleports + return locations)
    // -------------------------------------------------------------------------

    public void saveTempData(String key, String valueJson, int ttlSeconds) {
        String sql = """
            INSERT INTO network_temp_data (key_name, value_json, expires_at)
            VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? SECOND))
            ON DUPLICATE KEY UPDATE value_json=VALUES(value_json), expires_at=VALUES(expires_at)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, valueJson);
            ps.setInt(3, ttlSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save temp data key=" + key + ": " + e.getMessage());
        }
    }

    /** Returns value_json or null if key not found or expired. */
    public String loadTempData(String key) {
        String sql = "SELECT value_json FROM network_temp_data WHERE key_name = ? AND expires_at > NOW()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("value_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load temp data key=" + key + ": " + e.getMessage());
        }
        return null;
    }

    public void deleteTempData(String key) {
        String sql = "DELETE FROM network_temp_data WHERE key_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to delete temp data key=" + key + ": " + e.getMessage());
        }
    }

    /** Cleans up expired temp data rows. */
    public void cleanupTempData() {
        String sql = "DELETE FROM network_temp_data WHERE expires_at <= NOW()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to cleanup temp data: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // server_date  (singleton row, id=1)
    // -------------------------------------------------------------------------

    public void saveServerDate(String dataJson) {
        String sql = "INSERT INTO server_date (id, data_json) VALUES (1, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save server_date: " + e.getMessage());
        }
    }

    public String loadServerDate() {
        String sql = "SELECT data_json FROM server_date WHERE id = 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("data_json");
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load server_date: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // server_homepads  (singleton row, id=1)
    // -------------------------------------------------------------------------

    public void saveHomepads(String dataJson) {
        String sql = "INSERT INTO server_homepads (id, data_json) VALUES (1, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save homepads: " + e.getMessage());
        }
    }

    public String loadHomepads() {
        String sql = "SELECT data_json FROM server_homepads WHERE id = 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("data_json");
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load homepads: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // server_warps  (singleton row, id=1)
    // -------------------------------------------------------------------------

    public void saveWarps(String dataJson) {
        String sql = "INSERT INTO server_warps (id, data_json) VALUES (1, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save warps: " + e.getMessage());
        }
    }

    public String loadWarps() {
        String sql = "SELECT data_json FROM server_warps WHERE id = 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("data_json");
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load warps: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // server_locked_containers  (singleton row, id=1)
    // -------------------------------------------------------------------------

    public void saveLockedContainers(String dataJson) {
        String sql = "INSERT INTO server_locked_containers (id, data_json) VALUES (1, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save locked containers: " + e.getMessage());
        }
    }

    public String loadLockedContainers() {
        String sql = "SELECT data_json FROM server_locked_containers WHERE id = 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("data_json");
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load locked containers: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // server_gates  (singleton row, id=1)
    // -------------------------------------------------------------------------

    public void saveGates(String dataJson) {
        String sql = "INSERT INTO server_gates (id, data_json) VALUES (1, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save gates: " + e.getMessage());
        }
    }

    public String loadGates() {
        String sql = "SELECT data_json FROM server_gates WHERE id = 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("data_json");
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load gates: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // server_avatars  (singleton row, id=1)
    // -------------------------------------------------------------------------

    public void saveAvatars(String dataJson) {
        String sql = "INSERT INTO server_avatars (id, data_json) VALUES (1, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save avatars: " + e.getMessage());
        }
    }

    public String loadAvatars() {
        String sql = "SELECT data_json FROM server_avatars WHERE id = 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("data_json");
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load avatars: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // server_dominions  (per-dominion raw pipe-delimited rows)
    // -------------------------------------------------------------------------

    public void saveDominion(UUID id, String rawData) {
        String sql = "INSERT INTO server_dominions (id, raw_data) VALUES (?, ?) ON DUPLICATE KEY UPDATE raw_data=VALUES(raw_data)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString()); ps.setString(2, rawData); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save dominion " + id + ": " + e.getMessage());
        }
    }

    public String loadDominion(UUID id) {
        String sql = "SELECT raw_data FROM server_dominions WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("raw_data");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load dominion " + id + ": " + e.getMessage());
        }
        return null;
    }

    public Map<UUID, String> loadAllDominions() {
        String sql = "SELECT id, raw_data FROM server_dominions";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("id")), rs.getString("raw_data"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all dominions: " + e.getMessage());
        }
        return result;
    }

    public void deleteDominion(UUID id) {
        String sql = "DELETE FROM server_dominions WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to delete dominion " + id + ": " + e.getMessage());
        }
    }

    public void deleteDominionPermissions(UUID dominionId) {
        String sql = "DELETE FROM server_dominion_permissions WHERE dominion_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dominionId.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to delete dominion permissions for " + dominionId + ": " + e.getMessage());
        }
    }

    public void deleteDominionPlayerPerms(UUID dominionId) {
        String sql = "DELETE FROM server_dominion_player_perms WHERE dominion_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dominionId.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to delete dominion player perms for " + dominionId + ": " + e.getMessage());
        }
    }

    public void deleteDefendersForDominion(UUID dominionId) {
        String sql = "DELETE FROM server_defenders WHERE dominion_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dominionId.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to delete defenders for dominion " + dominionId + ": " + e.getMessage());
        }
    }

    public void saveDominionPermissions(UUID dominionId, String dataJson) {
        String sql = "INSERT INTO server_dominion_permissions (dominion_id, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dominionId.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save dominion permissions for " + dominionId + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllDominionPermissions() {
        String sql = "SELECT dominion_id, data_json FROM server_dominion_permissions";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("dominion_id")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all dominion permissions: " + e.getMessage());
        }
        return result;
    }

    public String loadDominionPermissionsById(UUID dominionId) {
        String sql = "SELECT data_json FROM server_dominion_permissions WHERE dominion_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dominionId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load permissions for dominion " + dominionId + ": " + e.getMessage());
        }
        return null;
    }

    public String loadDominionPlayerPermsById(UUID dominionId) {
        String sql = "SELECT data_json FROM server_dominion_player_perms WHERE dominion_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dominionId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load player perms for dominion " + dominionId + ": " + e.getMessage());
        }
        return null;
    }

    public void saveDominionPlayerPerms(UUID dominionId, String dataJson) {
        String sql = "INSERT INTO server_dominion_player_perms (dominion_id, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dominionId.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save dominion player perms for " + dominionId + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllDominionPlayerPerms() {
        String sql = "SELECT dominion_id, data_json FROM server_dominion_player_perms";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("dominion_id")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all dominion player perms: " + e.getMessage());
        }
        return result;
    }

    public void saveOutpost(UUID id, String dataJson) {
        String sql = "INSERT INTO server_outposts (id, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save outpost " + id + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllOutposts() {
        String sql = "SELECT id, data_json FROM server_outposts";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("id")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all outposts: " + e.getMessage());
        }
        return result;
    }

    public void deleteOutpost(UUID id) {
        String sql = "DELETE FROM server_outposts WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to delete outpost " + id + ": " + e.getMessage());
        }
    }

    public void saveDefendersForDominion(UUID dominionId, String dataJson) {
        String sql = "INSERT INTO server_defenders (dominion_id, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dominionId.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save defenders for dominion " + dominionId + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllDefenders() {
        String sql = "SELECT dominion_id, data_json FROM server_defenders";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("dominion_id")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all defenders: " + e.getMessage());
        }
        return result;
    }

    public void savePlayerToggles(UUID uuid, String dataJson) {
        String sql = "INSERT INTO player_toggles (uuid, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save player toggles for " + uuid + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllPlayerToggles() {
        String sql = "SELECT uuid, data_json FROM player_toggles";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all player toggles: " + e.getMessage());
        }
        return result;
    }

    public String loadPlayerToggles(UUID uuid) {
        String sql = "SELECT data_json FROM player_toggles WHERE uuid=?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("data_json");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load player toggles for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    public void savePlayerCompressible(UUID uuid, String dataJson) {
        String sql = "INSERT INTO player_compressible (uuid, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save player compressible for " + uuid + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllPlayerCompressible() {
        String sql = "SELECT uuid, data_json FROM player_compressible";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all player compressible: " + e.getMessage());
        }
        return result;
    }

    public void savePlayerSentinels(UUID uuid, String dataJson) {
        String sql = "INSERT INTO player_sentinels (uuid, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save player sentinels for " + uuid + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllPlayerSentinels() {
        String sql = "SELECT uuid, data_json FROM player_sentinels";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all player sentinels: " + e.getMessage());
        }
        return result;
    }

    public void saveServerShops(String dataJson) {
        String sql = "INSERT INTO server_shops (id, data_json) VALUES (1, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save server shops: " + e.getMessage());
        }
    }

    public String loadServerShops() {
        String sql = "SELECT data_json FROM server_shops WHERE id = 1";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("data_json");
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load server shops: " + e.getMessage());
        }
        return null;
    }

    public void savePlayerShops(UUID uuid, String dataJson) {
        String sql = "INSERT INTO player_shops (uuid, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save player shops for " + uuid + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllPlayerShops() {
        String sql = "SELECT uuid, data_json FROM player_shops";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all player shops: " + e.getMessage());
        }
        return result;
    }

    public void savePlayerShopLocation(UUID uuid, String dataJson) {
        String sql = "INSERT INTO player_shop_locations (uuid, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save player shop location for " + uuid + ": " + e.getMessage());
        }
    }

    public void deletePlayerShopLocation(UUID uuid) {
        String sql = "DELETE FROM player_shop_locations WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to delete player shop location for " + uuid + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllPlayerShopLocations() {
        String sql = "SELECT uuid, data_json FROM player_shop_locations";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all player shop locations: " + e.getMessage());
        }
        return result;
    }

    public void savePlayerShopCollaborators(UUID uuid, String dataJson) {
        String sql = "INSERT INTO player_shop_collaborators (uuid, data_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE data_json=VALUES(data_json)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString()); ps.setString(2, dataJson); ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save player shop collaborators for " + uuid + ": " + e.getMessage());
        }
    }

    public Map<UUID, String> loadAllPlayerShopCollaborators() {
        String sql = "SELECT uuid, data_json FROM player_shop_collaborators";
        Map<UUID, String> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(UUID.fromString(rs.getString("uuid")), rs.getString("data_json"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load all player shop collaborators: " + e.getMessage());
        }
        return result;
    }

    public void saveLastLocation(UUID uuid, String server, String world,
                                 double x, double y, double z, float yaw, float pitch) {
        String sql = """
            INSERT INTO player_last_location (uuid, server, world, x, y, z, yaw, pitch)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE server=VALUES(server), world=VALUES(world),
            x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, server);
            ps.setString(3, world);
            ps.setDouble(4, x);
            ps.setDouble(5, y);
            ps.setDouble(6, z);
            ps.setFloat(7, yaw);
            ps.setFloat(8, pitch);
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to save last location for " + uuid + ": " + e.getMessage());
        }
    }

    /** Returns the player's last logout location, or null if none recorded yet. */
    public LastLocation loadLastLocation(UUID uuid) {
        String sql = "SELECT server, world, x, y, z, yaw, pitch FROM player_last_location WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new LastLocation(
                            rs.getString("server"),
                            rs.getString("world"),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load last location for " + uuid + ": " + e.getMessage());
        }
        return null;
    }
}
