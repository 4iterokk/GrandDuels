package org.chiterok.grandDuels.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.jetbrains.annotations.Nullable;

/** Thin wrapper around a YAML file in the plugin data folder. */
public final class YamlFile {

    public enum Mode {
        /** Copied from the jar on first run; missing keys fall back to the jar defaults. */
        RESOURCE_WITH_DEFAULTS,
        /** Copied from the jar on first run only; the user owns the content afterwards. */
        RESOURCE_ONCE,
        /** Plugin-generated data file, created empty when missing. */
        DATA
    }

    private final JavaPlugin plugin;
    private final String name;
    private final Mode mode;
    private final File file;
    private volatile YamlConfiguration yaml;

    public YamlFile(JavaPlugin plugin, String name, Mode mode) {
        this.plugin = plugin;
        this.name = name;
        this.mode = mode;
        this.file = new File(plugin.getDataFolder(), name);
        ensureExists();
        reload();
    }

    private void ensureExists() {
        if (file.exists()) return;
        if (mode == Mode.DATA) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create " + parent);
            }
            try {
                if (!file.createNewFile()) plugin.getLogger().warning("Could not create " + file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create " + file, e);
            }
        } else {
            plugin.saveResource(name, false);
        }
    }

    public void reload() {
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        if (mode == Mode.RESOURCE_WITH_DEFAULTS) {
            YamlConfiguration defaults = readDefaults();
            if (defaults != null) {
                loaded.setDefaults(defaults);
                if (mergeMissing(loaded, defaults)) {
                    try {
                        loaded.save(file);
                        plugin.getLogger().info("Added missing default keys to " + name);
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.WARNING, "Could not update " + name, e);
                    }
                }
            }
        }
        this.yaml = loaded;
    }

    private @Nullable YamlConfiguration readDefaults() {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read default " + name, e);
            return null;
        }
    }

    /**
     * Copies every default key that is absent from the file on disk (new keys introduced by a plugin update), so
     * that admins can see and edit them. Existing values - including lists - are never overwritten.
     */
    private static boolean mergeMissing(YamlConfiguration loaded, YamlConfiguration defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) continue;
            if (!loaded.isSet(key)) {
                loaded.set(key, defaults.get(key));
                changed = true;
            }
        }
        return changed;
    }

    public YamlConfiguration get() {
        return yaml;
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file, e);
        }
    }
}
