package org.chiterok.grandDuels.cooldown;

import org.bukkit.entity.Player;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.config.Messages;
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
    private final Map<UUID, Map<CooldownType, Long>> expiries = new ConcurrentHashMap<>();

    public PvPCooldownManager(GrandDuels plugin) {
        this.plugin = plugin;
    }

    public long remainingMillis(Player player, CooldownType type) {
        Map<CooldownType, Long> perPlayer = expiries.get(player.getUniqueId());
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

    public boolean isOnCooldown(Player player, CooldownType type) {
        return remainingMillis(player, type) > 0L;
    }

    /** Starts a cooldown of {@code seconds} (the duel's rule for this item). 0 or less disables it. */
    public void apply(Player player, CooldownType type, double seconds) {
        if (seconds <= 0.0) return;
        long millis = (long) (seconds * 1000.0);
        expiries.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(CooldownType.class))
                .put(type, System.currentTimeMillis() + millis);
        player.setCooldown(type.material(), (int) Math.ceil(seconds * 20.0));
    }

    public void notifyBlocked(Player player, CooldownType type) {
        plugin.messages().actionBar(player, type.messagePath(),
                Messages.ph("time", TimeUtil.seconds(remainingMillis(player, type))));
    }

    /** Forgets all cooldowns of the player and clears the client-side item cooldowns. */
    public void clear(Player player) {
        expiries.remove(player.getUniqueId());
        if (!player.isOnline()) return;
        for (CooldownType type : CooldownType.values()) player.setCooldown(type.material(), 0);
    }
}
