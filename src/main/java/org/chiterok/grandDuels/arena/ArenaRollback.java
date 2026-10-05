package org.chiterok.grandDuels.arena;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records the original {@link BlockData} of every block changed during a duel and restores them afterwards.
 * Only the first (original) state of each position is kept. Main-thread only.
 */
public final class ArenaRollback {

    private record Key(UUID world, int x, int y, int z) {}

    private record Change(Key key, BlockData data) {}

    private final Map<Key, BlockData> originals = new LinkedHashMap<>();

    /** Call BEFORE the block changes. */
    public void record(Block block) {
        originals.putIfAbsent(new Key(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()),
                block.getBlockData());
    }

    /** Records a block through a state captured before the change (e.g. the replaced state of a placement). */
    public void record(BlockState state) {
        originals.putIfAbsent(new Key(state.getWorld().getUID(), state.getX(), state.getY(), state.getZ()),
                state.getBlockData());
    }

    public boolean isEmpty() {
        return originals.isEmpty();
    }

    /** Restores everything immediately. Used on shutdown, when no scheduler is available. */
    public void restoreAll() {
        for (Change change : drain()) apply(change);
    }

    /** Restores {@code batchSize} blocks per tick to avoid lag spikes, then runs {@code onDone}. */
    public void restoreBatched(Plugin plugin, int batchSize, Runnable onDone) {
        List<Change> pending = drain();
        if (pending.isEmpty()) {
            onDone.run();
            return;
        }
        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                int end = Math.min(index + batchSize, pending.size());
                for (; index < end; index++) apply(pending.get(index));
                if (index >= pending.size()) {
                    cancel();
                    onDone.run();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private List<Change> drain() {
        List<Change> changes = new ArrayList<>(originals.size());
        for (Map.Entry<Key, BlockData> entry : originals.entrySet()) {
            changes.add(new Change(entry.getKey(), entry.getValue()));
        }
        originals.clear();
        return changes;
    }

    private static void apply(Change change) {
        World world = Bukkit.getWorld(change.key().world());
        if (world == null) return;
        world.getBlockAt(change.key().x(), change.key().y(), change.key().z()).setBlockData(change.data(), false);
    }
}
