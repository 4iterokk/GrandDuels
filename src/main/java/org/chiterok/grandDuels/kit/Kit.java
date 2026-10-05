package org.chiterok.grandDuels.kit;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.chiterok.grandDuels.cooldown.CooldownType;
import org.chiterok.grandDuels.match.MatchSettings;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable kit definition. {@code components} lists the Paper data components that were customized in kits.yml
 * and is only used for GUI indicators.
 */
public record Kit(String id, String displayName, ItemStack icon, List<String> description,
                  Map<Integer, ItemStack> items,
                  @Nullable ItemStack helmet, @Nullable ItemStack chestplate,
                  @Nullable ItemStack leggings, @Nullable ItemStack boots, @Nullable ItemStack offhand,
                  List<PotionEffect> effects, Set<String> components) {

    /** Replaces the player's inventory, effects, health and food with this kit. */
    public void apply(Player player, MatchSettings settings) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            if (isAllowed(entry.getValue(), settings)) inventory.setItem(entry.getKey(), entry.getValue().clone());
        }
        if (helmet != null) inventory.setHelmet(helmet.clone());
        if (chestplate != null) inventory.setChestplate(chestplate.clone());
        if (leggings != null) inventory.setLeggings(leggings.clone());
        if (boots != null) inventory.setBoots(boots.clone());
        if (offhand != null && isAllowed(offhand, settings)) inventory.setItemInOffHand(offhand.clone());

        for (PotionEffect effect : effects) player.addPotionEffect(effect);

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(maxHealth == null ? 20.0 : maxHealth.getValue());
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
    }

    private static boolean isAllowed(ItemStack item, MatchSettings settings) {
        Material type = item.getType();
        for (CooldownType banned : settings.banned()) {
            if (banned.material() == type) return false;
        }
        return true;
    }

    /** Keys for {@code gui.tags.*} describing modern-combat items contained in the kit. */
    public List<String> featureKeys() {
        Set<String> keys = new LinkedHashSet<>();
        List<ItemStack> all = new ArrayList<>(items.values());
        for (ItemStack extra : new ItemStack[]{helmet, chestplate, leggings, boots, offhand}) {
            if (extra != null) all.add(extra);
        }
        for (ItemStack item : all) {
            Material type = item.getType();
            if (type == Material.MACE) keys.add("mace");
            else if (type == Material.WIND_CHARGE) keys.add("wind-charge");
            else if (type == Material.TRIDENT) keys.add("trident");
            else if (type == Material.WOLF_ARMOR) keys.add("wolf-armor");
            else if (type.name().endsWith("_SPEAR")) keys.add("spear");
        }
        return List.copyOf(keys);
    }
}
