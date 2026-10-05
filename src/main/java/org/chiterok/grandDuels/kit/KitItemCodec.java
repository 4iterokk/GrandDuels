package org.chiterok.grandDuels.kit;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.chiterok.grandDuels.utils.ColorUtil;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts items between kits.yml sections and {@link ItemStack}.
 * <ul>
 *   <li>{@code data}: lossless Base64 of {@link ItemStack#serializeAsBytes()} (keeps every data component).</li>
 *   <li>Structured form: material/name/lore/enchantments/potion/trim/attribute-modifiers via {@link ItemMeta},
 *       plus Paper {@code DataComponentTypes} through the {@code components} section.</li>
 * </ul>
 * All decode errors are reported as {@link IllegalArgumentException} with a human readable message.
 */
public final class KitItemCodec {

    private final JavaPlugin plugin;

    public KitItemCodec(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- decode

    public ItemStack decode(ConfigurationSection section, Set<String> usedComponents) {
        String data = section.getString("data");
        if (data != null && !data.isBlank()) {
            try {
                return ItemStack.deserializeBytes(Base64.getDecoder().decode(data.trim()));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("invalid serialized item data (" + e.getMessage() + ")", e);
            }
        }

        String materialName = section.getString("material");
        Material material = materialName == null ? null : Material.matchMaterial(materialName);
        if (material == null || material.isAir() || !material.isItem()) {
            throw new IllegalArgumentException("unknown or non-item material '" + materialName + "'");
        }

        ItemStack item = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        ConfigurationSection components = section.getConfigurationSection("components");
        if (components != null) applyComponents(item, components, usedComponents);
        item.editMeta(meta -> applyMeta(meta, section));
        return item;
    }

    private void applyComponents(ItemStack item, ConfigurationSection c, Set<String> used) {
        if (c.contains("max-stack-size")) {
            item.setData(DataComponentTypes.MAX_STACK_SIZE, Math.max(1, Math.min(99, c.getInt("max-stack-size"))));
            used.add("max_stack_size");
        }
        if (c.contains("max-damage")) {
            item.setData(DataComponentTypes.MAX_DAMAGE, Math.max(1, c.getInt("max-damage")));
            used.add("max_damage");
        }
        ConfigurationSection food = c.getConfigurationSection("food");
        if (food != null) {
            item.setData(DataComponentTypes.FOOD, FoodProperties.food()
                    .nutrition(Math.max(0, food.getInt("nutrition", 4)))
                    .saturation((float) food.getDouble("saturation", 2.4))
                    .canAlwaysEat(food.getBoolean("can-always-eat", false))
                    .build());
            used.add("food");
        }
        ConfigurationSection consumable = c.getConfigurationSection("consumable");
        if (consumable != null) {
            item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                    .consumeSeconds((float) Math.max(0.0, consumable.getDouble("consume-seconds", 1.6)))
                    .build());
            used.add("consumable");
        }
    }

    private void applyMeta(ItemMeta meta, ConfigurationSection s) {
        String name = s.getString("name");
        if (name != null) meta.displayName(ColorUtil.colorizeItem(name));

        List<String> lore = s.getStringList("lore");
        if (!lore.isEmpty()) meta.lore(ColorUtil.colorizeItem(lore));

        ConfigurationSection enchantments = s.getConfigurationSection("enchantments");
        if (enchantments != null) {
            for (String key : enchantments.getKeys(false)) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(ColorUtil.key(key)));
                if (enchantment == null) throw new IllegalArgumentException("unknown enchantment '" + key + "'");
                meta.addEnchant(enchantment, Math.max(1, enchantments.getInt(key)), true);
            }
        }

        if (s.getBoolean("unbreakable")) meta.setUnbreakable(true);
        if (meta instanceof Damageable damageable && s.contains("damage")) {
            damageable.setDamage(Math.max(0, s.getInt("damage")));
        }
        if (meta instanceof PotionMeta potionMeta) applyPotion(potionMeta, s.getConfigurationSection("potion"));
        if (meta instanceof ArmorMeta armorMeta) applyTrim(armorMeta, s.getConfigurationSection("trim"));
        applyAttributeModifiers(meta, s.getMapList("attribute-modifiers"));
    }

    private void applyPotion(PotionMeta meta, ConfigurationSection s) {
        if (s == null) return;
        String type = s.getString("type");
        if (type != null) {
            PotionType potionType = Registry.POTION.get(NamespacedKey.minecraft(ColorUtil.key(type)));
            if (potionType == null) throw new IllegalArgumentException("unknown potion type '" + type + "'");
            meta.setBasePotionType(potionType);
        }
        for (Map<?, ?> effect : s.getMapList("effects")) meta.addCustomEffect(parseEffect(effect), true);
    }

    private void applyTrim(ArmorMeta meta, ConfigurationSection s) {
        if (s == null) return;
        String materialKey = s.getString("material", "");
        String patternKey = s.getString("pattern", "");
        TrimMaterial material = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(ColorUtil.key(materialKey)));
        TrimPattern pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(ColorUtil.key(patternKey)));
        if (material == null) throw new IllegalArgumentException("unknown trim material '" + materialKey + "'");
        if (pattern == null) throw new IllegalArgumentException("unknown trim pattern '" + patternKey + "'");
        meta.setTrim(new ArmorTrim(material, pattern));
    }

    private void applyAttributeModifiers(ItemMeta meta, List<Map<?, ?>> entries) {
        int index = 0;
        for (Map<?, ?> entry : entries) {
            String attributeName = ColorUtil.key(String.valueOf(entry.get("attribute")))
                    .replace("generic.", "").replace("player.", "");
            Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attributeName));
            if (attribute == null) throw new IllegalArgumentException("unknown attribute '" + attributeName + "'");

            double amount = number(entry.get("amount"), 0.0);
            Object operationRaw = entry.get("operation");
            AttributeModifier.Operation operation;
            try {
                operation = operationRaw == null ? AttributeModifier.Operation.ADD_NUMBER
                        : AttributeModifier.Operation.valueOf(String.valueOf(operationRaw).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown attribute operation '" + operationRaw + "'", e);
            }

            Object slotRaw = entry.get("slot");
            EquipmentSlotGroup group = slotRaw == null ? EquipmentSlotGroup.ANY
                    : EquipmentSlotGroup.getByName(ColorUtil.key(String.valueOf(slotRaw)));
            if (group == null) throw new IllegalArgumentException("unknown equipment slot '" + slotRaw + "'");

            NamespacedKey key = new NamespacedKey(plugin, "kit_modifier_" + index++);
            meta.addAttributeModifier(attribute, new AttributeModifier(key, amount, operation, group));
        }
    }

    public PotionEffect parseEffect(Map<?, ?> map) {
        String typeName = String.valueOf(map.get("type"));
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(ColorUtil.key(typeName)));
        if (type == null) throw new IllegalArgumentException("unknown potion effect '" + typeName + "'");
        int duration = (int) number(map.get("duration"), 600);
        int amplifier = Math.max(0, (int) number(map.get("amplifier"), 0));
        boolean ambient = Boolean.parseBoolean(String.valueOf(map.get("ambient")));
        boolean particles = map.get("particles") == null || Boolean.parseBoolean(String.valueOf(map.get("particles")));
        boolean icon = map.get("icon") == null || Boolean.parseBoolean(String.valueOf(map.get("icon")));
        return new PotionEffect(type, duration == 0 ? 1 : duration, amplifier, ambient, particles, icon);
    }

    private static double number(Object raw, double fallback) {
        if (raw == null) return fallback;
        if (raw instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + raw + "' is not a number", e);
        }
    }

    // ---------------------------------------------------------------- encode

    /** Writes a lossless representation (material/amount for readability, {@code data} is authoritative). */
    public void encode(ConfigurationSection target, ItemStack item) {
        target.set("material", item.getType().name());
        target.set("amount", item.getAmount());
        target.set("data", Base64.getEncoder().encodeToString(item.serializeAsBytes()));
    }

    public Map<String, Object> encodeEffect(PotionEffect effect) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", effect.getType().getKey().getKey());
        map.put("duration", effect.isInfinite() ? -1 : effect.getDuration());
        map.put("amplifier", effect.getAmplifier());
        map.put("ambient", effect.isAmbient());
        map.put("particles", effect.hasParticles());
        map.put("icon", effect.hasIcon());
        return map;
    }
}
