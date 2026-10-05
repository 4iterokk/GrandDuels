package org.chiterok.grandDuels.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.utils.ItemBuilder;
import net.kyori.adventure.text.Component;

/** Base class of all plugin GUIs. {@link org.chiterok.grandDuels.runtime.GuiListener} routes clicks here. */
public abstract class GuiHolder implements InventoryHolder {

    protected final GrandDuels plugin;
    protected final Player viewer;
    private Inventory inventory;

    protected GuiHolder(GrandDuels plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    protected final Inventory create(int size, Component title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        return inventory;
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    /** Builds the inventory contents and opens it for the viewer. */
    public abstract void open();

    /** @param slot raw slot inside the top inventory */
    public abstract void onClick(int slot, ClickType click);

    protected final ItemStack filler() {
        return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(plugin.messages().string("gui.filler")).build();
    }
}
