package org.chiterok.grandDuels.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Blocking storage backend. Implementations are only called from the single storage thread. */
public interface StatsStorage extends AutoCloseable {

    void init() throws Exception;

    Optional<PlayerStats> load(UUID uuid) throws Exception;

    void save(PlayerStats stats) throws Exception;

    /** The {@code limit} players with the highest ELO (ties: more wins first, then name), best first. */
    List<PlayerStats> top(int limit) throws Exception;

    @Override
    void close();
}
