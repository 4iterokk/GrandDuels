package org.chiterok.grandDuels.match;

import org.bukkit.entity.Player;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.arena.Arena;
import org.chiterok.grandDuels.arena.ArenaState;
import org.chiterok.grandDuels.kit.Kit;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Registry of running duels and the entry point for starting them. Main thread only. */
public final class MatchManager {

    public enum StartResult { STARTED, PLAYER_BUSY, NO_ARENA, FAILED }

    private final GrandDuels plugin;
    private final Map<UUID, Match> byPlayer = new HashMap<>();

    public MatchManager(GrandDuels plugin) {
        this.plugin = plugin;
    }

    public @Nullable Match of(UUID playerId) {
        return byPlayer.get(playerId);
    }

    public @Nullable Match of(Player player) {
        return byPlayer.get(player.getUniqueId());
    }

    public boolean isInMatch(UUID playerId) {
        return byPlayer.containsKey(playerId);
    }

    public Collection<Match> all() {
        return new ArrayList<>(new java.util.LinkedHashSet<>(byPlayer.values()));
    }

    public StartResult start(Player first, Player second, Kit kit, MatchSettings settings) {
        if (isInMatch(first.getUniqueId()) || isInMatch(second.getUniqueId())) return StartResult.PLAYER_BUSY;

        Arena arena = plugin.arenas().acquireFree(plugin.settings().duel().boundaryPadding());
        if (arena == null) return StartResult.NO_ARENA;

        Match match = new Match(plugin, this, arena, kit, settings, first, second);
        byPlayer.put(first.getUniqueId(), match);
        byPlayer.put(second.getUniqueId(), match);
        plugin.requests().purge(first.getUniqueId());
        plugin.requests().purge(second.getUniqueId());

        boolean started;
        try {
            started = match.begin(first, second);
        } catch (RuntimeException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Duel start failed", e);
            started = false;
        }
        if (!started) {
            match.abort();
            byPlayer.remove(first.getUniqueId());
            byPlayer.remove(second.getUniqueId());
            arena.setState(ArenaState.WAITING);
            return StartResult.FAILED;
        }
        return StartResult.STARTED;
    }

    void unregister(Match match) {
        for (UUID id : match.participants()) byPlayer.remove(id, match);
    }

    /** Frees a single player (restored early, e.g. on quit) while the match object lives on. */
    void release(UUID playerId) {
        byPlayer.remove(playerId);
    }

    /** Restores a leftover snapshot (crash / interrupted duel) for a joining player. */
    public boolean recover(Player player) {
        UUID id = player.getUniqueId();
        if (isInMatch(id)) return false;
        PlayerSnapshot snapshot = plugin.snapshots().load(id);
        if (snapshot == null) return false;
        plugin.cooldowns().clear(player);
        snapshot.restore(player, snapshot.location());
        plugin.snapshots().delete(id);
        return true;
    }

    /** Synchronously ends every duel (plugin disable). */
    public void shutdown() {
        List<Match> matches = new ArrayList<>(all());
        for (Match match : matches) match.abort();
        byPlayer.clear();
    }
}
