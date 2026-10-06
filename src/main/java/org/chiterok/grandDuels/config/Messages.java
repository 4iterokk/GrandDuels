package org.chiterok.grandDuels.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.chiterok.grandDuels.utils.ColorUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads messages.yml and renders it through {@link ColorUtil}. */
public final class Messages {

    private final ConfigManager configs;

    public Messages(ConfigManager configs) {
        this.configs = configs;
    }

    /** Builds a placeholder map from alternating key/value arguments. */
    @SuppressWarnings("unchecked")
    public static Map<String, String> ph(Object... keyValues) {
        // Tolerate a ready-made map passed as the only "varargs" element (a classic misuse of this API).
        if (keyValues.length == 1 && keyValues[0] instanceof Map<?, ?> ready) return (Map<String, String>) ready;
        if (keyValues.length % 2 != 0) throw new IllegalArgumentException("placeholders must be key/value pairs");
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), String.valueOf(keyValues[i + 1]));
        }
        return map;
    }

    public Component get(String path, Map<String, String> placeholders) {
        String raw = configs.messages().getString(path);
        return raw == null ? missing(path) : ColorUtil.colorize(raw, placeholders);
    }

    public Component get(String path) {
        return get(path, Map.of());
    }

    public Component prefixed(String path, Map<String, String> placeholders) {
        String raw = configs.messages().getString(path);
        if (raw == null) return missing(path);
        String prefix = configs.messages().getString("prefix");
        return ColorUtil.colorize((prefix == null ? "" : prefix) + raw, placeholders);
    }

    public List<Component> lines(String path, Map<String, String> placeholders) {
        List<Component> out = new ArrayList<>();
        for (String line : configs.messages().getStringList(path)) out.add(ColorUtil.colorize(line, placeholders));
        return out;
    }

    /** Raw (uncolored) configured string, for callers that feed it into {@link ColorUtil} themselves. */
    public String string(String path) {
        // getString(path) honors the jar defaults; getString(path, fallback) would skip them for keys missing on disk
        String value = configs.messages().getString(path);
        return value != null ? value : "[missing: " + path + "]";
    }

    public List<String> stringList(String path) {
        return configs.messages().getStringList(path);
    }

    public void send(CommandSender target, String path, Object... keyValues) {
        target.sendMessage(prefixed(path, ph(keyValues)));
    }

    public void send(CommandSender target, String path, Map<String, String> placeholders) {
        target.sendMessage(prefixed(path, placeholders));
    }

    public void actionBar(Player player, String path, Map<String, String> placeholders) {
        player.sendActionBar(get(path, placeholders));
    }

    public void sendPlain(CommandSender target, String path, Object... keyValues) {
        target.sendMessage(get(path, ph(keyValues)));
    }

    public void actionBar(Player player, String path, Object... keyValues) {
        player.sendActionBar(get(path, ph(keyValues)));
    }

    /** Shows {@code titles.<name>.title} / {@code titles.<name>.subtitle}. Durations are in ticks. */
    public void title(Player player, String name, Map<String, String> placeholders,
                      int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Component title = get("titles." + name + ".title", placeholders);
        Component subtitle = get("titles." + name + ".subtitle", placeholders);
        Title.Times times = Title.Times.times(ticks(fadeInTicks), ticks(stayTicks), ticks(fadeOutTicks));
        player.showTitle(Title.title(title, subtitle, times));
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(ticks * 50L);
    }

    private static Component missing(String path) {
        return Component.text("[missing message: " + path + "]", NamedTextColor.RED);
    }
}
