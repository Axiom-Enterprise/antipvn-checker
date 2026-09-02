package com.axiom.antivpn.common.check;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WhitelistStorage {

    private final @NotNull Logger logger;
    private final @NotNull Connection connection;

    public WhitelistStorage(@NotNull Path dataFolder, @NotNull Logger logger) {
        this.logger = logger;
        Path dbPath = dataFolder.resolve("whitelist.db");
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS whitelisted_ips (ip TEXT PRIMARY KEY)");
                stmt.execute("CREATE TABLE IF NOT EXISTS whitelisted_players (uuid TEXT PRIMARY KEY, name TEXT)");
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to open whitelist database: " + dbPath, e);
        }
    }

    public synchronized @NotNull List<String> loadIps() {
        List<String> result = new java.util.ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ip FROM whitelisted_ips")) {
            while (rs.next()) {
                result.add(rs.getString("ip"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load whitelisted IPs", e);
        }
        return result;
    }

    public synchronized @NotNull Map<UUID, String> loadPlayers() {
        Map<UUID, String> result = new LinkedHashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, name FROM whitelisted_players")) {
            while (rs.next()) {
                try {
                    result.put(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load whitelisted players", e);
        }
        return result;
    }

    public synchronized void addIp(@NotNull String ip) {
        execute("INSERT OR IGNORE INTO whitelisted_ips (ip) VALUES (?)", ip);
    }

    public synchronized void removeIp(@NotNull String ip) {
        execute("DELETE FROM whitelisted_ips WHERE ip = ?", ip);
    }

    public synchronized void addPlayer(@NotNull UUID uuid, @Nullable String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO whitelisted_players (uuid, name) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to add whitelisted player " + uuid, e);
        }
    }

    public synchronized void removePlayer(@NotNull UUID uuid) {
        execute("DELETE FROM whitelisted_players WHERE uuid = ?", uuid.toString());
    }

    private void execute(@NotNull String sql, @NotNull String param) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, param);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute whitelist query: " + sql, e);
        }
    }

    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to close whitelist database", e);
        }
    }
}
