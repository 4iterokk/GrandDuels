package org.chiterok.grandDuels.match;

import org.chiterok.grandDuels.cooldown.RuleDefaults;
import org.chiterok.grandDuels.cooldown.RuleType;

import java.util.Map;
import java.util.Set;

/**
 * Rules of one duel, resolved when the request is sent (from the sender's personal preferences and the server
 * defaults) so later config reloads cannot change a duel that is already agreed on.
 *
 * @param customCooldowns whether {@code cooldowns} are enforced (false = vanilla cooldowns only)
 * @param cooldowns       seconds per item type, 0 = none
 * @param banned          item types/abilities that cannot be used (items are also stripped from kits)
 */
public record MatchSettings(boolean customCooldowns, Map<RuleType, Double> cooldowns, Set<RuleType> banned) {

    public MatchSettings {
        cooldowns = Map.copyOf(cooldowns);
        banned = Set.copyOf(banned);
    }

    public static MatchSettings defaults(RuleDefaults defaults) {
        return new MatchSettings(true, defaults.cooldowns(), defaults.banned());
    }

    public boolean isBanned(RuleType type) {
        return banned.contains(type);
    }

    public double cooldownSeconds(RuleType type) {
        return cooldowns.getOrDefault(type, 0.0);
    }

    public MatchSettings withCustomCooldowns(boolean enabled) {
        return new MatchSettings(enabled, cooldowns, banned);
    }

    public MatchSettings withBanned(Set<RuleType> newBanned) {
        return new MatchSettings(customCooldowns, cooldowns, newBanned);
    }
}
