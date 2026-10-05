package org.chiterok.grandDuels.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.config.Messages;
import org.chiterok.grandDuels.kit.Kit;
import org.chiterok.grandDuels.match.MatchSettings;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Request screen (menu/duel-settings.yml). The toggles start from the sender's personal preferences and only
 * affect this request.
 */
public final class DuelSettingsGUI extends GuiHolder {

    private final UUID targetId;
    private final String targetName;
    private final Kit kit;
    private final int returnPage;
    private MatchSettings settings;
    private Map<Integer, String> actions = Map.of();

    public DuelSettingsGUI(GrandDuels plugin, Player viewer, Player target, Kit kit, int returnPage) {
        super(plugin, viewer);
        this.targetId = target.getUniqueId();
        this.targetName = target.getName();
        this.kit = kit;
        this.returnPage = returnPage;
        this.settings = plugin.preferences().get(viewer.getUniqueId()).toSettings(plugin.settings());
    }

    @Override
    public void open() {
        MenuDefinition def = plugin.menus().get(MenuManager.DUEL_SETTINGS);
        create(def, placeholders());
        refresh(def);
        viewer.openInventory(getInventory());
    }

    private Map<String, String> placeholders() {
        return Messages.ph("target", targetName, "kit", kit.displayName());
    }

    private void refresh(MenuDefinition def) {
        Set<String> flags = new HashSet<>();
        if (settings.allowGapples()) flags.add("gapples");
        if (settings.customCooldowns()) flags.add("cooldowns");
        this.actions = render(def, flags, placeholders()).actions();
    }

    @Override
    public void onClick(int slot, ClickType click) {
        String action = actions.get(slot);
        if (action == null) return;
        MenuDefinition def = plugin.menus().get(MenuManager.DUEL_SETTINGS);
        switch (action.toUpperCase(Locale.ROOT)) {
            case "TOGGLE_GAPPLES" -> {
                settings = settings.withGapples(!settings.allowGapples());
                refresh(def);
            }
            case "TOGGLE_COOLDOWNS" -> {
                settings = settings.withCustomCooldowns(!settings.customCooldowns());
                refresh(def);
            }
            case "OPEN_COOLDOWNS" -> new CooldownMenuGUI(plugin, viewer, () -> {
                Player target = plugin.getServer().getPlayer(targetId);
                if (target != null) new DuelSettingsGUI(plugin, viewer, target, kit, returnPage).open();
            }).open();
            case "SEND" -> {
                Player target = plugin.getServer().getPlayer(targetId);
                viewer.closeInventory();
                if (target == null) {
                    plugin.messages().send(viewer, "general.player-not-found", "player", targetName);
                    return;
                }
                plugin.duels().sendRequest(viewer, target, kit, settings);
            }
            case "BACK" -> {
                Player target = plugin.getServer().getPlayer(targetId);
                if (target == null) {
                    viewer.closeInventory();
                    return;
                }
                new KitSelectorGUI(plugin, viewer, target, returnPage).open();
            }
            case "CLOSE" -> viewer.closeInventory();
            default -> { /* unknown action ids are ignored */ }
        }
    }
}
