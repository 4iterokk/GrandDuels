package org.chiterok.grandDuels.data;

import java.util.Locale;
import java.util.UUID;

/** Immutable duel statistics, safe to hand between the main thread and the storage thread. */
public record PlayerStats(UUID uuid, String name, int wins, int losses, int kills, int deaths,
                          int currentStreak, int bestStreak) {

    public static PlayerStats empty(UUID uuid, String name) {
        return new PlayerStats(uuid, name, 0, 0, 0, 0, 0, 0);
    }

    public PlayerStats withName(String newName) {
        return new PlayerStats(uuid, newName, wins, losses, kills, deaths, currentStreak, bestStreak);
    }

    public PlayerStats afterWin() {
        int streak = currentStreak + 1;
        return new PlayerStats(uuid, name, wins + 1, losses, kills + 1, deaths, streak, Math.max(bestStreak, streak));
    }

    public PlayerStats afterLoss() {
        return new PlayerStats(uuid, name, wins, losses + 1, kills, deaths + 1, 0, bestStreak);
    }

    public double kdRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public String kdFormatted() {
        return String.format(Locale.ROOT, "%.2f", kdRatio());
    }
}
