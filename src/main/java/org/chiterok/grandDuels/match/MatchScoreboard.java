package org.chiterok.grandDuels.match;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.chiterok.grandDuels.config.Messages;
import org.chiterok.grandDuels.utils.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One scoreboard shared by both duelists: a sidebar built from messages.yml and a health objective below the
 * nameplates. It uses the plain Bukkit scoreboard API (teams as line carriers), so no packet library is required.
 * A shared board is required because the HEALTH criterion only updates scores on the board a player is viewing.
 */
public final class MatchScoreboard {

    private static final int MAX_LINES = 15;

    private final Scoreboard board;
    private final List<String> templates;
    private final List<Team> lineTeams = new ArrayList<>();

    public MatchScoreboard(Messages messages) {
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective sidebar = board.registerNewObjective("gd_sidebar", Criteria.DUMMY,
                ColorUtil.colorize(messages.string("scoreboard.title")));
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        sidebar.numberFormat(NumberFormat.blank());

        Objective health = board.registerNewObjective("gd_health", Criteria.HEALTH,
                Component.text("\u2764", NamedTextColor.RED));
        health.setDisplaySlot(DisplaySlot.BELOW_NAME);

        List<String> configured = messages.stringList("scoreboard.lines");
        this.templates = configured.size() > MAX_LINES ? configured.subList(0, MAX_LINES) : configured;
        for (int i = 0; i < templates.size(); i++) {
            String entry = "\u00a7" + Integer.toHexString(i) + "\u00a7r";
            Team team = board.registerNewTeam("gd_line_" + i);
            team.addEntry(entry);
            sidebar.getScore(entry).setScore(templates.size() - i);
            lineTeams.add(team);
        }
    }

    public Scoreboard board() {
        return board;
    }

    public void show(Player player) {
        player.setScoreboard(board);
    }

    public void update(Map<String, String> placeholders) {
        for (int i = 0; i < templates.size(); i++) {
            lineTeams.get(i).prefix(ColorUtil.colorize(templates.get(i), placeholders));
        }
    }
}
