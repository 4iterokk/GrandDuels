package org.chiterok.grandDuels.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.chiterok.grandDuels.GrandDuels;

/** Owns every YAML file of the plugin and the typed {@link Settings} derived from config.yml. */
public final class ConfigManager {

    private final YamlFile config;
    private final YamlFile messages;
    private final YamlFile kits;
    private final YamlFile arenas;
    private volatile Settings settings;

    public ConfigManager(GrandDuels plugin) {
        this.config = new YamlFile(plugin, "config.yml", YamlFile.Mode.RESOURCE_WITH_DEFAULTS);
        this.messages = new YamlFile(plugin, "messages.yml", YamlFile.Mode.RESOURCE_WITH_DEFAULTS);
        this.kits = new YamlFile(plugin, "kits.yml", YamlFile.Mode.RESOURCE_ONCE);
        this.arenas = new YamlFile(plugin, "arenas.yml", YamlFile.Mode.DATA);
        this.settings = Settings.from(config.get());
    }

    /** Reloads config, messages and kits files. Arenas are runtime state and are intentionally not reloaded. */
    public void reload() {
        config.reload();
        messages.reload();
        kits.reload();
        this.settings = Settings.from(config.get());
    }

    public Settings settings() {
        return settings;
    }

    public FileConfiguration config() {
        return config.get();
    }

    public FileConfiguration messages() {
        return messages.get();
    }

    public FileConfiguration kits() {
        return kits.get();
    }

    public YamlFile configFile() {
        return config;
    }

    public YamlFile kitsFile() {
        return kits;
    }

    public YamlFile arenasFile() {
        return arenas;
    }
}
