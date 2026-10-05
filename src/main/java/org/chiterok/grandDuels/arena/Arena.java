package org.chiterok.grandDuels.arena;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.chiterok.grandDuels.utils.StoredLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A duel arena: two spawn points (pos1/pos2) and a runtime state.
 * The arena boundary is the cuboid spanned by both points, expanded by the configured padding when a duel starts.
 */
public final class Arena {

    private final String name;
    private final ArenaRollback rollback = new ArenaRollback();
    private volatile StoredLocation pos1;
    private volatile StoredLocation pos2;
    private volatile ArenaState state = ArenaState.WAITING;
    private volatile BoundingBox activeBounds;

    public Arena(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public ArenaState state() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public ArenaRollback rollback() {
        return rollback;
    }

    public @Nullable StoredLocation pos1() {
        return pos1;
    }

    public @Nullable StoredLocation pos2() {
        return pos2;
    }

    public void setPos1(@Nullable StoredLocation pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(@Nullable StoredLocation pos2) {
        this.pos2 = pos2;
    }

    public @Nullable Location location1() {
        StoredLocation p = pos1;
        return p == null ? null : p.toLocation();
    }

    public @Nullable Location location2() {
        StoredLocation p = pos2;
        return p == null ? null : p.toLocation();
    }

    public @Nullable String worldName() {
        StoredLocation p = pos1;
        return p == null ? null : p.world();
    }

    /** Both points set, in the same world, and that world is loaded. */
    public boolean isComplete() {
        StoredLocation a = pos1;
        StoredLocation b = pos2;
        if (a == null || b == null || !a.world().equals(b.world())) return false;
        return a.toLocation() != null && b.toLocation() != null;
    }

    /** Computes the boundary for a starting duel. Requires {@link #isComplete()}. */
    public void activate(double padding) {
        Location a = location1();
        Location b = location2();
        if (a == null || b == null) throw new IllegalStateException("arena " + name + " is incomplete");
        this.activeBounds = BoundingBox.of(a, b).expand(padding);
    }

    public @Nullable BoundingBox activeBounds() {
        return activeBounds;
    }
}
