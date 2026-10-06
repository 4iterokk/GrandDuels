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
public record Settings(String language, StorageType storageType, DuelSettings duel, RuleSettings rules,
                       MatchmakingSettings matchmaking, PlayerKitSettings playerKits, ArenaModeSettings arenaMode) {

    /** Language used when {@code language} in config.yml is missing or malformed. */
    public static final String DEFAULT_LANGUAGE = "en";

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

    /**
     * @param enabled        false: {@code /duel kit create} copies the inventory instead of opening the editor
     * @param timeoutSeconds editing sessions are cancelled after this long (0 = never)
     * @param radius         editors cannot move farther than this from where they started (0 = unlimited)
     * @param maxItemBytes   an item whose serialized form is larger than this is rejected (0 = unlimited)
     */
    public record KitEditorSettings(boolean enabled, int timeoutSeconds, double radius, int maxItemBytes) {}

    public record PlayerKitSettings(int maxPerPlayer, Set<Material> bannedMaterials, KitEditorSettings editor) {}

    /**
     * @param enabled          false disables {@code /arena}
     * @param spawn            where players are teleported after choosing a kit (null until an admin sets it)
     * @param radius           players cannot move farther than this from the spawn (0 = unlimited)
     * @param combatTagSeconds {@code /arena leave} is refused this long after taking or dealing player damage
     */
    public record ArenaModeSettings(boolean enabled, @Nullable StoredLocation spawn, double radius,
                                    int combatTagSeconds) {}

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

        String language = c.getString("language", DEFAULT_LANGUAGE).trim().toLowerCase(Locale.ROOT);
        if (!language.matches("[a-z0-9_-]{2,16}")) language = DEFAULT_LANGUAGE;

        KitEditorSettings editor = new KitEditorSettings(
                c.getBoolean("player-kits.editor.enabled", true),
                Math.max(0, c.getInt("player-kits.editor.timeout-seconds", 600)),
                Math.max(0.0, c.getDouble("player-kits.editor.radius", 10.0)),
                Math.max(0, c.getInt("player-kits.editor.max-item-bytes", 4096)));
        ArenaModeSettings arenaMode = new ArenaModeSettings(
                c.getBoolean("arena-mode.enabled", true),
                StoredLocation.read(c.getConfigurationSection("arena-mode.spawn")),
                Math.max(0.0, c.getDouble("arena-mode.radius", 60.0)),
                Math.max(0, c.getInt("arena-mode.combat-tag-seconds", 10)));

        return new Settings(
                language,
                parseEnum(StorageType.class, c.getString("storage.type"), StorageType.YAML),
                duel, rules,
                new MatchmakingSettings(Math.max(1, c.getInt("matchmaking.k-factor", 32)),
                        Math.max(0, c.getInt("matchmaking.ranked.initial-elo-range", 100)),
                        Math.max(0.0, c.getDouble("matchmaking.ranked.elo-range-per-second", 5.0)),
                        Math.max(0, c.getInt("matchmaking.ranked.max-elo-range", 800))),
                new PlayerKitSettings(Math.max(0, c.getInt("player-kits.max-per-player", 5)), Set.copyOf(banned), editor),
                arenaMode);
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
