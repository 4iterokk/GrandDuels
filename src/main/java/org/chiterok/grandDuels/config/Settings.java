package org.chiterok.grandDuels.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.chiterok.grandDuels.utils.StoredLocation;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable, typed snapshot of config.yml. A new instance is built on every reload.
 * Item restrictions and cooldowns are NOT here: they live in menu/cooldowns.yml (see RuleDefaults).
 */
public record Settings(StorageType storageType, DuelSettings duel, RuleSettings rules,
                       MatchmakingSettings matchmaking, PlayerKitSettings playerKits) {

    public enum StorageType { YAML, SQLITE }

    public enum ReturnMode { PREVIOUS_LOCATION, LOBBY }

    public record DuelSettings(int requestExpireSeconds, boolean useKitSelector, String defaultKit,
                               int countdownSeconds, int celebrationSeconds, int maxDurationSeconds,
                               ReturnMode returnMode, @Nullable StoredLocation lobby, double boundaryPadding,
                               boolean buildingEnabled, int rollbackBatchSize, Set<String> commandWhitelist) {}

    public record WeaponRule(double maxDamage, double knockbackMultiplier) {}

    public record RuleSettings(boolean blockItemDrop, boolean blockItemPickup, WeaponRule mace, WeaponRule spear) {}

    /**
     * @param kFactor            ELO k-factor
     * @param initialEloRange    ranked: allowed ELO difference when a player joins the queue
     * @param eloRangePerSecond  ranked: how much the allowed difference grows per second of waiting
     * @param maxEloRange        ranked: upper limit of the allowed difference (0 = unlimited)
     */
    public record MatchmakingSettings(int kFactor, int initialEloRange, double eloRangePerSecond, int maxEloRange) {}

    public record PlayerKitSettings(int maxPerPlayer, Set<Material> bannedMaterials) {}

    public static Settings from(FileConfiguration c) {
        Set<String> whitelist = new LinkedHashSet<>();
        for (String entry : c.getStringList("duel.command-whitelist")) {
            String normalized = entry.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("/")) normalized = normalized.substring(1);
            if (!normalized.isEmpty()) whitelist.add(normalized);
        }

        DuelSettings duel = new DuelSettings(
                Math.max(5, c.getInt("duel.request-expire-seconds", 30)),
                c.getBoolean("duel.use-kit-selector", true),
                c.getString("duel.default-kit", "nodebuff").toLowerCase(Locale.ROOT),
                Math.max(0, c.getInt("duel.countdown-seconds", 5)),
                Math.max(0, c.getInt("duel.celebration-seconds", 5)),
                Math.max(0, c.getInt("duel.max-duration-seconds", 600)),
                parseEnum(ReturnMode.class, c.getString("duel.return-mode"), ReturnMode.PREVIOUS_LOCATION),
                StoredLocation.read(c.getConfigurationSection("lobby")),
                Math.max(0.0, c.getDouble("duel.boundary-padding", 40.0)),
                c.getBoolean("duel.building-enabled", false),
                Math.max(1, c.getInt("duel.rollback-batch-size", 500)),
                Set.copyOf(whitelist));

        RuleSettings rules = new RuleSettings(
                c.getBoolean("rules.block-item-drop", true),
                c.getBoolean("rules.block-item-pickup", true),
                weaponRule(c.getConfigurationSection("rules.mace")),
                weaponRule(c.getConfigurationSection("rules.spear")));

        Set<Material> banned = EnumSet.noneOf(Material.class);
        for (String name : c.getStringList("player-kits.banned-materials")) {
            Material material = Material.matchMaterial(name);
            if (material != null) banned.add(material);
        }

        return new Settings(
                parseEnum(StorageType.class, c.getString("storage.type"), StorageType.YAML),
                duel, rules,
                new MatchmakingSettings(Math.max(1, c.getInt("matchmaking.k-factor", 32)),
                        Math.max(0, c.getInt("matchmaking.ranked.initial-elo-range", 100)),
                        Math.max(0.0, c.getDouble("matchmaking.ranked.elo-range-per-second", 5.0)),
                        Math.max(0, c.getInt("matchmaking.ranked.max-elo-range", 800))),
                new PlayerKitSettings(Math.max(0, c.getInt("player-kits.max-per-player", 5)), Set.copyOf(banned)));
    }

    private static WeaponRule weaponRule(@Nullable ConfigurationSection s) {
        if (s == null) return new WeaponRule(0.0, 1.0);
        return new WeaponRule(Math.max(0.0, s.getDouble("max-damage", 0.0)),
                Math.max(0.0, s.getDouble("knockback-multiplier", 1.0)));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, @Nullable String raw, E fallback) {
        if (raw == null) return fallback;
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
