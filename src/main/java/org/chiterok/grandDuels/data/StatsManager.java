package org.chiterok.grandDuels.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.config.Settings;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/**
 * Asynchronous stats facade. All storage I/O runs on one dedicated thread, which serializes
 * read-modify-write cycles (no lost updates). The cache is read from the main thread and written only on that thread.
 */
public final class StatsManager {

    private final GrandDuels plugin;
    private final ConcurrentHashMap<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private StatsStorage storage;

    public StatsManager(GrandDuels plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Settings.StorageType type = plugin.settings().storageType();
        StatsStorage candidate = type == Settings.StorageType.SQLITE
                ? new SqliteStatsStorage(plugin.getDataFolder()) : new YamlStatsStorage(plugin.getDataFolder());
        try {
            candidate.init();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Stats storage " + type + " failed, falling back to YAML", e);
            candidate.close();
            candidate = new YamlStatsStorage(plugin.getDataFolder());
            try {
                candidate.init();
            } catch (Exception fatal) {
                plugin.getLogger().log(Level.SEVERE, "YAML stats storage failed too; stats are disabled", fatal);
                candidate = null;
            }
        }
        this.storage = candidate;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GrandDuels-Stats");
            t.setDaemon(true);
            return t;
        });
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (storage != null) storage.close();
    }

    /** Cached stats or an empty record (never null). Main thread. */
    public PlayerStats cached(UUID uuid, String name) {
        PlayerStats stats = cache.get(uuid);
        return stats != null ? stats : PlayerStats.empty(uuid, name);
    }

    public void preload(Player player) {
        update(player.getUniqueId(), player.getName(), s -> s);
    }

    /** Queued behind pending updates so a just-recorded result is not re-cached for an offline player. */
    public void evict(UUID uuid) {
        if (executor == null || executor.isShutdown()) {
            cache.remove(uuid);
            return;
        }
        executor.execute(() -> cache.remove(uuid));
    }

    public void recordResult(Player winner, Player loser) {
        update(winner.getUniqueId(), winner.getName(), PlayerStats::afterWin);
        update(loser.getUniqueId(), loser.getName(), PlayerStats::afterLoss);
    }

    /** Looks up stats of any player (online or not) without blocking the main thread. */
    public CompletableFuture<Optional<PlayerStats>> lookup(UUID uuid) {
        PlayerStats hit = cache.get(uuid);
        if (hit != null) return CompletableFuture.completedFuture(Optional.of(hit));
        if (executor == null || storage == null) return CompletableFuture.completedFuture(Optional.empty());
        return CompletableFuture.supplyAsync(() -> {
            try {
                return storage.load(uuid);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not load stats for " + uuid, e);
                return Optional.<PlayerStats>empty();
            }
        }, executor);
    }

    private void update(UUID uuid, String name, UnaryOperator<PlayerStats> change) {
        if (executor == null || storage == null || executor.isShutdown()) return;
        executor.execute(() -> {
            try {
                PlayerStats current = cache.get(uuid);
                if (current == null) {
                    current = storage.load(uuid).orElseGet(() -> PlayerStats.empty(uuid, name));
                }
                PlayerStats updated = change.apply(current.withName(name));
                storage.save(updated);
                cache.put(uuid, updated);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not update stats for " + uuid, e);
            }
        });
    }

    /** Resolves a name to a UUID using only locally cached data (no blocking Mojang lookup). */
    public static @Nullable UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        var cached = Bukkit.getOfflinePlayerIfCached(name);
        return cached == null ? null : cached.getUniqueId();
    }
}
