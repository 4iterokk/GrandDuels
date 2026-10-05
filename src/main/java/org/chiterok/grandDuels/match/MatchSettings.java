package org.chiterok.grandDuels.match;

import org.chiterok.grandDuels.config.Settings;
import org.chiterok.grandDuels.cooldown.CooldownType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Rules of one duel, resolved when the request is sent (from the sender's personal preferences and the server
 * defaults) so later config reloads cannot change a duel that is already agreed on.
 *
 * @param customCooldowns whether {@code cooldowns} are enforced (false = vanilla behaviour only)
 * @param cooldowns       seconds per item type, 0 = none
 * @param banned          item types that cannot be used (and are stripped from kits)
 */
public record MatchSettings(boolean customCooldowns, Map<CooldownType, Double> cooldowns, Set<CooldownType> banned) {

    public MatchSettings {
        cooldowns = Map.copyOf(cooldowns);
        banned = Set.copyOf(banned);
    }

    public static MatchSettings defaults(Settings server) {
        Map<CooldownType, Double> map = new EnumMap<>(CooldownType.class);
        for (CooldownType type : CooldownType.values()) map.put(type, server.cooldownSeconds(type));
        return new MatchSettings(true, map, Set.of());
    }

    public boolean isBanned(CooldownType type) {
        return banned.contains(type);
    }

    public double cooldownSeconds(CooldownType type) {
        return cooldowns.getOrDefault(type, 0.0);
    }

    public boolean allowGapples() {
        return !banned.contains(CooldownType.GOLDEN_APPLE) && !banned.contains(CooldownType.ENCHANTED_GOLDEN_APPLE);
    }

    public MatchSettings withGapples(boolean allowed) {
        Set<CooldownType> next = EnumSet.noneOf(CooldownType.class);
        next.addAll(banned);
        if (allowed) {
            next.remove(CooldownType.GOLDEN_APPLE);
            next.remove(CooldownType.ENCHANTED_GOLDEN_APPLE);
        } else {
            next.add(CooldownType.GOLDEN_APPLE);
            next.add(CooldownType.ENCHANTED_GOLDEN_APPLE);
        }
        return new MatchSettings(customCooldowns, cooldowns, next);
    }

    public MatchSettings withCustomCooldowns(boolean enabled) {
        return new MatchSettings(enabled, cooldowns, banned);
    }
}
