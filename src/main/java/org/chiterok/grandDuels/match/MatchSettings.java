package org.chiterok.grandDuels.match;

/** Per-duel rule toggles chosen in the duel settings GUI. */
public record MatchSettings(boolean allowGapples, boolean customCooldowns) {

    public static MatchSettings defaults() {
        return new MatchSettings(true, true);
    }
}
