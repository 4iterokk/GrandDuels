package org.chiterok.grandDuels.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.config.Messages;
import org.chiterok.grandDuels.kit.Kit;
import org.chiterok.grandDuels.utils.ColorUtil;
import org.chiterok.grandDuels.utils.ItemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Paginated kit chooser (45 kits per page) shown before a duel request is sent. */
public final class KitSelectorGUI extends GuiHolder {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_CLOSE = 48;

    private final UUID targetId;
    private final String targetName;
    private final List<Kit> kits;
    private int page;

    public KitSelectorGUI(GrandDuels plugin, Player viewer, Player target, int page) {
        super(plugin, viewer);
        this.targetId = target.getUniqueId();
        this.targetName = target.getName();
        this.kits = new ArrayList<>(plugin.kits().all());
        this.page = Math.max(0, Math.min(page, pageCount() - 1));
    }

    private int pageCount() {
        return Math.max(1, (int) Math.ceil(kits.size() / (double) PER_PAGE));
    }

    @Override
    public void open() {
        Messages m = plugin.messages();
        Inventory inv = create(SIZE, m.get("gui.kit-selector.title"));

        if (kits.isEmpty()) {
            inv.setItem(22, ItemBuilder.of(Material.BARRIER).name(m.string("gui.kit-selector.empty")).build());
        }
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < kits.size(); i++) {
            inv.setItem(i, kitItem(kits.get(start + i)));
        }
        for (int slot = 45; slot < SIZE; slot++) inv.setItem(slot, filler());
        if (page > 0) inv.setItem(SLOT_PREV, ItemBuilder.of(Material.ARROW).name(m.string("gui.kit-selector.prev")).build());
        if (page < pageCount() - 1) {
            inv.setItem(SLOT_NEXT, ItemBuilder.of(Material.ARROW).name(m.string("gui.kit-selector.next")).build());
        }
        inv.setItem(SLOT_PAGE, ItemBuilder.of(Material.PAPER).name(m.string("gui.kit-selector.page"),
                Messages.ph("page", page + 1, "pages", pageCount())).build());
        inv.setItem(SLOT_CLOSE, ItemBuilder.of(Material.BARRIER).name(m.string("gui.kit-selector.close")).build());
        viewer.openInventory(inv);
    }

    private ItemStack kitItem(Kit kit) {
        Messages m = plugin.messages();
        List<String> lore = new ArrayList<>(kit.description());
        List<String> features = kit.featureKeys();
        if (!features.isEmpty() || !kit.components().isEmpty()) lore.add("");
        for (String key : features) lore.add(m.string("gui.tags." + key));
        if (!kit.components().isEmpty()) {
            lore.add(m.string("gui.tags.components").replace("{components}", String.join(", ", kit.components())));
        }
        lore.add("");
        lore.add(m.string("gui.kit-selector.hint"));

        ItemStack item = ItemBuilder.of(kit.icon()).amount(1).build();
        item.editMeta(meta -> {
            meta.displayName(ColorUtil.colorizeItem(kit.displayName()));
            meta.lore(ColorUtil.colorizeItem(lore, Map.of()));
        });
        return item;
    }

    @Override
    public void onClick(int slot, ClickType click) {
        if (slot >= 0 && slot < PER_PAGE) {
            int index = page * PER_PAGE + slot;
            if (index >= kits.size()) return;
            Player target = plugin.getServer().getPlayer(targetId);
            if (target == null) {
                plugin.messages().send(viewer, "general.player-not-found", "player", targetName);
                viewer.closeInventory();
                return;
            }
            new DuelSettingsGUI(plugin, viewer, target, kits.get(index), page).open();
        } else if (slot == SLOT_PREV && page > 0) {
            page--;
            open();
        } else if (slot == SLOT_NEXT && page < pageCount() - 1) {
            page++;
            open();
        } else if (slot == SLOT_CLOSE) {
            viewer.closeInventory();
        }
    }
}
