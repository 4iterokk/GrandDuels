package org.chiterok.grandDuels;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.chiterok.grandDuels.arena.ArenaManager;
import org.chiterok.grandDuels.command.admin.DuelsCommand;
import org.chiterok.grandDuels.command.player.DuelCommand;
import org.chiterok.grandDuels.config.ConfigManager;
import org.chiterok.grandDuels.config.Messages;
import org.chiterok.grandDuels.config.Settings;
import org.chiterok.grandDuels.cooldown.PvPCooldownManager;
import org.chiterok.grandDuels.data.SnapshotRepository;
import org.chiterok.grandDuels.data.StatsManager;
import org.chiterok.grandDuels.kit.KitManager;
import org.chiterok.grandDuels.match.DuelService;
import org.chiterok.grandDuels.match.MatchManager;
import org.chiterok.grandDuels.match.RequestManager;
import org.chiterok.grandDuels.runtime.ArenaProtectionListener;
import org.chiterok.grandDuels.runtime.CombatRestrictionListener;
import org.chiterok.grandDuels.runtime.GuiListener;
import org.chiterok.grandDuels.runtime.MatchListener;
import org.chiterok.grandDuels.runtime.PvPRulesListener;

/** Composition root: builds every service once and exposes them to the rest of the plugin. */
public final class GrandDuels extends JavaPlugin {

    private ConfigManager configs;
    private Messages messages;
    private ArenaManager arenas;
    private KitManager kits;
    private PvPCooldownManager cooldowns;
    private SnapshotRepository snapshots;
    private StatsManager stats;
    private RequestManager requests;
    private MatchManager matches;
    private DuelService duels;

    @Override
    public void onEnable() {
        this.configs = new ConfigManager(this);
        this.messages = new Messages(configs);
        this.arenas = new ArenaManager(this);
        this.kits = new KitManager(this);
        this.cooldowns = new PvPCooldownManager(this);
        this.snapshots = new SnapshotRepository(this);
        this.stats = new StatsManager(this);
        this.requests = new RequestManager(this);
        this.matches = new MatchManager(this);
        this.duels = new DuelService(this);

        arenas.load();
        kits.reload();
        stats.start();

        registerListeners(new GuiListener(), new PvPRulesListener(this), new CombatRestrictionListener(this),
                new ArenaProtectionListener(this), new MatchListener(this));
        registerCommand("duel", new DuelCommand(this));
        registerCommand("duels", new DuelsCommand(this));

        // /reload or late enable: players are already online
        for (Player online : Bukkit.getOnlinePlayers()) {
            stats.preload(online);
            if (matches.recover(online)) messages.send(online, "duels.recovered");
        }
        getLogger().info("GrandDuels enabled.");
    }

    @Override
    public void onDisable() {
        if (matches != null) matches.shutdown();
        if (requests != null) requests.clear();
        if (stats != null) stats.stop();
    }

    /** Reloads config.yml, messages.yml and kits.yml. Storage type changes need a restart. */
    public void reloadAll() {
        configs.reload();
        kits.reload();
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) getServer().getPluginManager().registerEvents(listener, this);
    }

    private void registerCommand(String name, org.bukkit.command.TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command '" + name + "' is missing from plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public ConfigManager configs() {
        return configs;
    }

    public Settings settings() {
        return configs.settings();
    }

    public Messages messages() {
        return messages;
    }

    public ArenaManager arenas() {
        return arenas;
    }

    public KitManager kits() {
        return kits;
    }

    public PvPCooldownManager cooldowns() {
        return cooldowns;
    }

    public SnapshotRepository snapshots() {
        return snapshots;
    }

    public StatsManager stats() {
        return stats;
    }

    public RequestManager requests() {
        return requests;
    }

    public MatchManager matches() {
        return matches;
    }

    public DuelService duels() {
        return duels;
    }
}
