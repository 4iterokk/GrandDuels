package org.chiterok.grandDuels.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Small fluent builder for GUI items. All text goes through {@link ColorUtil}. */
public final class ItemBuilder {

    private final ItemStack item;
    private Component name;
    private List<Component> lore;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public static ItemBuilder of(ItemStack base) {
        return new ItemBuilder(base.clone());
    }

    public ItemBuilder name(String text) {
        return name(text, Map.of());
    }

    public ItemBuilder name(String text, Map<String, String> placeholders) {
        this.name = ColorUtil.colorizeItem(text, placeholders);
        return this;
    }

    public ItemBuilder lore(List<String> lines, Map<String, String> placeholders) {
        this.lore = ColorUtil.colorizeItem(lines, placeholders);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemStack build() {
        item.editMeta(meta -> {
            if (name != null) meta.displayName(name);
            if (lore != null) meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
        });
        return item;
    }
}
