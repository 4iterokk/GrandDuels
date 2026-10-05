package org.chiterok.grandDuels.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

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
            try (InputStream in = plugin.getResource(name)) {
                if (in != null) {
                    loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not read default " + name, e);
            }
        }
        this.yaml = loaded;
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
