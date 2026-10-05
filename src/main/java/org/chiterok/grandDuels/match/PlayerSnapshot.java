package org.chiterok.grandDuels.match;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.chiterok.grandDuels.utils.StoredLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Complete pre-duel state of a player. Can be persisted for crash recovery. */
public final class PlayerSnapshot {

    private final ItemStack[] contents;
    private final double health;
    private final int food;
    private final float saturation;
    private final int level;
    private final float exp;
    private final List<PotionEffect> effects;
    private final GameMode gameMode;
    private final StoredLocation location;
    private final int fireTicks;
    private final boolean allowFlight;
    private final boolean flying;
    private final @Nullable Scoreboard scoreboard;

    private PlayerSnapshot(ItemStack[] contents, double health, int food, float saturation, int level, float exp,
                           List<PotionEffect> effects, GameMode gameMode, StoredLocation location, int fireTicks,
                           boolean allowFlight, boolean flying, @Nullable Scoreboard scoreboard) {
        this.contents = contents;
        this.health = health;
        this.food = food;
        this.saturation = saturation;
        this.level = level;
        this.exp = exp;
        this.effects = effects;
        this.gameMode = gameMode;
        this.location = location;
        this.fireTicks = fireTicks;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.scoreboard = scoreboard;
    }

    public static PlayerSnapshot capture(Player player) {
        ItemStack[] source = player.getInventory().getContents();
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
        return new PlayerSnapshot(copy, player.getHealth(), player.getFoodLevel(), player.getSaturation(),
                player.getLevel(), player.getExp(), new ArrayList<>(player.getActivePotionEffects()),
                player.getGameMode(), StoredLocation.of(player.getLocation()), player.getFireTicks(),
                player.getAllowFlight(), player.isFlying(), player.getScoreboard());
    }

    public @Nullable Location location() {
        return location.toLocation();
    }

    public StoredLocation storedLocation() {
        return location;
    }

    /**
     * Resets the player completely to the captured state. {@code destination} overrides the saved location.
     * <p>
     * {@code async=true} uses {@link Player#teleportAsync(Location)}, which loads the target chunks before moving the
     * player. A synchronous teleport right after a respawn or into unloaded chunks can leave the client stuck on
     * "Loading terrain...". Use {@code async=false} only while the player is leaving (quit) or the server stops.
     */
    public void restore(Player player, @Nullable Location destination, boolean async) {
        player.closeInventory();
        player.getInventory().clear();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) copy[i] = contents[i] == null ? null : contents[i].clone();
        player.getInventory().setContents(copy);

        for (PotionEffect active : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(active.getType());
        }
        for (PotionEffect effect : effects) player.addPotionEffect(effect);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? 20.0 : maxHealth.getValue();
        player.setHealth(Math.max(0.5, Math.min(health, max)));
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);
        player.setFireTicks(fireTicks);
        player.setFallDistance(0.0f);
        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);
        if (scoreboard != null) player.setScoreboard(scoreboard);

        Location target = destination != null ? destination : location();
        if (target == null) return;
        if (async) player.teleportAsync(target);
        else player.teleport(target);
    }

    // ---------------------------------------------------------------- persistence

    public void writeTo(YamlConfiguration yaml) {
        List<String> encoded = new ArrayList<>(contents.length);
        for (ItemStack item : contents) {
            encoded.add(item == null || item.getType().isAir() ? "" : Base64.getEncoder().encodeToString(item.serializeAsBytes()));
        }
        yaml.set("contents", encoded);
        yaml.set("health", health);
        yaml.set("food", food);
        yaml.set("saturation", (double) saturation);
        yaml.set("level", level);
        yaml.set("exp", (double) exp);
        yaml.set("effects", effects);
        yaml.set("gamemode", gameMode.name());
        location.write(yaml.createSection("location"));
        yaml.set("fire-ticks", fireTicks);
        yaml.set("allow-flight", allowFlight);
        yaml.set("flying", flying);
    }

    public static @Nullable PlayerSnapshot readFrom(YamlConfiguration yaml) {
        StoredLocation location = StoredLocation.read(yaml.getConfigurationSection("location"));
        if (location == null) return null;
        List<String> encoded = yaml.getStringList("contents");
        ItemStack[] contents = new ItemStack[encoded.size()];
        for (int i = 0; i < contents.length; i++) {
            String raw = encoded.get(i);
            contents[i] = raw.isEmpty() ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(raw));
        }
        List<PotionEffect> effects = new ArrayList<>();
        for (Object o : yaml.getList("effects", List.of())) {
            if (o instanceof PotionEffect effect) effects.add(effect);
        }
        GameMode mode;
        try {
            mode = GameMode.valueOf(yaml.getString("gamemode", "SURVIVAL"));
        } catch (IllegalArgumentException e) {
            mode = GameMode.SURVIVAL;
        }
        return new PlayerSnapshot(contents, yaml.getDouble("health", 20.0), yaml.getInt("food", 20),
                (float) yaml.getDouble("saturation", 5.0), yaml.getInt("level"), (float) yaml.getDouble("exp"),
                effects, mode, location, yaml.getInt("fire-ticks"), yaml.getBoolean("allow-flight"),
                yaml.getBoolean("flying"), null);
    }
}
