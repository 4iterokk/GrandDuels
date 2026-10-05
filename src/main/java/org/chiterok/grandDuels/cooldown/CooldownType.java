package org.chiterok.grandDuels.cooldown;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/** Items with a configurable per-duel cooldown. */
public enum CooldownType {
    GOLDEN_APPLE("golden-apple", Material.GOLDEN_APPLE),
    ENCHANTED_GOLDEN_APPLE("enchanted-golden-apple", Material.ENCHANTED_GOLDEN_APPLE),
    TRIDENT("trident", Material.TRIDENT),
    WIND_CHARGE("wind-charge", Material.WIND_CHARGE),
    ENDER_PEARL("ender-pearl", Material.ENDER_PEARL),
    CHORUS_FRUIT("chorus-fruit", Material.CHORUS_FRUIT);

    private final String configKey;
    private final Material material;

    CooldownType(String configKey, Material material) {
        this.configKey = configKey;
        this.material = material;
    }

    /** Key under {@code cooldowns.} in config.yml and messages.yml. */
    public String configKey() {
        return configKey;
    }

    public String messagePath() {
        return "cooldowns." + configKey;
    }

    public Material material() {
        return material;
    }

    public static @Nullable CooldownType forConsumable(Material material) {
        return switch (material) {
            case GOLDEN_APPLE -> GOLDEN_APPLE;
            case ENCHANTED_GOLDEN_APPLE -> ENCHANTED_GOLDEN_APPLE;
            case CHORUS_FRUIT -> CHORUS_FRUIT;
            default -> null;
        };
    }

    public static @Nullable CooldownType forProjectile(EntityType type) {
        return switch (type) {
            case ENDER_PEARL -> ENDER_PEARL;
            case TRIDENT -> TRIDENT;
            case WIND_CHARGE -> WIND_CHARGE;
            default -> null;
        };
    }
}
