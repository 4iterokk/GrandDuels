package org.chiterok.grandDuels.arena;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.utils.StoredLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/** Registry and persistence (arenas.yml) of arenas. Mutations happen on the main thread. */
public final class ArenaManager {

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final GrandDuels plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();

    public ArenaManager(GrandDuels plugin) {
        this.plugin = plugin;
    }

    public static boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    public void load() {
        arenas.clear();
        ConfigurationSection root = plugin.configs().arenasFile().get().getConfigurationSection("arenas");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            Arena arena = new Arena(key.toLowerCase(Locale.ROOT));
            arena.setPos1(StoredLocation.read(section.getConfigurationSection("pos1")));
            arena.setPos2(StoredLocation.read(section.getConfigurationSection("pos2")));
            arenas.put(arena.name(), arena);
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
    }

    public void save() {
        YamlConfiguration yaml = plugin.configs().arenasFile().get();
        yaml.set("arenas", null);
        for (Arena arena : arenas.values()) {
            ConfigurationSection section = yaml.createSection("arenas." + arena.name());
            StoredLocation p1 = arena.pos1();
            StoredLocation p2 = arena.pos2();
            if (p1 != null) p1.write(section.createSection("pos1"));
            if (p2 != null) p2.write(section.createSection("pos2"));
        }
        plugin.configs().arenasFile().save();
    }

    public @Nullable Arena get(String name) {
        return arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Arena> all() {
        return List.copyOf(arenas.values());
    }

    public List<String> names() {
        return new ArrayList<>(arenas.keySet());
    }

    /** @return the new arena, or {@code null} if one with that name exists. */
    public @Nullable Arena create(String name) {
        Arena arena = new Arena(name.toLowerCase(Locale.ROOT));
        if (arenas.putIfAbsent(arena.name(), arena) != null) return null;
        save();
        return arena;
    }

    /** @return false if the arena does not exist or is in use. */
    public boolean delete(String name) {
        Arena arena = get(name);
        if (arena == null || arena.state() != ArenaState.WAITING) return false;
        arenas.remove(arena.name());
        save();
        return true;
    }

    /** Reserves a random free, complete arena (state STARTING) and computes its boundary. */
    public @Nullable Arena acquireFree(double boundaryPadding) {
        List<Arena> free = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.state() == ArenaState.WAITING && arena.isComplete()) free.add(arena);
        }
        if (free.isEmpty()) return null;
        Arena chosen = free.get(ThreadLocalRandom.current().nextInt(free.size()));
        chosen.activate(boundaryPadding);
        chosen.setState(ArenaState.STARTING);
        return chosen;
    }

    public boolean hasActive() {
        for (Arena arena : arenas.values()) {
            if (arena.state() != ArenaState.WAITING) return true;
        }
        return false;
    }

    /** Finds the in-use arena whose boundary contains the location. */
    public @Nullable Arena findActiveAt(Location location) {
        if (location.getWorld() == null) return null;
        String world = location.getWorld().getName();
        for (Arena arena : arenas.values()) {
            if (arena.state() == ArenaState.WAITING) continue;
            BoundingBox bounds = arena.activeBounds();
            if (bounds == null || !world.equals(arena.worldName())) continue;
            if (bounds.contains(location.getX(), location.getY(), location.getZ())) return arena;
        }
        return null;
    }
}
