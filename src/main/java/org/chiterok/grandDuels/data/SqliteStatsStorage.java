package org.chiterok.grandDuels.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

/** SQLite backend using the sqlite-jdbc driver that ships with Paper. */
public final class SqliteStatsStorage implements StatsStorage {

    private final File dbFile;
    private Connection connection;

    public SqliteStatsStorage(File dataFolder) {
        this.dbFile = new File(dataFolder, "stats.db");
    }

    @Override
    public void init() throws SQLException {
        File parent = dbFile.getParentFile();
        if (parent != null) parent.mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS duel_stats ("
                    + "uuid TEXT PRIMARY KEY, name TEXT NOT NULL, wins INTEGER NOT NULL, losses INTEGER NOT NULL, "
                    + "kills INTEGER NOT NULL, deaths INTEGER NOT NULL, streak INTEGER NOT NULL, "
                    + "best_streak INTEGER NOT NULL)");
        }
    }

    @Override
    public Optional<PlayerStats> load(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name, wins, losses, kills, deaths, streak, best_streak FROM duel_stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PlayerStats(uuid, rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                        rs.getInt(5), rs.getInt(6), rs.getInt(7)));
            }
        }
    }

    @Override
    public void save(PlayerStats s) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO duel_stats (uuid, name, wins, losses, kills, deaths, streak, best_streak) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, "
                        + "wins = excluded.wins, losses = excluded.losses, kills = excluded.kills, "
                        + "deaths = excluded.deaths, streak = excluded.streak, best_streak = excluded.best_streak")) {
            ps.setString(1, s.uuid().toString());
            ps.setString(2, s.name());
            ps.setInt(3, s.wins());
            ps.setInt(4, s.losses());
            ps.setInt(5, s.kills());
            ps.setInt(6, s.deaths());
            ps.setInt(7, s.currentStreak());
            ps.setInt(8, s.bestStreak());
            ps.executeUpdate();
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {
            // shutting down
        }
    }
}
