package com.axiom.antivpn.common.telemetry;

import com.axiom.antivpn.common.policy.EnforcementAction;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TelemetryStorage implements AutoCloseable {
    private final @NotNull Logger logger;
    private final @NotNull Connection connection;
    private final long maxRows;

    public TelemetryStorage(@NotNull Path dataFolder, @NotNull Logger logger, long maxRows) {
        this.logger = logger;
        this.maxRows = Math.max(1000, maxRows);
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.resolve("antivpn.db"));
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS check_history (id INTEGER PRIMARY KEY AUTOINCREMENT, checked_at INTEGER NOT NULL, player_uuid TEXT, player_name TEXT NOT NULL, ip TEXT NOT NULL, country_code TEXT, detection TEXT NOT NULL, risk_score INTEGER NOT NULL, action TEXT NOT NULL, matched INTEGER NOT NULL, cache_hit INTEGER NOT NULL, api_failure INTEGER NOT NULL)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_check_history_player ON check_history(player_name, checked_at DESC)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_check_history_time ON check_history(checked_at)");
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to open telemetry database", e);
        }
    }

    public synchronized void record(@NotNull CheckRecord record) {
        String sql = "INSERT INTO check_history (checked_at,player_uuid,player_name,ip,country_code,detection,risk_score,action,matched,cache_hit,api_failure) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, record.checkedAt());
            statement.setString(2, record.playerUuid() == null ? null : record.playerUuid().toString());
            statement.setString(3, record.playerName());
            statement.setString(4, record.ip());
            statement.setString(5, record.countryCode());
            statement.setString(6, record.detection());
            statement.setInt(7, record.riskScore());
            statement.setString(8, record.action().name());
            statement.setInt(9, record.matched() ? 1 : 0);
            statement.setInt(10, record.cacheHit() ? 1 : 0);
            statement.setInt(11, record.apiFailure() ? 1 : 0);
            statement.executeUpdate();
            trim();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to record AntiVPN telemetry", e);
        }
    }

    public synchronized @NotNull List<CheckRecord> historyByPlayer(@NotNull String playerName, int limit) {
        int safeLimit = Math.max(1, Math.min(50, limit));
        List<CheckRecord> result = new ArrayList<>();
        String sql = "SELECT checked_at,player_uuid,player_name,ip,country_code,detection,risk_score,action,matched,cache_hit,api_failure FROM check_history WHERE lower(player_name)=lower(?) ORDER BY checked_at DESC LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setInt(2, safeLimit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(read(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to query AntiVPN history", e);
        }
        return result;
    }

    public synchronized @NotNull StatsSnapshot stats() {
        long checks = scalar("SELECT COUNT(*) FROM check_history");
        long blocked = scalar("SELECT COUNT(*) FROM check_history WHERE matched=1");
        return new StatsSnapshot(checks, checks - blocked, blocked,
                scalar("SELECT COUNT(*) FROM check_history WHERE cache_hit=1"),
                scalar("SELECT COUNT(*) FROM check_history WHERE api_failure=1"),
                grouped("detection"), grouped("country_code"));
    }

    public synchronized void purgeOlderThan(@NotNull Duration retention) {
        long cutoff = System.currentTimeMillis() - retention.toMillis();
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM check_history WHERE checked_at < ?")) {
            statement.setLong(1, cutoff);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to purge AntiVPN history", e);
        }
    }

    private long scalar(String sql) {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to aggregate AntiVPN telemetry", e);
            return 0;
        }
    }

    private Map<String, Long> grouped(String column) {
        Map<String, Long> values = new LinkedHashMap<>();
        String sql = "SELECT " + column + ", COUNT(*) AS total FROM check_history WHERE " + column + " IS NOT NULL GROUP BY " + column + " ORDER BY total DESC LIMIT 10";
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) values.put(rs.getString(1), rs.getLong(2));
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to aggregate AntiVPN telemetry", e);
        }
        return values;
    }

    private CheckRecord read(ResultSet rs) throws SQLException {
        String rawUuid = rs.getString("player_uuid");
        UUID uuid = rawUuid == null ? null : UUID.fromString(rawUuid);
        return new CheckRecord(rs.getLong("checked_at"), uuid, rs.getString("player_name"), rs.getString("ip"),
                rs.getString("country_code"), rs.getString("detection"), rs.getInt("risk_score"),
                EnforcementAction.valueOf(rs.getString("action")), rs.getInt("matched") != 0,
                rs.getInt("cache_hit") != 0, rs.getInt("api_failure") != 0);
    }

    private void trim() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM check_history WHERE id IN (SELECT id FROM check_history ORDER BY id DESC LIMIT -1 OFFSET ?)")) {
            statement.setLong(1, maxRows);
            statement.executeUpdate();
        }
    }

    @Override public synchronized void close() {
        try { connection.close(); } catch (SQLException e) { logger.log(Level.WARNING, "Failed to close telemetry database", e); }
    }
}
