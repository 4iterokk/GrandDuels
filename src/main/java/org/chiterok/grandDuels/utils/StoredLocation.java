package org.chiterok.grandDuels.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/**
 * Lossless, world-name based location. Resolving it to a {@link Location} can fail when the world is not (yet) loaded,
 * which must never destroy the persisted value.
 */
public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public static StoredLocation of(Location location) {
        World world = location.getWorld();
        if (world == null) throw new IllegalArgumentException("location has no world");
        return new StoredLocation(world.getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public static @Nullable StoredLocation read(@Nullable ConfigurationSection section) {
        if (section == null) return null;
        String world = section.getString("world");
        if (world == null || world.isBlank()) return null;
        return new StoredLocation(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

    public void write(ConfigurationSection section) {
        section.set("world", world);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", (double) yaw);
        section.set("pitch", (double) pitch);
    }

    public @Nullable Location toLocation() {
        World resolved = Bukkit.getWorld(world);
        return resolved == null ? null : new Location(resolved, x, y, z, yaw, pitch);
    }

    public String pretty() {
        return String.format(java.util.Locale.ROOT, "%s %.1f, %.1f, %.1f", world, x, y, z);
    }
}
