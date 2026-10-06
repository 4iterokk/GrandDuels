package org.chiterok.grandDuels.cooldown;

import org.bukkit.entity.Player;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.utils.TimeUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player item cooldowns during duels.
 * <p>
 * Every cooldown is mirrored to the client with {@link Player#setCooldown(org.bukkit.Material, int)} so that the item
 * is greyed out and the server refuses to start using it. The expiry map here is the authority for messages and checks.
 * All access happens on the main thread.
 */
public final class PvPCooldownManager {

    private final GrandDuels plugin;
    private final Map<UUID, Map<RuleType, Long>> expiries = new ConcurrentHashMap<>();

    public PvPCooldownManager(GrandDuels plugin) {
        this.plugin = plugin;
    }

    public long remainingMillis(Player player, RuleType type) {
        Map<RuleType, Long> perPlayer = expiries.get(player.getUniqueId());
        if (perPlayer == null) return 0L;
        Long expiry = perPlayer.get(type);
        if (expiry == null) return 0L;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            perPlayer.remove(type);
            return 0L;
        }
        return remaining;
    }

    public boolean isOnCooldown(Player player, RuleType type) {
        return remainingMillis(player, type) > 0L;
    }

    /** Starts a cooldown of {@code seconds} (the duel's rule for this item). 0 or less disables it. */
    public void apply(Player player, RuleType type, double seconds) {
        if (seconds <= 0.0 || !type.hasCooldown() || type.material() == null) return;
        long millis = (long) (seconds * 1000.0);
        expiries.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(RuleType.class))
                .put(type, System.currentTimeMillis() + millis);
        player.setCooldown(type.material(), (int) Math.ceil(seconds * 20.0));
    }

    public void notifyBlocked(Player player, RuleType type) {
        plugin.messages().actionBar(player, type.messagePath(),
                "time", TimeUtil.seconds(remainingMillis(player, type)));
    }

    /** Forgets all cooldowns of the player and clears the client-side item cooldowns. */
    public void clear(Player player) {
        expiries.remove(player.getUniqueId());
        if (!player.isOnline()) return;
        for (RuleType type : RuleType.values()) {
            if (type.hasCooldown() && type.material() != null) player.setCooldown(type.material(), 0);
        }
    }
}
