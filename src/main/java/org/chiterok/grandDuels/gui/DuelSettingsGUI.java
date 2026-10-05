package org.chiterok.grandDuels.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.config.Messages;
import org.chiterok.grandDuels.kit.Kit;
import org.chiterok.grandDuels.match.MatchSettings;
import org.chiterok.grandDuels.utils.ItemBuilder;

import java.util.Map;
import java.util.UUID;

/** Request screen: toggles sub-rules and sends the invitation. */
public final class DuelSettingsGUI extends GuiHolder {

    private static final int SLOT_GAPPLES = 11;
    private static final int SLOT_COOLDOWNS = 13;
    private static final int SLOT_SEND = 15;
    private static final int SLOT_BACK = 18;
    private static final int SLOT_CANCEL = 26;

    private final UUID targetId;
    private final String targetName;
    private final Kit kit;
    private final int returnPage;
    private boolean allowGapples = true;
    private boolean customCooldowns = true;

    public DuelSettingsGUI(GrandDuels plugin, Player viewer, Player target, Kit kit, int returnPage) {
        super(plugin, viewer);
        this.targetId = target.getUniqueId();
        this.targetName = target.getName();
        this.kit = kit;
        this.returnPage = returnPage;
    }

    @Override
    public void open() {
        Inventory inv = create(27, plugin.messages().get("gui.settings.title"));
        render(inv);
        viewer.openInventory(inv);
    }

    private void render(Inventory inv) {
        Messages m = plugin.messages();
        Map<String, String> ph = Messages.ph("target", targetName, "kit", kit.displayName());
        for (int slot = 0; slot < inv.getSize(); slot++) inv.setItem(slot, filler());

        inv.setItem(SLOT_GAPPLES, ItemBuilder.of(Material.GOLDEN_APPLE)
                .name(m.string(allowGapples ? "gui.settings.gapples-on" : "gui.settings.gapples-off"))
                .lore(m.stringList("gui.settings.gapples-lore"), ph).build());
        inv.setItem(SLOT_COOLDOWNS, ItemBuilder.of(Material.CLOCK)
                .name(m.string(customCooldowns ? "gui.settings.cooldowns-on" : "gui.settings.cooldowns-off"))
                .lore(m.stringList("gui.settings.cooldowns-lore"), ph).build());
        inv.setItem(SLOT_SEND, ItemBuilder.of(Material.EMERALD_BLOCK).name(m.string("gui.settings.send"))
                .lore(m.stringList("gui.settings.send-lore"), ph).build());
        inv.setItem(SLOT_BACK, ItemBuilder.of(Material.ARROW).name(m.string("gui.settings.back")).build());
        inv.setItem(SLOT_CANCEL, ItemBuilder.of(Material.BARRIER).name(m.string("gui.settings.cancel")).build());
    }

    @Override
    public void onClick(int slot, ClickType click) {
        switch (slot) {
            case SLOT_GAPPLES -> {
                allowGapples = !allowGapples;
                render(getInventory());
            }
            case SLOT_COOLDOWNS -> {
                customCooldowns = !customCooldowns;
                render(getInventory());
            }
            case SLOT_SEND -> {
                Player target = plugin.getServer().getPlayer(targetId);
                viewer.closeInventory();
                if (target == null) {
                    plugin.messages().send(viewer, "general.player-not-found", "player", targetName);
                    return;
                }
                plugin.duels().sendRequest(viewer, target, kit, new MatchSettings(allowGapples, customCooldowns));
            }
            case SLOT_BACK -> {
                Player target = plugin.getServer().getPlayer(targetId);
                if (target == null) {
                    viewer.closeInventory();
                    return;
                }
                new KitSelectorGUI(plugin, viewer, target, returnPage).open();
            }
            case SLOT_CANCEL -> viewer.closeInventory();
            default -> { /* filler */ }
        }
    }
}
