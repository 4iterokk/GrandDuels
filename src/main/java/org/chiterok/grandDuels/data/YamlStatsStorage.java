package org.chiterok.grandDuels.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public final class YamlStatsStorage implements StatsStorage {

    private final File file;
    private YamlConfiguration yaml;

    public YamlStatsStorage(File dataFolder) {
        this.file = new File(dataFolder, "stats.yml");
    }

    @Override
    public synchronized void init() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public synchronized Optional<PlayerStats> load(UUID uuid) {
        ConfigurationSection s = yaml.getConfigurationSection("players." + uuid);
        if (s == null) return Optional.empty();
        return Optional.of(new PlayerStats(uuid, s.getString("name", "unknown"), s.getInt("wins"), s.getInt("losses"),
                s.getInt("kills"), s.getInt("deaths"), s.getInt("streak"), s.getInt("best-streak"),
                s.getInt("elo", PlayerStats.DEFAULT_ELO)));
    }

    @Override
    public synchronized void save(PlayerStats stats) throws IOException {
        String base = "players." + stats.uuid();
        yaml.set(base + ".name", stats.name());
        yaml.set(base + ".wins", stats.wins());
        yaml.set(base + ".losses", stats.losses());
        yaml.set(base + ".kills", stats.kills());
        yaml.set(base + ".deaths", stats.deaths());
        yaml.set(base + ".streak", stats.currentStreak());
        yaml.set(base + ".best-streak", stats.bestStreak());
        yaml.set(base + ".elo", stats.elo());
        yaml.save(file);
    }

    @Override
    public void close() {
        // nothing to release; every save is flushed immediately
    }
}
